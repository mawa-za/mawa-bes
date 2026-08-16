package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.MembershipPremiumPaymentCreateRequest;
import za.co.mawa.bes.dto.v2.ManualPremiumReceiptCaptureRequest;
import za.co.mawa.bes.dto.v2.PaymentBatchResponseDto;
import za.co.mawa.bes.dto.v2.PremiumPaymentDeletionRequest;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.PremiumPaymentDeletionStatusResponse;
import za.co.mawa.bes.dto.v2.PremiumPaymentTransferRequest;
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
    private final za.co.mawa.bes.repository.v2.CashupReceiptRepository cashupReceiptRepository;
    private final ManualPremiumReceiptRepository manualPremiumReceiptRepository;
    private final AttachmentRepository attachmentRepository;
    private final OnlineCashupService onlineCashupService;
    private final ApprovalService approvalService;
    private final za.co.mawa.bes.repository.v2.ApprovalRequestRepository approvalRequestRepository;
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
        validatePremiumPeriodSelection(request);

        PaymentBatchEntity batch = createBatch(request);

        List<String> preferredPeriods = PeriodUtil.isValidPeriod(request.getPeriodYYYYMM())
                ? List.of(request.getPeriodYYYYMM())
                : null;
        List<ReceiptResponseDto> receipts = allocateAmountToPremiums(
                batch, request.getMembershipId(), request.getAmountCents(), request.getCreatedBy(), preferredPeriods);
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
        applyAuthoritativeLegacyReceiptAmount(request);
        validateManual(request);
        ManualReceiptBookEntity receiptBook = manualReceiptBookService.requireActiveBookForReceipt(
                request.getManualReceiptNo());
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

        if (manualPremiumReceiptRepository.existsByReceiptBookNoAndManualReceiptNoAndVoidedAtIsNull(request.getReceiptBookNo().trim(), request.getManualReceiptNo().trim())) {
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

    @Transactional(readOnly = true)
    public void validateDeletionAllowed(String paymentBatchId) {
        PaymentBatchEntity batch = paymentBatchRepository.findById(paymentBatchId)
                .orElseThrow(() -> new IllegalArgumentException("Payment batch not found: " + paymentBatchId));
        if (batch.getSourceType() != ReceiptSourceType.MEMBERSHIP_PREMIUM) {
            throw new IllegalArgumentException("Only membership premium payments can be deleted");
        }
        if (batch.getStatus() != PaymentBatchStatus.POSTED) {
            throw new IllegalStateException("Only POSTED premium payments can be deleted");
        }
        List<ReceiptEntity> receipts = receiptRepository.findByPaymentBatchId(paymentBatchId);
        if (receipts.isEmpty()) {
            throw new IllegalStateException("No receipts were found for this premium payment");
        }
        if (allowPremiumPaymentDeletionWithoutCashupValidation()) {
            return;
        }
        boolean linkedToOpenCashup = false;
        for (ReceiptEntity receipt : receipts) {
            var links = new ArrayList<>(cashupReceiptRepository.findByReceiptId(receipt.getId()));
            manualPremiumReceiptRepository.findByPaymentBatchId(paymentBatchId).ifPresent(manualReceipt -> {
                for (var manualLink : cashupReceiptRepository.findByReceiptId(manualReceipt.getId())) {
                    if (links.stream().noneMatch(existing -> existing.getId().equals(manualLink.getId()))) {
                        links.add(manualLink);
                    }
                }
            });
            for (String physicalReceiptNo : List.of(
                    receipt.getManualReceiptNo() == null ? "" : receipt.getManualReceiptNo(),
                    receipt.getExternalReceiptNo() == null ? "" : receipt.getExternalReceiptNo())) {
                Long physicalNo = numericReceiptNo(physicalReceiptNo);
                if (physicalNo == null) continue;
                for (var physicalLink : cashupReceiptRepository.findByReceiptNo(physicalNo)) {
                    if (links.stream().noneMatch(existing -> existing.getId().equals(physicalLink.getId()))) {
                        links.add(physicalLink);
                    }
                }
            }
            Long numericReceiptNo = numericReceiptNo(receipt.getReceiptNo());
            if (numericReceiptNo != null) {
                for (var legacyLink : cashupReceiptRepository.findByReceiptNo(numericReceiptNo)) {
                    if (links.stream().noneMatch(existing -> existing.getId().equals(legacyLink.getId()))) {
                        links.add(legacyLink);
                    }
                }
            }
            if (links.isEmpty()) {
                throw new IllegalStateException("Receipt " + receipt.getReceiptNo() + " is not linked to an open cash-up");
            }
            for (var link : links) {
                if (link.getCashup() == null || !"OPEN".equalsIgnoreCase(link.getCashup().getStatus())) {
                    throw new IllegalStateException("Premium payments can only be deleted while the linked cash-up is OPEN");
                }
                linkedToOpenCashup = true;
            }
        }
        if (!linkedToOpenCashup) {
            throw new IllegalStateException("The premium payment is not linked to an open cash-up");
        }
    }

    @Transactional
    public ApprovalRequestResponse requestDeletion(String paymentBatchId, PremiumPaymentDeletionRequest request) {
        if (request == null || isBlank(request.getRequesterId())) {
            throw new IllegalArgumentException("requesterId is required");
        }
        if (isBlank(request.getReason())) {
            throw new IllegalArgumentException("A deletion reason is required");
        }

        var existingApproval = approvalRequestRepository.findByApprovalTypeAndReferenceId(
                ApprovalType.PREMIUM_PAYMENT_DELETION, paymentBatchId).orElse(null);
        if (existingApproval != null) {
            if (existingApproval.getStatus() == ApprovalStatus.PENDING
                    || existingApproval.getStatus() == ApprovalStatus.IN_PROGRESS) {
                return approvalService.getById(existingApproval.getId());
            }
            PaymentBatchEntity existingBatch = paymentBatchRepository.findById(paymentBatchId)
                    .orElseThrow(() -> new IllegalArgumentException("Payment batch not found: " + paymentBatchId));
            if (existingApproval.getStatus() == ApprovalStatus.APPROVED
                    && existingBatch.getStatus() == PaymentBatchStatus.REVERSED) {
                return approvalService.getById(existingApproval.getId());
            }
            throw new IllegalStateException(
                    "A premium payment deletion request already exists with status " + existingApproval.getStatus());
        }

        validateDeletionAllowed(paymentBatchId);
        PaymentBatchEntity batch = paymentBatchRepository.findById(paymentBatchId).orElseThrow();

        ApprovalSubmitRequest approval = new ApprovalSubmitRequest();
        approval.setApprovalType(ApprovalType.PREMIUM_PAYMENT_DELETION);
        approval.setReferenceId(batch.getId());
        approval.setReferenceNo(batch.getPaymentBatchNo());
        approval.setTitle("Delete premium payment " + batch.getPaymentBatchNo());
        approval.setDescription(request.getReason().trim());
        approval.setRequesterId(request.getRequesterId().trim());
        approval.setPayloadJson("{\"paymentBatchId\":\"" + batch.getId()
                + "\",\"membershipId\":\"" + batch.getMembershipId()
                + "\",\"amountCents\":" + batch.getTotalAmountCents() + "}");
        return approvalService.submitForApproval(approval);
    }

    @Transactional(readOnly = true)
    public PremiumPaymentDeletionStatusResponse deletionStatus(String paymentBatchId) {
        PaymentBatchEntity batch = paymentBatchRepository.findById(paymentBatchId)
                .orElseThrow(() -> new IllegalArgumentException("Payment batch not found: " + paymentBatchId));
        var approval = approvalRequestRepository.findByApprovalTypeAndReferenceId(
                ApprovalType.PREMIUM_PAYMENT_DELETION, paymentBatchId).orElse(null);
        return PremiumPaymentDeletionStatusResponse.builder()
                .paymentBatchId(batch.getId())
                .paymentBatchStatus(batch.getStatus())
                .approvalRequestId(approval == null ? null : approval.getId())
                .approvalStatus(approval == null ? null : approval.getStatus())
                .build();
    }

    @Transactional
    public PaymentBatchResponseDto transferManualPayment(
            String paymentBatchId,
            PremiumPaymentTransferRequest request
    ) {
        if (!allowPremiumPaymentTransfer()) {
            throw new IllegalStateException("Premium payment transfer is disabled in Premium Payment Settings");
        }
        if (request == null || isBlank(request.getTargetMembershipId())) {
            throw new IllegalArgumentException("targetMembershipId is required");
        }
        if (!PeriodUtil.isValidPeriod(request.getTargetPeriodYYYYMM())) {
            throw new IllegalArgumentException("targetPeriodYYYYMM is required and must use YYYYMM format");
        }
        if (isBlank(request.getRequestedBy())) {
            throw new IllegalArgumentException("requestedBy is required");
        }
        if (isBlank(request.getReason())) {
            throw new IllegalArgumentException("A transfer reason is required");
        }

        PaymentBatchEntity batch = paymentBatchRepository.findById(paymentBatchId)
                .orElseThrow(() -> new IllegalArgumentException("Payment batch not found: " + paymentBatchId));
        if (batch.getSourceType() != ReceiptSourceType.MEMBERSHIP_PREMIUM) {
            throw new IllegalArgumentException("Only membership premium payments can be transferred");
        }
        if (batch.getStatus() != PaymentBatchStatus.POSTED) {
            throw new IllegalStateException("Only POSTED premium payments can be transferred");
        }

        ManualPremiumReceiptEntity manualReceipt = manualPremiumReceiptRepository
                .findByPaymentBatchId(paymentBatchId)
                .orElse(null);
        List<ReceiptEntity> receipts = receiptRepository.findByPaymentBatchId(paymentBatchId);
        boolean migratedLegacyManualReceipt = isMigratedLegacyManualReceipt(batch, receipts);
        if (manualReceipt == null && !migratedLegacyManualReceipt) {
            throw new IllegalStateException(
                    "Only manually captured premium payments can be transferred");
        }

        String sourceMembershipId = batch.getMembershipId();
        String targetMembershipId = request.getTargetMembershipId().trim();
        if (isBlank(sourceMembershipId)) {
            throw new IllegalStateException("The payment does not have a source membership");
        }
        if (sourceMembershipId.equals(targetMembershipId)) {
            throw new IllegalArgumentException("Select a different target membership");
        }

        membershipService.getMembershipById(targetMembershipId)
                .orElseThrow(() -> new IllegalArgumentException("Target membership not found: " + targetMembershipId));

        if (receipts.isEmpty()) {
            throw new IllegalStateException("No receipts were found for this premium payment");
        }

        List<ReceiptAllocationEntity> allocations = new ArrayList<>();
        for (ReceiptEntity receipt : receipts) {
            if (receipt.getStatus() != ReceiptStatus.POSTED) {
                throw new IllegalStateException("All receipts in the payment batch must be POSTED before transfer");
            }
            List<ReceiptAllocationEntity> receiptAllocations = receiptAllocationRepository.findByReceiptId(receipt.getId())
                    .stream()
                    .filter(allocation -> allocation.getStatus() == ReceiptStatus.POSTED
                            && allocation.getAllocationType() == ReceiptAllocationType.MEMBERSHIP_PREMIUM)
                    .toList();
            if (receiptAllocations.isEmpty()) {
                throw new IllegalStateException("Receipt " + receipt.getReceiptNo() + " has no posted premium allocation");
            }
            allocations.addAll(receiptAllocations);
        }

        long allocationTotalCents = allocations.stream()
                .mapToLong(allocation -> allocation.getAmountCents() == null ? 0L : allocation.getAmountCents())
                .sum();
        if (allocationTotalCents <= 0L) {
            throw new IllegalStateException("The payment has no transferable premium amount");
        }
        if (batch.getTotalAmountCents() != null && allocationTotalCents != batch.getTotalAmountCents()) {
            throw new IllegalStateException("The payment allocation total does not match the payment batch total");
        }

        long targetMonthlyPremiumCents = determineMonthlyPremiumCents(targetMembershipId);
        MembershipPremiumEntity targetPremium = membershipPremiumService.findOrCreatePremium(
                targetMembershipId, request.getTargetPeriodYYYYMM(), targetMonthlyPremiumCents, request.getRequestedBy().trim());
        long targetBalanceCents = targetPremium.getBalanceCents() == null ? 0L : targetPremium.getBalanceCents();
        if (allocationTotalCents > targetBalanceCents) {
            throw new IllegalArgumentException(
                    "The target premium month does not have enough outstanding balance for this payment (R "
                            + String.format(java.util.Locale.ROOT, "%.2f", targetBalanceCents / 100.0) + ")");
        }

        String actor = request.getRequestedBy().trim();
        String reason = request.getReason().trim();
        String targetPeriod = request.getTargetPeriodYYYYMM();
        for (ReceiptAllocationEntity allocation : allocations) {
            if (!sourceMembershipId.equals(allocation.getMembershipId())) {
                throw new IllegalStateException("The payment contains an allocation for a different source membership");
            }
            MembershipPremiumEntity sourcePremium = membershipPremiumService.getById(allocation.getReferenceId());
            membershipPremiumService.reversePayment(sourcePremium, allocation.getAmountCents(), actor);
            targetPremium = membershipPremiumService.applyPayment(targetPremium, allocation.getAmountCents(), actor);

            allocation.setReferenceId(targetPremium.getId());
            allocation.setReferenceNo(targetMembershipId + "-" + targetPeriod);
            allocation.setPeriodYYYYMM(targetPeriod);
            allocation.setMembershipId(targetMembershipId);
            allocation.setUpdatedAt(LocalDateTime.now());
            allocation.setUpdatedBy(actor);
            receiptAllocationRepository.save(allocation);
        }

        String auditNote = "Premium payment transferred from membership " + sourceMembershipId
                + " to " + targetMembershipId + " for period " + targetPeriod + ". Reason: " + reason;
        for (ReceiptEntity receipt : receipts) {
            receipt.setMembershipId(targetMembershipId);
            receipt.setNotes(appendNote(receipt.getNotes(), auditNote));
            receipt.setUpdatedAt(LocalDateTime.now());
            receipt.setUpdatedBy(actor);
            receiptRepository.save(receipt);
        }

        batch.setMembershipId(targetMembershipId);
        batch.setNotes(appendNote(batch.getNotes(), auditNote));
        batch.setUpdatedAt(LocalDateTime.now());
        batch.setUpdatedBy(actor);
        paymentBatchRepository.save(batch);

        if (manualReceipt != null) {
            manualReceipt.setMembershipId(targetMembershipId);
            manualReceipt.setNotes(appendNote(manualReceipt.getNotes(), auditNote));
            manualPremiumReceiptRepository.save(manualReceipt);
        }

        membershipService.recalculatePaidUpToPeriod(sourceMembershipId);
        String targetPaidUpTo = membershipService.recalculatePaidUpToPeriod(targetMembershipId);

        List<ReceiptResponseDto> responseReceipts = receipts.stream()
                .map(receipt -> receiptService.getReceipt(receipt.getId()))
                .toList();
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
                .paidUpToPeriod(targetPaidUpTo)
                .receipts(responseReceipts)
                .build();
    }

    private boolean isMigratedLegacyManualReceipt(
            PaymentBatchEntity batch,
            List<ReceiptEntity> receipts
    ) {
        if (batch == null || isBlank(batch.getLegacyPremiumPaymentId()) || receipts == null || receipts.isEmpty()) {
            return false;
        }
        String legacyPaymentId = batch.getLegacyPremiumPaymentId().trim();
        return receipts.stream().anyMatch(receipt ->
                receipt != null
                        && !isBlank(receipt.getLegacyPremiumPaymentId())
                        && legacyPaymentId.equals(receipt.getLegacyPremiumPaymentId().trim())
                        && !isBlank(receipt.getExternalReceiptNo()));
    }

    @Transactional
    public void reverseApprovedPayment(String paymentBatchId, String actionBy, String reason) {
        validateDeletionAllowed(paymentBatchId);
        PaymentBatchEntity batch = paymentBatchRepository.findById(paymentBatchId)
                .orElseThrow(() -> new IllegalArgumentException("Payment batch not found: " + paymentBatchId));
        List<ReceiptEntity> receipts = receiptRepository.findByPaymentBatchId(paymentBatchId);
        for (ReceiptEntity receipt : receipts) {
            for (ReceiptAllocationEntity allocation : receiptAllocationRepository.findByReceiptId(receipt.getId())) {
                if (allocation.getStatus() != ReceiptStatus.POSTED
                        || allocation.getAllocationType() != ReceiptAllocationType.MEMBERSHIP_PREMIUM
                        || isBlank(allocation.getReferenceId())) {
                    continue;
                }
                MembershipPremiumEntity premium = membershipPremiumService.getById(allocation.getReferenceId());
                membershipPremiumService.reversePayment(premium, allocation.getAmountCents(), actionBy);
            }
        }

        String reversalReason = isBlank(reason) ? "Approved premium payment deletion" : reason.trim();
        for (ReceiptEntity receipt : receipts) {
            receiptService.reverseReceipt(receipt.getId(), reversalReason, actionBy);
        }
        ManualPremiumReceiptEntity manualReceipt = manualPremiumReceiptRepository
                .findByPaymentBatchId(paymentBatchId)
                .orElse(null);
        onlineCashupService.removeReceipts(
                receipts,
                manualReceipt == null ? List.of() : List.of(manualReceipt.getId()),
                actionBy,
                !allowPremiumPaymentDeletionWithoutCashupValidation());

        if (manualReceipt != null && manualReceipt.getVoidedAt() == null) {
            manualReceipt.setVoidedAt(LocalDateTime.now());
            manualReceipt.setVoidedBy(actionBy);
            manualReceipt.setVoidReason(isBlank(reason) ? "Approved premium payment deletion" : reason.trim());
            manualPremiumReceiptRepository.save(manualReceipt);
        }

        batch.setStatus(PaymentBatchStatus.REVERSED);
        batch.setNotes((isBlank(batch.getNotes()) ? "" : batch.getNotes() + "\n")
                + "Reversed after approved deletion: " + reversalReason);
        paymentBatchRepository.save(batch);
        if (!isBlank(batch.getMembershipId())) {
            membershipService.recalculatePaidUpToPeriod(batch.getMembershipId());
        }
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
        List<ReceiptResponseDto> responses = allocateAmountToPremiums(
                batch,
                request.getMembershipId(),
                request.getAmountCents(),
                request.getCreatedBy(),
                List.of(request.getPeriodYYYYMM()));
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


    private void applyAuthoritativeLegacyReceiptAmount(ManualPremiumReceiptCaptureRequest request) {
        if (request == null
                || isBlank(request.getMembershipId())
                || isBlank(request.getCaptureMode())
                || !"LEGACY_CATCH_UP".equalsIgnoreCase(request.getCaptureMode().trim())) {
            return;
        }

        long membershipPremiumCents = determineMonthlyPremiumCents(request.getMembershipId());
        if (membershipPremiumCents <= 0) {
            throw new IllegalStateException(
                    "The membership does not have a valid premium amount for legacy receipt capture");
        }

        // Legacy physical receipts represent the membership's monthly premium.
        // Do not trust a user-entered/client-supplied historical amount here: older
        // clients and previously editable screens could submit an incorrect value.
        request.setAmountCents(membershipPremiumCents);
    }


    private void validateManual(ManualPremiumReceiptCaptureRequest request) {
        if (request == null || isBlank(request.getMembershipId())) throw new IllegalArgumentException("membershipId is required");
        if (request.getAmountCents() == null || request.getAmountCents() <= 0) throw new IllegalArgumentException("amountCents must be greater than zero");
        if (isBlank(request.getPaymentMethod())) throw new IllegalArgumentException("paymentMethod is required");
        if (!PeriodUtil.isValidPeriod(request.getPeriodYYYYMM())) throw new IllegalArgumentException("periodYYYYMM is required and must use YYYYMM format");
        if (request.getOriginalReceiptDate() == null) throw new IllegalArgumentException("originalReceiptDate is required");
        if (request.getOriginalReceiptDate().isAfter(LocalDate.now())) throw new IllegalArgumentException("originalReceiptDate cannot be in the future");
        if (isBlank(request.getManualReceiptNo())) throw new IllegalArgumentException("manualReceiptNo is required");
        if (isBlank(request.getOriginalCollectorEmployeeId())) throw new IllegalArgumentException("originalCollectorEmployeeId is required");
        if (isBlank(request.getLocationAreaCode())) throw new IllegalArgumentException("locationAreaCode is required");
        if (isBlank(request.getCaptureMode()) || isBlank(request.getCreatedBy())) throw new IllegalArgumentException("captureMode and createdBy are required");
    }

    private Long numericReceiptNo(String receiptNo) {
        if (isBlank(receiptNo)) return null;
        String digits = receiptNo.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;
        try {
            return Long.valueOf(digits);
        } catch (NumberFormatException ignored) {
            return null;
        }
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

        List<String> requestedPeriods = preferredPeriods == null
                ? List.of()
                : preferredPeriods.stream()
                        .filter(PeriodUtil::isValidPeriod)
                        .distinct()
                        .toList();

        for (String period : requestedPeriods) {
            if (remaining <= 0) {
                break;
            }
            MembershipPremiumEntity premium = membershipPremiumService.findOrCreatePremium(
                    membershipId, period, monthlyPremiumCents, createdBy);
            long balance = premium.getBalanceCents() == null ? 0L : premium.getBalanceCents();
            if (balance <= 0) {
                throw new IllegalStateException("Selected payment period " + period + " is already fully paid");
            }
            long amountForPremium = Math.min(remaining, balance);
            ReceiptEntity receipt = createPremiumReceipt(batch, premium, amountForPremium, createdBy, null);
            MembershipPremiumEntity updatedPremium = membershipPremiumService.applyPayment(premium, amountForPremium, createdBy);
            ReceiptAllocationEntity allocation = receiptService.createAllocation(
                    receipt.getId(), ReceiptAllocationType.MEMBERSHIP_PREMIUM, updatedPremium.getId(),
                    updatedPremium.getMembershipId() + "-" + updatedPremium.getPeriodYYYYMM(),
                    updatedPremium.getPeriodYYYYMM(), updatedPremium.getMembershipId(), amountForPremium, createdBy);
            receiptResponses.add(receiptMapper.toDto(receipt, List.of(allocation)));
            remaining -= amountForPremium;
        }

        if (!requestedPeriods.isEmpty() && remaining > 0) {
            throw new IllegalStateException(
                    "The selected payment period outstanding balance is lower than the captured receipt amount");
        }

        for (MembershipPremiumEntity premium : unpaidPremiums) {
            if (requestedPeriods.contains(premium.getPeriodYYYYMM())) {
                continue;
            }
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

    private void validatePremiumPeriodSelection(MembershipPremiumPaymentCreateRequest request) {
        List<MembershipPremiumEntity> unpaidPremiums = membershipPremiumService.getUnpaidPremiums(request.getMembershipId());
        String selectedPeriod = request.getPeriodYYYYMM();

        if (unpaidPremiums.size() <= 1) {
            if (!isBlank(selectedPeriod) && !PeriodUtil.isValidPeriod(selectedPeriod)) {
                throw new IllegalArgumentException("periodYYYYMM must use YYYYMM format");
            }
            return;
        }

        if (!PeriodUtil.isValidPeriod(selectedPeriod)) {
            throw new IllegalArgumentException(
                    "Select the outstanding premium month to process because this membership has more than one outstanding premium");
        }

        MembershipPremiumEntity selectedPremium = unpaidPremiums.stream()
                .filter(premium -> selectedPeriod.equals(premium.getPeriodYYYYMM()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "The selected premium month is not one of the membership's outstanding premiums"));

        long balanceCents = selectedPremium.getBalanceCents() == null ? 0L : selectedPremium.getBalanceCents();
        if (request.getAmountCents() > balanceCents) {
            throw new IllegalArgumentException(
                    "The payment amount may not exceed the outstanding balance for the selected premium month (R "
                            + String.format(java.util.Locale.ROOT, "%.2f", balanceCents / 100.0) + ")");
        }
    }

    private boolean allowPremiumPaymentDeletionWithoutCashupValidation() {
        return settingEnabled("ALLOW_PREMIUM_PAYMENT_DELETE_WITHOUT_CASHUP_VALIDATION");
    }

    private boolean allowPremiumPaymentTransfer() {
        return settingEnabled("ALLOW_PREMIUM_PAYMENT_TRANSFER");
    }

    private boolean settingEnabled(String attribute) {
        String configured = settingService.getSetting(attribute, "MEMBERSHIP");
        if (configured == null) return false;
        String value = configured.trim();
        return "1".equals(value)
                || "true".equalsIgnoreCase(value)
                || "yes".equalsIgnoreCase(value)
                || "on".equalsIgnoreCase(value);
    }

    private String appendNote(String existing, String note) {
        return isBlank(existing) ? note : existing + "\n" + note;
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