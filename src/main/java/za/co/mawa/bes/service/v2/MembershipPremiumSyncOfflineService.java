package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.*;
        import za.co.mawa.bes.entity.v2.MembershipPremiumEntity;
import za.co.mawa.bes.entity.v2.PaymentBatchEntity;
import za.co.mawa.bes.entity.v2.ReceiptAllocationEntity;
import za.co.mawa.bes.entity.v2.ReceiptEntity;
import za.co.mawa.bes.enums.*;
        import za.co.mawa.bes.repository.v2.PaymentBatchRepository;
import za.co.mawa.bes.repository.v2.ReceiptRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipPremiumSyncOfflineService {

    private final PaymentBatchRepository paymentBatchRepository;
    private final ReceiptRepository receiptRepository;
    private final MembershipPremiumService membershipPremiumService;
    private final ReceiptService receiptService;
    private final ReceiptMapper receiptMapper;
    private final @Qualifier("MembershipServiceV2") MembershipService membershipService;

    @Transactional
    public PaymentSyncOfflineResponseDto sync(MembershipPremiumPaymentSyncOfflineRequest request) {
        validate(request);

        var existingBatch = paymentBatchRepository.findByDeviceIdAndLocalPaymentBatchId(
                request.getDeviceId(),
                request.getLocalPaymentBatchId()
        );

        if (existingBatch.isPresent()) {
            PaymentBatchEntity batch = existingBatch.get();

            return PaymentSyncOfflineResponseDto.builder()
                    .syncStatus("ALREADY_SYNCED")
                    .paymentBatchId(batch.getId())
                    .paymentBatchNo(batch.getPaymentBatchNo())
                    .membershipId(batch.getMembershipId())
                    .receipts(List.of())
                    .warnings(List.of("Payment batch already synced"))
                    .build();
        }

        if (paymentBatchRepository.existsByPaymentBatchNo(request.getPaymentBatchNo())) {
            PaymentBatchEntity batch = paymentBatchRepository.findByPaymentBatchNo(request.getPaymentBatchNo())
                    .orElseThrow();

            return PaymentSyncOfflineResponseDto.builder()
                    .syncStatus("ALREADY_SYNCED")
                    .paymentBatchId(batch.getId())
                    .paymentBatchNo(batch.getPaymentBatchNo())
                    .membershipId(batch.getMembershipId())
                    .receipts(List.of())
                    .warnings(List.of("Payment batch number already exists"))
                    .build();
        }

        PaymentBatchEntity batch = createPaymentBatch(request);

        List<ReceiptResponseDto> syncedReceipts = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Long monthlyPremiumCents = determineMonthlyPremiumCents(request.getMembershipId());

        for (PremiumReceiptOfflineDto offlineReceipt : request.getReceipts()) {
            if (receiptRepository.existsByReceiptNo(offlineReceipt.getReceiptNo())) {
                ReceiptEntity existingReceipt = receiptRepository.findByReceiptNo(offlineReceipt.getReceiptNo())
                        .orElseThrow();

                syncedReceipts.add(receiptService.getReceipt(existingReceipt.getId()));
                warnings.add("Receipt already existed: " + offlineReceipt.getReceiptNo());
                continue;
            }

            MembershipPremiumEntity premium = membershipPremiumService.findOrCreatePremium(
                    request.getMembershipId(),
                    offlineReceipt.getPeriodYYYYMM(),
                    monthlyPremiumCents,
                    request.getCreatedBy()
            );

            if (premium.getStatus() == PremiumStatus.PAID) {
                warnings.add("Period already paid: " + offlineReceipt.getPeriodYYYYMM());
            }

            ReceiptEntity receipt = createReceiptFromOfflineRequest(batch, request, offlineReceipt);

            ReceiptAllocationEntity allocation = receiptService.createAllocation(
                    receipt.getId(),
                    ReceiptAllocationType.MEMBERSHIP_PREMIUM,
                    premium.getId(),
                    request.getMembershipId() + "-" + offlineReceipt.getPeriodYYYYMM(),
                    offlineReceipt.getPeriodYYYYMM(),
                    request.getMembershipId(),
                    offlineReceipt.getAmountCents(),
                    request.getCreatedBy()
            );

            if (premium.getStatus() != PremiumStatus.PAID) {
                membershipPremiumService.applyPayment(
                        premium,
                        offlineReceipt.getAmountCents(),
                        request.getCreatedBy()
                );
            }

            syncedReceipts.add(receiptMapper.toDto(receipt, List.of(allocation)));
        }

        String syncStatus = warnings.isEmpty() ? "SYNCED" : "SYNCED_WITH_WARNINGS";

        if (!warnings.isEmpty()) {
            batch.setSyncStatus(SyncStatus.SYNCED_WITH_WARNINGS);
            paymentBatchRepository.save(batch);
        }

        return PaymentSyncOfflineResponseDto.builder()
                .syncStatus(syncStatus)
                .paymentBatchId(batch.getId())
                .paymentBatchNo(batch.getPaymentBatchNo())
                .membershipId(batch.getMembershipId())
                .paidUpToPeriod(getLastPeriod(syncedReceipts))
                .receipts(syncedReceipts)
                .warnings(warnings)
                .build();
    }

    private PaymentBatchEntity createPaymentBatch(MembershipPremiumPaymentSyncOfflineRequest request) {
        PaymentBatchEntity batch = new PaymentBatchEntity();
        batch.setPaymentBatchNo(request.getPaymentBatchNo());
        batch.setSourceType(ReceiptSourceType.MEMBERSHIP_PREMIUM);
        batch.setMembershipId(request.getMembershipId());
        batch.setPaymentMethod(request.getPaymentMethod());
        batch.setTotalAmountCents(request.getTotalAmountCents());
        batch.setPaymentDate(request.getPaymentDate() == null ? LocalDateTime.now() : request.getPaymentDate());
        batch.setLocation(request.getLocation());
        batch.setEmployeeResponsible(request.getEmployeeResponsible());
        batch.setDeviceId(request.getDeviceId());
        batch.setTerminalId(request.getTerminalId());
        batch.setLocalPaymentBatchId(request.getLocalPaymentBatchId());
        batch.setStatus(PaymentBatchStatus.POSTED);
        batch.setSyncStatus(SyncStatus.SYNCED);
        batch.setCreatedAt(LocalDateTime.now());
        batch.setCreatedBy(request.getCreatedBy());

        return paymentBatchRepository.save(batch);
    }

    private ReceiptEntity createReceiptFromOfflineRequest(
            PaymentBatchEntity batch,
            MembershipPremiumPaymentSyncOfflineRequest request,
            PremiumReceiptOfflineDto offlineReceipt
    ) {
        ReceiptEntity receipt = new ReceiptEntity();
        receipt.setReceiptNo(offlineReceipt.getReceiptNo());
        receipt.setPaymentBatchId(batch.getId());
        receipt.setPaymentBatchNo(batch.getPaymentBatchNo());
        receipt.setSourceType(ReceiptSourceType.MEMBERSHIP_PREMIUM);
        receipt.setMembershipId(request.getMembershipId());
        receipt.setReceiptDate(batch.getPaymentDate());
        receipt.setPaymentMethod(request.getPaymentMethod());
        receipt.setTotalAmountCents(offlineReceipt.getAmountCents());
        receipt.setStatus(ReceiptStatus.POSTED);
        receipt.setSyncStatus(SyncStatus.SYNCED);
        receipt.setLocation(request.getLocation());
        receipt.setEmployeeResponsible(request.getEmployeeResponsible());
        receipt.setDeviceId(request.getDeviceId());
        receipt.setTerminalId(request.getTerminalId());
        receipt.setPrinted(Boolean.TRUE.equals(offlineReceipt.getPrinted()));
        receipt.setPrintCount(Boolean.TRUE.equals(offlineReceipt.getPrinted()) ? 1 : 0);
        receipt.setCreatedAt(LocalDateTime.now());
        receipt.setCreatedBy(request.getCreatedBy());

        return receiptService.saveReceipt(receipt);
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

    private Long determineMonthlyPremiumCents(String membershipId) {

        return membershipService.getMembershipById(membershipId).get().getPremiumCents();
    }

    private void validate(MembershipPremiumPaymentSyncOfflineRequest request) {
        if (request.getDeviceId() == null || request.getDeviceId().isBlank()) {
            throw new RuntimeException("deviceId is required");
        }

        if (request.getLocalPaymentBatchId() == null || request.getLocalPaymentBatchId().isBlank()) {
            throw new RuntimeException("localPaymentBatchId is required");
        }

        if (request.getPaymentBatchNo() == null || request.getPaymentBatchNo().isBlank()) {
            throw new RuntimeException("paymentBatchNo is required");
        }

        if (request.getMembershipId() == null || request.getMembershipId().isBlank()) {
            throw new RuntimeException("membershipId is required");
        }

        if (request.getReceipts() == null || request.getReceipts().isEmpty()) {
            throw new RuntimeException("At least one receipt is required");
        }
    }
}