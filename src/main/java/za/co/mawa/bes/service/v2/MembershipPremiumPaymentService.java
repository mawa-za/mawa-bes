package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.MembershipPremiumPaymentCreateRequest;
import za.co.mawa.bes.dto.v2.PaymentBatchResponseDto;
import za.co.mawa.bes.dto.v2.ReceiptResponseDto;
import za.co.mawa.bes.entity.v2.MembershipPremiumEntity;
import za.co.mawa.bes.entity.v2.PaymentBatchEntity;
import za.co.mawa.bes.entity.v2.ReceiptAllocationEntity;
import za.co.mawa.bes.entity.v2.ReceiptEntity;
import za.co.mawa.bes.enums.*;
import za.co.mawa.bes.repository.v2.PaymentBatchRepository;
import za.co.mawa.bes.repository.v2.ReceiptAllocationRepository;
import za.co.mawa.bes.service.NotificationService;

import java.time.LocalDateTime;
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
    private final @Qualifier("MembershipServiceV2") MembershipService membershipService;
    @Autowired
    NumberAllocationService numberAllocationService;

    @Transactional
    public PaymentBatchResponseDto createPayment(MembershipPremiumPaymentCreateRequest request) {
        validate(request);

        PaymentBatchEntity batch = new PaymentBatchEntity();
        batch.setPaymentBatchNo(numberAllocationService.allocateNumber("PAYMENT_BATCH"));
        batch.setSourceType(ReceiptSourceType.MEMBERSHIP_PREMIUM);
        batch.setMembershipId(request.getMembershipId());
        batch.setPaymentMethod(request.getPaymentMethod());
        batch.setTotalAmountCents(request.getAmountCents());
        batch.setPaymentDate(request.getPaymentDate() == null ? LocalDateTime.now() : request.getPaymentDate());
        batch.setLocation(request.getLocation());
        batch.setEmployeeResponsible(request.getEmployeeResponsible());
        batch.setDeviceId(request.getDeviceId());
        batch.setTerminalId(request.getTerminalId());
        batch.setStatus(PaymentBatchStatus.POSTED);
        batch.setSyncStatus(SyncStatus.SYNCED);
        batch.setNotes(request.getNotes());
        batch.setCreatedBy(request.getCreatedBy());
        batch.setCreatedAt(LocalDateTime.now());

        batch = paymentBatchRepository.save(batch);

        List<ReceiptResponseDto> receipts = allocateAmountToPremiums(
                batch,
                request.getMembershipId(),
                request.getAmountCents(),
                request.getCreatedBy(),
                null
        );

        String paidUpTo = membershipService.recalculatePaidUpToPeriod(request.getMembershipId());

        return PaymentBatchResponseDto.builder()
                .id(batch.getId())
                .paymentBatchNo(batch.getPaymentBatchNo())
                .sourceType(batch.getSourceType().name())
                .membershipId(batch.getMembershipId())
                .paymentMethod(batch.getPaymentMethod())
                .totalAmountCents(batch.getTotalAmountCents())
                .paymentDate(batch.getPaymentDate())
                .status(batch.getStatus().name())
                .syncStatus(batch.getSyncStatus().name())
                .paidUpToPeriod(paidUpTo)
                .receipts(receipts)
                .build();
    }

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

            ReceiptEntity receipt = createPremiumReceipt(batch, premium, amountForPremium, createdBy, null, true);
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

        String nextPeriod = getNextFuturePeriod(receiptResponses);

        while (remaining > 0) {
            long amountForPremium = Math.min(remaining, monthlyPremiumCents);

            MembershipPremiumEntity premium = membershipPremiumService.findOrCreatePremium(
                    membershipId,
                    nextPeriod,
                    monthlyPremiumCents,
                    createdBy
            );

            ReceiptEntity receipt = createPremiumReceipt(batch, premium, amountForPremium, createdBy, null, true);

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
            String receiptNo,
            Boolean printed
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
        receipt.setPrinted(Boolean.TRUE.equals(printed));
        receipt.setPrintCount(Boolean.TRUE.equals(printed) ? 1 : 0);
        receipt.setCreatedAt(LocalDateTime.now());
        receipt.setCreatedBy(createdBy);

        return receiptService.saveReceipt(receipt);
    }

    private String getNextFuturePeriod(List<ReceiptResponseDto> receipts) {
        if (receipts.isEmpty()) {
            return PeriodUtil.currentPeriod();
        }

        ReceiptResponseDto lastReceipt = receipts.get(receipts.size() - 1);

        if (lastReceipt.getAllocations() == null || lastReceipt.getAllocations().isEmpty()) {
            return PeriodUtil.currentPeriod();
        }

        String lastPeriod = lastReceipt.getAllocations().get(0).getPeriodYYYYMM();
        return PeriodUtil.nextPeriod(lastPeriod);
    }

    private String getLastPeriod(List<ReceiptResponseDto> receipts) {
        if (receipts == null || receipts.isEmpty()) {
            return null;
        }

        ReceiptResponseDto lastReceipt = receipts.get(receipts.size() - 1);

        if (lastReceipt.getAllocations() == null || lastReceipt.getAllocations().isEmpty()) {
            return null;
        }

        return lastReceipt.getAllocations().get(0).getPeriodYYYYMM();
    }

    private Long determineMonthlyPremiumCents(String membershipId) {
        /*
         * TODO:
         * Replace this with your actual premium calculation:
         * - membership plan base premium
         * - dependent premium additions
         * - age-based premium rules
         */
        return 25000L;
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