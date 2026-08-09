package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.PaymentBatchResponseDto;
import za.co.mawa.bes.dto.v2.ReceiptResponseDto;
import za.co.mawa.bes.dto.v2.group.GroupSocietyPaymentRequest;
import za.co.mawa.bes.entity.v2.*;
import za.co.mawa.bes.enums.*;
import za.co.mawa.bes.repository.v2.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupSocietyPaymentService {
    private final GroupSocietyService groupSocietyService;
    private final PaymentBatchRepository paymentBatchRepository;
    private final ReceiptRepository receiptRepository;
    private final GroupSocietyAccountTxnRepository txnRepository;
    private final ReceiptService receiptService;
    private final ReceiptMapper receiptMapper;
    private final OnlineCashupService onlineCashupService;
    private final NumberAllocationService numberAllocationService;

    @Transactional
    public PaymentBatchResponseDto createPayment(String groupSocietyId, GroupSocietyPaymentRequest request) {
        if (request == null || request.getAmountCents() == null || request.getAmountCents() <= 0) {
            throw new IllegalArgumentException("amountCents must be greater than zero");
        }
        if (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()) {
            throw new IllegalArgumentException("paymentMethod is required");
        }
        GroupSocietyEntity society = groupSocietyService.getById(groupSocietyId);
        String actor = blank(request.getCreatedBy()) ? "SYSTEM" : request.getCreatedBy().trim();
        LocalDate paymentDate = request.getPaymentDate() == null ? LocalDate.now() : request.getPaymentDate();

        PaymentBatchEntity batch = new PaymentBatchEntity();
        batch.setPaymentBatchNo(numberAllocationService.allocateNumber("PAYMENT_BATCH"));
        batch.setSourceType(ReceiptSourceType.GROUP_SOCIETY);
        batch.setReceivedFromPartnerId(society.getPartnerId());
        batch.setPaymentMethod(request.getPaymentMethod().trim().toUpperCase());
        batch.setTotalAmountCents(request.getAmountCents());
        batch.setPaymentDate(paymentDate.atStartOfDay());
        batch.setLocation(request.getLocation());
        batch.setEmployeeResponsible(request.getEmployeeResponsible());
        batch.setDeviceId(blank(request.getDeviceId()) ? "ERP-ONLINE" : request.getDeviceId().trim());
        batch.setTerminalId(request.getTerminalId());
        batch.setStatus(PaymentBatchStatus.POSTED);
        batch.setSyncStatus(SyncStatus.SYNCED);
        batch.setNotes(request.getNotes());
        batch.setCreatedBy(actor);
        batch.setCreatedAt(LocalDateTime.now());
        batch = paymentBatchRepository.save(batch);

        ReceiptEntity receipt = new ReceiptEntity();
        receipt.setReceiptNo(numberAllocationService.allocateNumber("RECEIPT"));
        receipt.setPaymentBatchId(batch.getId());
        receipt.setPaymentBatchNo(batch.getPaymentBatchNo());
        receipt.setSourceType(ReceiptSourceType.GROUP_SOCIETY);
        receipt.setReceivedFromPartnerId(society.getPartnerId());
        receipt.setReceiptDate(paymentDate.atStartOfDay());
        receipt.setPaymentMethod(batch.getPaymentMethod());
        receipt.setTotalAmountCents(request.getAmountCents());
        receipt.setStatus(ReceiptStatus.POSTED);
        receipt.setSyncStatus(SyncStatus.SYNCED);
        receipt.setLocation(request.getLocation());
        receipt.setEmployeeResponsible(request.getEmployeeResponsible());
        receipt.setDeviceId(batch.getDeviceId());
        receipt.setTerminalId(request.getTerminalId());
        receipt.setCaptureSource("ERP_ONLINE");
        receipt.setCapturedBy(actor);
        receipt.setPrinted(false);
        receipt.setPrintCount(0);
        receipt.setNotes(request.getNotes());
        receipt.setCreatedAt(LocalDateTime.now());
        receipt.setCreatedBy(actor);
        receipt = receiptRepository.save(receipt);

        GroupSocietyPaymentRequest posting = new GroupSocietyPaymentRequest();
        posting.setAmountCents(request.getAmountCents());
        posting.setPaymentDate(paymentDate);
        posting.setPaymentMethod(batch.getPaymentMethod());
        posting.setReferenceId(receipt.getId());
        posting.setReferenceNo(receipt.getReceiptNo());
        posting.setNotes(request.getNotes());
        GroupSocietyAccountTxnEntity txn = groupSocietyService.recordPayment(groupSocietyId, posting);
        txn.setPaymentBatchId(batch.getId());
        txn.setReceiptId(receipt.getId());
        txn.setCreatedBy(actor);
        txn = txnRepository.save(txn);

        var allocation = receiptService.createAllocation(
                receipt.getId(),
                ReceiptAllocationType.GROUP_SOCIETY_BALANCE,
                txn.getId(),
                society.getGroupNo(),
                null,
                null,
                request.getAmountCents(),
                actor
        );
        ReceiptResponseDto receiptDto = receiptMapper.toDto(receipt, List.of(allocation));
        onlineCashupService.addReceipts(batch, List.of(receipt.getId()), actor, batch.getDeviceId());

        return PaymentBatchResponseDto.builder()
                .id(batch.getId())
                .paymentBatchNo(batch.getPaymentBatchNo())
                .sourceType(batch.getSourceType())
                .receivedFromPartnerId(batch.getReceivedFromPartnerId())
                .paymentMethod(batch.getPaymentMethod())
                .totalAmountCents(batch.getTotalAmountCents())
                .paymentDate(batch.getPaymentDate())
                .location(batch.getLocation())
                .employeeResponsible(batch.getEmployeeResponsible())
                .deviceId(batch.getDeviceId())
                .terminalId(batch.getTerminalId())
                .status(batch.getStatus())
                .syncStatus(batch.getSyncStatus())
                .receipts(List.of(receiptDto))
                .build();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
