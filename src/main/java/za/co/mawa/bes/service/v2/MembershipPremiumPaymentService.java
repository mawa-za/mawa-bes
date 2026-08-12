package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.MembershipPremiumPaymentCreateRequest;
import za.co.mawa.bes.dto.v2.ManualPremiumReceiptCaptureRequest;
import za.co.mawa.bes.dto.v2.PaymentBatchResponseDto;
import za.co.mawa.bes.dto.v2.ReceiptResponseDto;
import za.co.mawa.bes.dto.v2.ReceiptAllocationResponseDto;
import za.co.mawa.bes.entity.v2.MembershipPremiumEntity;
import za.co.mawa.bes.entity.v2.PaymentBatchEntity;
import za.co.mawa.bes.entity.v2.ReceiptAllocationEntity;
import za.co.mawa.bes.entity.v2.ReceiptEntity;
import za.co.mawa.bes.enums.*;
import za.co.mawa.bes.repository.v2.PaymentBatchRepository;
import za.co.mawa.bes.repository.v2.ReceiptAllocationRepository;
import za.co.mawa.bes.repository.v2.ReceiptRepository;
import za.co.mawa.bes.entity.v2.ManualReceiptCutoverConfigurationEntity;
import za.co.mawa.bes.entity.v2.ManualPremiumReceiptEntity;
import za.co.mawa.bes.entity.v2.ManualReceiptBookEntity;
import za.co.mawa.bes.repository.v2.ManualPremiumReceiptRepository;
import za.co.mawa.bes.repository.AttachmentRepository;
import za.co.mawa.bes.service.NotificationService;
import za.co.mawa.bes.service.SettingService;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipPremiumPaymentService {

    private final MembershipPremiumService membershipPremiumService;
    private final ReceiptService receiptService;
    private final ReceiptMapper receiptMapper;
    private final PaymentBatchRepository paymentBatchRepository;
    private final ReceiptAllocationRepository receiptAllocationRepository;
    private final ReceiptRepository receiptRepository;
    private final ManualPremiumReceiptRepository manualPremiumReceiptRepository;
    private final AttachmentRepository attachmentRepository;
    private final OnlineCashupService onlineCashupService;
    private final ManualReceiptCutoverConfigurationService cutoverConfigurationService;
    private final ManualReceiptBookService manualReceiptBookService;
    private final @Qualifier("MembershipServiceV2") MembershipService membershipService;
    private final SettingService settingService;
    @Autowired
    NumberAllocationService numberAllocationService;

    @Transactional
    public PaymentBatchResponseDto createPayment(MembershipPremiumPaymentCreateRequest request) {
        validate(request);
        validatePremiumPaymentLimit(request);

        PaymentBatchEntity batch = createBatch(request);

        List<ReceiptResponseDto> receipts = allocateAmountToPremiums(
                batch, request.getMembershipId(), request.getAmountCents(), request.getCreatedBy(), null);
        onlineCashupService.addReceipts(
                batch,
                receipts.stream().map(ReceiptResponseDto::getId).toList(),
                request.getCreatedBy(),
                request.getDeviceId());
        String paidUpTo = membershipService.recalculatePaidUpToPeriod(request.getMembershipId());

        return PaymentBatchResponseDto.builder()
                .id(batch.getId())
                .paymentBatchNo(batch.getPaymentBatchNo())
                .sourceType(batch.getSourceType())
                .membershipId(batch.getMembershipId())
                .paymentMethod(batch.getPaymentMethod())
                .totalAmountCents(batch.getTotalAmountCents())
                .paymentDate(batch.getPaymentDate())
                .status(batch.getStatus())
                .syncStatus(batch.getSyncStatus())
                .paidUpToPeriod(paidUpTo)
                .receipts(receipts)
                .build();
    }

    @Transactional
    public PaymentBatchResponseDto captureManualReceipt(ManualPremiumReceiptCaptureRequest request) {
        validateManual(request);
        ManualReceiptBookEntity receiptBook = manualReceiptBookService.requireActiveBookForReceipt(
                request.getReceiptBookNo(), request.getManualReceiptNo());
        ManualReceiptBookService.BookUsageReference bookUsage = manualReceiptBookService.validateBookUsage(
                receiptBook, request.getOriginalCollectorEmployeeId(), request.getLocationAreaCode());
        ManualReceiptBookService.EmployeeReference collector = bookUsage.employee();
        ManualReceiptBookService.AreaReference area = bookUsage.area();
        request.setReceiptBookNo(receiptBook.getReceiptBookNo());
        request.setOriginalCollectorEmployeeId(collector.id());
        request.setLocationAreaCode(area.code());
        ManualReceiptCutoverConfigurationEntity config = cutoverConfigurationService.getRequired();
        String mode = request.getCaptureMode().trim().toUpperCase();
        LocalDate today = LocalDate.now();

        if ("LEGACY_CATCH_UP".equals(mode)) {
            if (!Boolean.TRUE.equals(config.getLegacyCaptureEnabled())) {
                throw new IllegalStateException("Legacy receipt capture is disabled");
            }
            if (!request.getOriginalReceiptDate().isBefore(config.getMawaPayGoLiveDate())) {
                throw new IllegalArgumentException("Legacy catch-up receipts must be dated before the MAWAPay go-live date");
            }
            if (config.getLegacyCaptureCloseDate() != null && today.isAfter(config.getLegacyCaptureCloseDate())) {
                throw new IllegalStateException("The legacy receipt capture window is closed");
            }
        } else if ("MANUAL_EMERGENCY".equals(mode)) {
            if (request.getOriginalReceiptDate().isBefore(config.getMawaPayGoLiveDate())) {
                throw new IllegalArgumentException("Receipts before go-live must be captured as LEGACY_CATCH_UP");
            }
            if (Boolean.TRUE.equals(config.getEmergencyReceiptRequiresProof()) && isBlank(request.getProofAttachmentId())) {
                throw new IllegalArgumentException("proofAttachmentId is required for an emergency manual receipt");
            }
            if (!isBlank(request.getProofAttachmentId()) && !attachmentRepository.existsById(request.getProofAttachmentId())) {
                throw new IllegalArgumentException("The proof attachment does not exist");
            }
            if (isBlank(request.getLateCaptureReason())) {
                throw new IllegalArgumentException("lateCaptureReason is required for an emergency manual receipt");
            }
        } else {
            throw new IllegalArgumentException("captureMode must be LEGACY_CATCH_UP or MANUAL_EMERGENCY");
        }

        if (manualPremiumReceiptRepository.existsByReceiptBookNoAndManualReceiptNo(request.getReceiptBookNo().trim(), request.getManualReceiptNo().trim())) {
            throw new IllegalStateException("This receipt book and receipt number have already been captured");
        }

        MembershipPremiumPaymentCreateRequest payment = new MembershipPremiumPaymentCreateRequest();
        payment.setMembershipId(request.getMembershipId());
        payment.setAmountCents(request.getAmountCents());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaymentDate(request.getOriginalReceiptDate().atStartOfDay());
        payment.setLocation(area.code());
        payment.setEmployeeResponsible(collector.id());
        payment.setDeviceId("LEGACY_CATCH_UP".equals(mode) ? "ERP-LEGACY-IMPORT" : "ERP-MANUAL-EMERGENCY");
        payment.setTerminalId(null);
        payment.setCreatedBy(request.getCreatedBy());
        payment.setNotes(request.getNotes());

        validate(payment);
        PaymentBatchEntity batch = createBatch(payment);
        List<ReceiptResponseDto> receipts = allocateManualAmountToPremiums(batch, request, mode, collector, area);
        saveManualReceiptRegister(batch, request, mode, collector, area);
        String paidUpTo = membershipService.recalculatePaidUpToPeriod(request.getMembershipId());

        // Receipt-book captures are reconciled through the dedicated manual cashup flow.
        // They must not be added to the automatic ERP online cashup.
        return PaymentBatchResponseDto.builder()
                .id(batch.getId()).paymentBatchNo(batch.getPaymentBatchNo()).sourceType(batch.getSourceType())
                .membershipId(batch.getMembershipId()).paymentMethod(batch.getPaymentMethod())
                .totalAmountCents(batch.getTotalAmountCents()).paymentDate(batch.getPaymentDate())
                .status(batch.getStatus()).syncStatus(batch.getSyncStatus()).paidUpToPeriod(paidUpTo).receipts(receipts).build();
    }

    private PaymentBatchEntity createBatch(MembershipPremiumPaymentCreateRequest request) {
        PaymentBatchEntity batch = new PaymentBatchEntity();
        batch.setPaymentBatchNo(numberAllocationService.allocateNumber("PAYMENT_BATCH"));
        batch.setSourceType(ReceiptSourceType.MEMBERSHIP_PREMIUM); batch.setMembershipId(request.getMembershipId());
        batch.setPaymentMethod(request.getPaymentMethod()); batch.setTotalAmountCents(request.getAmountCents());
        batch.setPaymentDate(request.getPaymentDate() == null ? LocalDateTime.now() : request.getPaymentDate());
        batch.setLocation(request.getLocation()); batch.setEmployeeResponsible(request.getEmployeeResponsible());
        batch.setDeviceId(request.getDeviceId()); batch.setTerminalId(request.getTerminalId());
        batch.setStatus(PaymentBatchStatus.POSTED); batch.setSyncStatus(SyncStatus.SYNCED);
        batch.setNotes(request.getNotes()); batch.setCreatedBy(request.getCreatedBy()); batch.setCreatedAt(LocalDateTime.now());
        return paymentBatchRepository.save(batch);
    }

    private void saveManualReceiptRegister(
            PaymentBatchEntity batch,
            ManualPremiumReceiptCaptureRequest request,
            String mode,
            ManualReceiptBookService.EmployeeReference collector,
            ManualReceiptBookService.AreaReference area) {
        ManualPremiumReceiptEntity register = new ManualPremiumReceiptEntity();
        register.setPaymentBatchId(batch.getId()); register.setMembershipId(request.getMembershipId()); register.setCaptureMode(mode);
        register.setReceiptBookNo(request.getReceiptBookNo().trim()); register.setManualReceiptNo(request.getManualReceiptNo().trim());
        register.setOriginalReceiptDate(request.getOriginalReceiptDate()); register.setAmountCents(request.getAmountCents());
        register.setPaymentMethod(request.getPaymentMethod()); register.setOriginalCollector(collector.name());
        register.setOriginalCollectorEmployeeId(collector.id()); register.setLocation(area.code()); register.setLocationName(area.name());
        register.setWorkcentreId(null);
        register.setLateCaptureReason(request.getLateCaptureReason()); register.setProofAttachmentId(request.getProofAttachmentId());
        register.setCapturedAt(LocalDateTime.now()); register.setCapturedBy(request.getCreatedBy()); register.setNotes(request.getNotes());
        manualPremiumReceiptRepository.save(register);
    }

    private List<ReceiptResponseDto> allocateManualAmountToPremiums(
            PaymentBatchEntity batch,
            ManualPremiumReceiptCaptureRequest request,
            String mode,
            ManualReceiptBookService.EmployeeReference collector,
            ManualReceiptBookService.AreaReference area) {
        List<ReceiptResponseDto> responses = allocateAmountToPremiums(batch, request.getMembershipId(), request.getAmountCents(), request.getCreatedBy(), null);
        for (ReceiptResponseDto response : responses) {
            ReceiptEntity receipt = receiptRepository.findById(response.getId()).orElseThrow();
            receipt.setCaptureSource(mode); receipt.setManualReceiptBookNo(request.getReceiptBookNo().trim());
            receipt.setManualReceiptNo(request.getManualReceiptNo().trim()); receipt.setOriginalReceiptDate(request.getOriginalReceiptDate());
            receipt.setOriginalCollector(collector.name()); receipt.setOriginalCollectorEmployeeId(collector.id());
            receipt.setLocationName(area.name()); receipt.setWorkcentreId(null);
            receipt.setLateCaptureReason(request.getLateCaptureReason()); receipt.setProofAttachmentId(request.getProofAttachmentId());
            receipt.setCapturedBy(request.getCreatedBy()); receipt.setPrinted(false); receipt.setPrintCount(0);
            receiptRepository.save(receipt);
        }
        return responses.stream().map(r -> receiptService.getReceipt(r.getId())).toList();
    }


    private void validateManual(ManualPremiumReceiptCaptureRequest request) {
        if (request == null || isBlank(request.getMembershipId())) throw new IllegalArgumentException("membershipId is required");
        if (request.getAmountCents() == null || request.getAmountCents() <= 0) throw new IllegalArgumentException("amountCents must be greater than zero");
        if (isBlank(request.getPaymentMethod())) throw new IllegalArgumentException("paymentMethod is required");
        if (request.getOriginalReceiptDate() == null) throw new IllegalArgumentException("originalReceiptDate is required");
        if (request.getOriginalReceiptDate().isAfter(LocalDate.now())) throw new IllegalArgumentException("originalReceiptDate cannot be in the future");
        if (isBlank(request.getReceiptBookNo()) || isBlank(request.getManualReceiptNo())) throw new IllegalArgumentException("receiptBookNo and manualReceiptNo are required");
        if (isBlank(request.getOriginalCollectorEmployeeId())) throw new IllegalArgumentException("originalCollectorEmployeeId is required");
        if (isBlank(request.getLocationAreaCode())) throw new IllegalArgumentException("locationAreaCode is required");
        if (isBlank(request.getCaptureMode()) || isBlank(request.getCreatedBy())) throw new IllegalArgumentException("captureMode and createdBy are required");
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }

    public List<ReceiptResponseDto> allocateAmountToPremiums(
            PaymentBatchEntity batch,
            String membershipId,
            Long amountCents,
            String createdBy,
            List<String> preferredPeriods
    ) {
        List<ReceiptResponseDto> receiptResponses = new ArrayList<>();

        long remaining = amountCents;
        long monthlyPremiumCents = determineMonthlyPremiumCents(membershipId);

        List<MembershipPremiumEntity> unpaidPremiums = membershipPremiumService.getUnpaidPremiums(membershipId);

        for (MembershipPremiumEntity premium : unpaidPremiums) {
            if (remaining <= 0) {
                break;
            }

            long amountForPremium = Math.min(remaining, premium.getBalanceCents());

            ReceiptEntity receipt = createPremiumReceipt(batch, premium, amountForPremium, createdBy, null);
            MembershipPremiumEntity updatedPremium = membershipPremiumService.applyPayment(
                    premium,
                    amountForPremium,
                    createdBy
            );

            ReceiptAllocationEntity allocation = receiptService.createAllocation(
                    receipt.getId(),
                    ReceiptAllocationType.MEMBERSHIP_PREMIUM,
                    updatedPremium.getId(),
                    updatedPremium.getMembershipId() + "-" + updatedPremium.getPeriodYYYYMM(),
                    updatedPremium.getPeriodYYYYMM(),
                    updatedPremium.getMembershipId(),
                    amountForPremium,
                    createdBy
            );

            receiptResponses.add(receiptMapper.toDto(receipt, List.of(allocation)));

            remaining -= amountForPremium;
        }

        String nextPeriod = getNextFuturePeriod(membershipId, receiptResponses);

        while (remaining > 0) {
            long amountForPremium = Math.min(remaining, monthlyPremiumCents);

            MembershipPremiumEntity premium = membershipPremiumService.findOrCreatePremium(
                    membershipId,
                    nextPeriod,
                    monthlyPremiumCents,
                    createdBy
            );

            ReceiptEntity receipt = createPremiumReceipt(batch, premium, amountForPremium, createdBy, null);

            MembershipPremiumEntity updatedPremium = membershipPremiumService.applyPayment(
                    premium,
                    amountForPremium,
                    createdBy
            );

            ReceiptAllocationEntity allocation = receiptService.createAllocation(
                    receipt.getId(),
                    ReceiptAllocationType.MEMBERSHIP_PREMIUM,
                    updatedPremium.getId(),
                    updatedPremium.getMembershipId() + "-" + updatedPremium.getPeriodYYYYMM(),
                    updatedPremium.getPeriodYYYYMM(),
                    updatedPremium.getMembershipId(),
                    amountForPremium,
                    createdBy
            );

            receiptResponses.add(receiptMapper.toDto(receipt, List.of(allocation)));

            remaining -= amountForPremium;
            nextPeriod = PeriodUtil.nextPeriod(nextPeriod);
        }

        return receiptResponses;
    }

    private ReceiptEntity createPremiumReceipt(
            PaymentBatchEntity batch,
            MembershipPremiumEntity premium,
            Long amountCents,
            String createdBy,
            String receiptNo
    ) {
        ReceiptEntity receipt = new ReceiptEntity();
        receipt.setReceiptNo(receiptNo == null ? numberAllocationService.allocateNumber("RECEIPT") : receiptNo);
        receipt.setPaymentBatchId(batch.getId());
        receipt.setPaymentBatchNo(batch.getPaymentBatchNo());
        receipt.setSourceType(ReceiptSourceType.MEMBERSHIP_PREMIUM);
        receipt.setMembershipId(premium.getMembershipId());
        receipt.setReceiptDate(batch.getPaymentDate());
        receipt.setPaymentMethod(batch.getPaymentMethod());
        receipt.setTotalAmountCents(amountCents);
        receipt.setStatus(ReceiptStatus.POSTED);
        receipt.setSyncStatus(batch.getSyncStatus());
        receipt.setLocation(batch.getLocation());
        receipt.setEmployeeResponsible(batch.getEmployeeResponsible());
        receipt.setDeviceId(batch.getDeviceId());
        receipt.setTerminalId(batch.getTerminalId());
        // A receipt only becomes printed after the POS agent (or direct Bluetooth fallback)
        // confirms that the print was actually spooled successfully.
        receipt.setPrinted(false);
        receipt.setPrintCount(0);
        receipt.setCreatedAt(LocalDateTime.now());
        receipt.setCreatedBy(createdBy);

        return receiptService.saveReceipt(receipt);
    }

    private String getNextFuturePeriod(String membershipId, List<ReceiptResponseDto> receipts) {
        String latestPeriod = null;

        if (receipts != null && !receipts.isEmpty()) {
            ReceiptResponseDto lastReceipt = receipts.get(receipts.size() - 1);
            java.util.List<ReceiptAllocationResponseDto> allocations = lastReceipt.getAllocations();
            if (allocations != null && !allocations.isEmpty()) {
                latestPeriod = allocations.get(0).getPeriodYYYYMM();
            }
        }

        for (MembershipPremiumEntity premium : membershipPremiumService.getPremiumsForMembership(membershipId)) {
            String period = premium.getPeriodYYYYMM();
            if (PeriodUtil.isValidPeriod(period) && (latestPeriod == null || period.compareTo(latestPeriod) > 0)) {
                latestPeriod = period;
            }
        }

        String paidUpToPeriod = membershipService.resolveMembership(membershipId).getPaidUpToPeriod();
        if (PeriodUtil.isValidPeriod(paidUpToPeriod)
                && (latestPeriod == null || paidUpToPeriod.compareTo(latestPeriod) > 0)) {
            latestPeriod = paidUpToPeriod;
        }

        if (latestPeriod == null) {
            return PeriodUtil.currentPeriod();
        }

        String nextExistingPeriod = PeriodUtil.nextPeriod(latestPeriod);
        return nextExistingPeriod.compareTo(PeriodUtil.currentPeriod()) < 0
                ? PeriodUtil.currentPeriod()
                : nextExistingPeriod;
    }

    private String getLastPeriod(List<ReceiptResponseDto> receipts) {
        if (receipts == null || receipts.isEmpty()) {
            return null;
        }

        ReceiptResponseDto lastReceipt = receipts.get(receipts.size() - 1);

        java.util.List<ReceiptAllocationResponseDto> allocations = lastReceipt.getAllocations();
        if (allocations == null || allocations.isEmpty()) {
            return null;
        }

        return allocations.get(0).getPeriodYYYYMM();
    }

    private long determineMonthlyPremiumCents(String membershipId) {
        Long premiumCents = membershipService.getMembershipById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + membershipId))
                .getPremiumCents();
        return premiumCents == null ? 0L : premiumCents;
    }

    private void validatePremiumPaymentLimit(MembershipPremiumPaymentCreateRequest request) {
        long monthlyPremiumCents = determineMonthlyPremiumCents(request.getMembershipId());
        if (monthlyPremiumCents <= 0) {
            return;
        }
        int maxMonths = resolveMaxPremiumPaymentMonths();
        long maximumCents = monthlyPremiumCents * (long) maxMonths;
        if (request.getAmountCents() > maximumCents) {
            throw new IllegalArgumentException(
                    "Premium payment may not exceed " + maxMonths + " months (R "
                            + String.format(java.util.Locale.ROOT, "%.2f", maximumCents / 100.0) + ").");
        }
    }

    private int resolveMaxPremiumPaymentMonths() {
        String configured = settingService.getSetting("MAX_PREMIUM_PAYMENT_MONTHS", "MEMBERSHIP");
        if (configured == null || configured.isBlank()) {
            return 3;
        }
        try {
            int value = Integer.parseInt(configured.trim());
            return value >= 1 && value <= 24 ? value : 3;
        } catch (NumberFormatException ignored) {
            return 3;
        }
    }

    private void validate(MembershipPremiumPaymentCreateRequest request) {
        if (request.getMembershipId() == null || request.getMembershipId().isBlank()) {
            throw new RuntimeException("membershipId is required");
        }

        if (request.getAmountCents() == null || request.getAmountCents() <= 0) {
            throw new RuntimeException("amountCents must be greater than zero");
        }

        if (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()) {
            throw new RuntimeException("paymentMethod is required");
        }
    }
}