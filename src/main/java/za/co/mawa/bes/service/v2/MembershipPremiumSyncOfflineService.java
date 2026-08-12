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
            validateExistingBatchIdentity(batch, request);
            return syncIntoBatch(
                    batch,
                    request,
                    batch.getMembershipId(),
                    new ArrayList<>(List.of("Payment batch already existed; backend receipts were verified")),
                    true
            );
        }

        if (paymentBatchRepository.existsByPaymentBatchNo(request.getPaymentBatchNo())) {
            PaymentBatchEntity batch = paymentBatchRepository.findByPaymentBatchNo(request.getPaymentBatchNo())
                    .orElseThrow();
            validateExistingBatchIdentity(batch, request);
            return syncIntoBatch(
                    batch,
                    request,
                    batch.getMembershipId(),
                    new ArrayList<>(List.of("Payment batch number already existed; backend receipts were verified")),
                    true
            );
        }

        // MAWA Pay may still hold a legacy membership id from data that was migrated
        // after the device snapshot was created. Resolve it once and persist every new
        // ledger row against the canonical membership id so ERP and sync history point
        // at the same membership record.
        String canonicalMembershipId = membershipService.resolveMembership(request.getMembershipId()).getId();
        PaymentBatchEntity batch = createPaymentBatch(request, canonicalMembershipId);
        return syncIntoBatch(batch, request, canonicalMembershipId, new ArrayList<>(), false);
    }

    private PaymentSyncOfflineResponseDto syncIntoBatch(
            PaymentBatchEntity batch,
            MembershipPremiumPaymentSyncOfflineRequest request,
            String canonicalMembershipId,
            List<String> warnings,
            boolean recoveringExistingBatch
    ) {
        List<ReceiptResponseDto> syncedReceipts = new ArrayList<>();
        Long monthlyPremiumCents = determineMonthlyPremiumCents(canonicalMembershipId);

        for (PremiumReceiptOfflineDto offlineReceipt : request.getReceipts()) {
            var existingReceipt = receiptRepository.findByReceiptNo(offlineReceipt.getReceiptNo());
            if (existingReceipt.isPresent()) {
                ReceiptEntity receipt = existingReceipt.get();
                if (!batch.getId().equals(receipt.getPaymentBatchId())) {
                    throw new IllegalStateException(
                            "Receipt number " + offlineReceipt.getReceiptNo()
                                    + " already belongs to a different payment batch"
                    );
                }
                syncedReceipts.add(receiptService.getReceipt(receipt.getId()));
                warnings.add("Receipt already existed: " + offlineReceipt.getReceiptNo());
                continue;
            }

            MembershipPremiumEntity premium = membershipPremiumService.findOrCreatePremium(
                    canonicalMembershipId,
                    offlineReceipt.getPeriodYYYYMM(),
                    monthlyPremiumCents,
                    request.getCreatedBy()
            );

            if (premium.getStatus() == PremiumStatus.PAID) {
                warnings.add("Period already paid: " + offlineReceipt.getPeriodYYYYMM());
            }

            ReceiptEntity receipt = createReceiptFromOfflineRequest(
                    batch, request, offlineReceipt, canonicalMembershipId
            );

            ReceiptAllocationEntity allocation = receiptService.createAllocation(
                    receipt.getId(),
                    ReceiptAllocationType.MEMBERSHIP_PREMIUM,
                    premium.getId(),
                    canonicalMembershipId + "-" + offlineReceipt.getPeriodYYYYMM(),
                    offlineReceipt.getPeriodYYYYMM(),
                    canonicalMembershipId,
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
            if (recoveringExistingBatch) {
                warnings.add("Recovered missing backend receipt: " + offlineReceipt.getReceiptNo());
            }
        }

        String paidUpToPeriod = membershipService.recalculatePaidUpToPeriod(canonicalMembershipId);
        String syncStatus = warnings.isEmpty() ? "SYNCED" : "SYNCED_WITH_WARNINGS";

        batch.setSyncStatus(warnings.isEmpty() ? SyncStatus.SYNCED : SyncStatus.SYNCED_WITH_WARNINGS);
        batch.setUpdatedAt(LocalDateTime.now());
        paymentBatchRepository.save(batch);

        return PaymentSyncOfflineResponseDto.builder()
                .syncStatus(syncStatus)
                .paymentBatchId(batch.getId())
                .paymentBatchNo(batch.getPaymentBatchNo())
                .membershipId(batch.getMembershipId())
                .paidUpToPeriod(paidUpToPeriod)
                .receipts(syncedReceipts)
                .warnings(warnings)
                .build();
    }

    private void validateExistingBatchIdentity(
            PaymentBatchEntity batch,
            MembershipPremiumPaymentSyncOfflineRequest request
    ) {
        if (batch.getSourceType() != ReceiptSourceType.MEMBERSHIP_PREMIUM
                || !request.getPaymentBatchNo().equals(batch.getPaymentBatchNo())
                || !request.getDeviceId().equals(batch.getDeviceId())
                || !request.getLocalPaymentBatchId().equals(batch.getLocalPaymentBatchId())) {
            throw new IllegalStateException(
                    "Payment batch number is already linked to a different MawaPay device transaction"
            );
        }
    }

    private PaymentBatchEntity createPaymentBatch(
            MembershipPremiumPaymentSyncOfflineRequest request,
            String canonicalMembershipId
    ) {
        PaymentBatchEntity batch = new PaymentBatchEntity();
        batch.setPaymentBatchNo(request.getPaymentBatchNo());
        batch.setSourceType(ReceiptSourceType.MEMBERSHIP_PREMIUM);
        batch.setMembershipId(canonicalMembershipId);
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
            PremiumReceiptOfflineDto offlineReceipt,
            String canonicalMembershipId
    ) {
        ReceiptEntity receipt = new ReceiptEntity();
        receipt.setReceiptNo(offlineReceipt.getReceiptNo());
        receipt.setPaymentBatchId(batch.getId());
        receipt.setPaymentBatchNo(batch.getPaymentBatchNo());
        receipt.setSourceType(ReceiptSourceType.MEMBERSHIP_PREMIUM);
        receipt.setMembershipId(canonicalMembershipId);
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