package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.GroupSocietyPaymentSyncOfflineRequest;
import za.co.mawa.bes.dto.v2.PaymentSyncOfflineResponseDto;
import za.co.mawa.bes.dto.v2.PremiumReceiptOfflineDto;
import za.co.mawa.bes.dto.v2.ReceiptResponseDto;
import za.co.mawa.bes.dto.v2.group.GroupSocietyPaymentRequest;
import za.co.mawa.bes.entity.v2.GroupSocietyAccountTxnEntity;
import za.co.mawa.bes.entity.v2.GroupSocietyEntity;
import za.co.mawa.bes.entity.v2.PaymentBatchEntity;
import za.co.mawa.bes.entity.v2.ReceiptAllocationEntity;
import za.co.mawa.bes.entity.v2.ReceiptEntity;
import za.co.mawa.bes.enums.PaymentBatchStatus;
import za.co.mawa.bes.enums.ReceiptAllocationType;
import za.co.mawa.bes.enums.ReceiptSourceType;
import za.co.mawa.bes.enums.ReceiptStatus;
import za.co.mawa.bes.enums.SyncStatus;
import za.co.mawa.bes.repository.v2.PaymentBatchRepository;
import za.co.mawa.bes.repository.v2.ReceiptRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupSocietyPaymentSyncOfflineService {

    private final PaymentBatchRepository paymentBatchRepository;
    private final ReceiptRepository receiptRepository;
    private final GroupSocietyService groupSocietyService;
    private final ReceiptService receiptService;
    private final ReceiptMapper receiptMapper;

    @Transactional
    public PaymentSyncOfflineResponseDto sync(GroupSocietyPaymentSyncOfflineRequest request) {
        validate(request);

        GroupSocietyEntity society = groupSocietyService.getById(request.getGroupSocietyId());

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
                    .groupSocietyId(request.getGroupSocietyId())
                    .partnerId(society.getPartnerId())
                    .receipts(List.of())
                    .warnings(List.of("Group society payment batch already synced"))
                    .build();
        }

        if (paymentBatchRepository.existsByPaymentBatchNo(request.getPaymentBatchNo())) {
            PaymentBatchEntity batch = paymentBatchRepository.findByPaymentBatchNo(request.getPaymentBatchNo())
                    .orElseThrow();

            return PaymentSyncOfflineResponseDto.builder()
                    .syncStatus("ALREADY_SYNCED")
                    .paymentBatchId(batch.getId())
                    .paymentBatchNo(batch.getPaymentBatchNo())
                    .groupSocietyId(request.getGroupSocietyId())
                    .partnerId(society.getPartnerId())
                    .receipts(List.of())
                    .warnings(List.of("Payment batch number already exists"))
                    .build();
        }

        PaymentBatchEntity batch = createPaymentBatch(request, society);

        List<ReceiptResponseDto> syncedReceipts = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (PremiumReceiptOfflineDto offlineReceipt : request.getReceipts()) {
            if (receiptRepository.existsByReceiptNo(offlineReceipt.getReceiptNo())) {
                ReceiptEntity existingReceipt = receiptRepository.findByReceiptNo(offlineReceipt.getReceiptNo())
                        .orElseThrow();

                syncedReceipts.add(receiptService.getReceipt(existingReceipt.getId()));
                warnings.add("Receipt already existed: " + offlineReceipt.getReceiptNo());
                continue;
            }

            ReceiptEntity receipt = createReceiptFromOfflineRequest(batch, request, society, offlineReceipt);
            GroupSocietyAccountTxnEntity accountTxn = recordAccountCredit(request, receipt, offlineReceipt);

            ReceiptAllocationEntity allocation = receiptService.createAllocation(
                    receipt.getId(),
                    ReceiptAllocationType.GROUP_SOCIETY_BALANCE,
                    accountTxn.getId(),
                    society.getGroupNo(),
                    null,
                    null,
                    offlineReceipt.getAmountCents(),
                    request.getCreatedBy()
            );

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
                .groupSocietyId(request.getGroupSocietyId())
                .partnerId(society.getPartnerId())
                .paidUpToPeriod(null)
                .receipts(syncedReceipts)
                .warnings(warnings)
                .build();
    }

    private PaymentBatchEntity createPaymentBatch(
            GroupSocietyPaymentSyncOfflineRequest request,
            GroupSocietyEntity society
    ) {
        PaymentBatchEntity batch = new PaymentBatchEntity();
        batch.setPaymentBatchNo(request.getPaymentBatchNo());
        batch.setSourceType(ReceiptSourceType.GROUP_SOCIETY);
        batch.setReceivedFromPartnerId(society.getPartnerId());
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
            GroupSocietyPaymentSyncOfflineRequest request,
            GroupSocietyEntity society,
            PremiumReceiptOfflineDto offlineReceipt
    ) {
        ReceiptEntity receipt = new ReceiptEntity();
        receipt.setReceiptNo(offlineReceipt.getReceiptNo());
        receipt.setPaymentBatchId(batch.getId());
        receipt.setPaymentBatchNo(batch.getPaymentBatchNo());
        receipt.setSourceType(ReceiptSourceType.GROUP_SOCIETY);
        receipt.setReceivedFromPartnerId(society.getPartnerId());
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

    private GroupSocietyAccountTxnEntity recordAccountCredit(
            GroupSocietyPaymentSyncOfflineRequest request,
            ReceiptEntity receipt,
            PremiumReceiptOfflineDto offlineReceipt
    ) {
        GroupSocietyPaymentRequest paymentRequest = new GroupSocietyPaymentRequest();
        paymentRequest.setAmountCents(offlineReceipt.getAmountCents());
        paymentRequest.setPaymentDate(toDateOnly(request.getPaymentDate()));
        paymentRequest.setPaymentMethod(request.getPaymentMethod());
        paymentRequest.setPeriod(null);
        paymentRequest.setReferenceId(receipt.getId());
        paymentRequest.setReferenceNo(receipt.getReceiptNo());
        paymentRequest.setNotes("Offline MawaPay group society receipt " + receipt.getReceiptNo());

        return groupSocietyService.recordPayment(request.getGroupSocietyId(), paymentRequest);
    }

    private LocalDate toDateOnly(LocalDateTime value) {
        return value == null ? LocalDate.now() : value.toLocalDate();
    }

    private void validate(GroupSocietyPaymentSyncOfflineRequest request) {
        if (request.getDeviceId() == null || request.getDeviceId().isBlank()) {
            throw new RuntimeException("deviceId is required");
        }

        if (request.getLocalPaymentBatchId() == null || request.getLocalPaymentBatchId().isBlank()) {
            throw new RuntimeException("localPaymentBatchId is required");
        }

        if (request.getPaymentBatchNo() == null || request.getPaymentBatchNo().isBlank()) {
            throw new RuntimeException("paymentBatchNo is required");
        }

        if (request.getGroupSocietyId() == null || request.getGroupSocietyId().isBlank()) {
            throw new RuntimeException("groupSocietyId is required");
        }

        if (request.getReceipts() == null || request.getReceipts().isEmpty()) {
            throw new RuntimeException("At least one receipt is required");
        }
    }
}
