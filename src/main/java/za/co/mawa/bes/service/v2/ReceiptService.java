package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.ReceiptPrintDto;
import za.co.mawa.bes.dto.v2.ReceiptResponseDto;
import za.co.mawa.bes.entity.v2.ReceiptAllocationEntity;
import za.co.mawa.bes.entity.v2.ReceiptEntity;
import za.co.mawa.bes.enums.ReceiptAllocationType;
import za.co.mawa.bes.enums.ReceiptStatus;
import za.co.mawa.bes.repository.v2.ReceiptAllocationRepository;
import za.co.mawa.bes.repository.v2.ReceiptRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service(value = "ReceiptServiceV2")
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final ReceiptAllocationRepository receiptAllocationRepository;
    private final ReceiptMapper receiptMapper;

    public ReceiptEntity saveReceipt(ReceiptEntity receipt) {
        return receiptRepository.save(receipt);
    }

    public ReceiptEntity getReceiptEntity(String receiptId) {
        return receiptRepository.findById(receiptId)
                .orElseThrow(() -> new RuntimeException("Receipt not found: " + receiptId));
    }

    public ReceiptResponseDto getReceipt(String receiptId) {
        ReceiptEntity receipt = getReceiptEntity(receiptId);
        List<ReceiptAllocationEntity> allocations = receiptAllocationRepository.findByReceiptId(receiptId);
        return receiptMapper.toDto(receipt, allocations);
    }

    public ReceiptResponseDto getReceiptByNumber(String receiptNo) {
        ReceiptEntity receipt = receiptRepository.findByReceiptNo(receiptNo)
                .orElseThrow(() -> new RuntimeException("Receipt not found: " + receiptNo));

        List<ReceiptAllocationEntity> allocations = receiptAllocationRepository.findByReceiptId(receipt.getId());
        return receiptMapper.toDto(receipt, allocations);
    }

    public ReceiptAllocationEntity createAllocation(
            String receiptId,
            ReceiptAllocationType allocationType,
            String referenceId,
            String referenceNo,
            String periodYYYYMM,
            String membershipId,
            Long amountCents,
            String createdBy
    ) {
        ReceiptAllocationEntity allocation = new ReceiptAllocationEntity();
        allocation.setReceiptId(receiptId);
        allocation.setAllocationType(allocationType);
        allocation.setReferenceId(referenceId);
        allocation.setReferenceNo(referenceNo);
        allocation.setPeriodYYYYMM(periodYYYYMM);
        allocation.setMembershipId(membershipId);
        allocation.setAmountCents(amountCents);
        allocation.setStatus(ReceiptStatus.POSTED);
        allocation.setCreatedAt(LocalDateTime.now());
        allocation.setCreatedBy(createdBy);

        return receiptAllocationRepository.save(allocation);
    }

    @Transactional
    public ReceiptPrintDto getPrintData(String receiptId) {
        ReceiptEntity receipt = getReceiptEntity(receiptId);
        List<ReceiptAllocationEntity> allocations = receiptAllocationRepository.findByReceiptId(receiptId);

        ReceiptAllocationEntity firstAllocation = allocations.isEmpty() ? null : allocations.get(0);

        receipt.setPrinted(true);
        receipt.setPrintCount(receipt.getPrintCount() == null ? 1 : receipt.getPrintCount() + 1);
        receipt.setUpdatedAt(LocalDateTime.now());
        receiptRepository.save(receipt);

        return ReceiptPrintDto.builder()
                .receiptNo(receipt.getReceiptNo())
                .paymentBatchNo(receipt.getPaymentBatchNo())
                .sourceType(receipt.getSourceType() == null ? null : receipt.getSourceType().name())
                .membershipId(receipt.getMembershipId())
                .premiumPeriodYYYYMM(firstAllocation == null ? null : firstAllocation.getPeriodYYYYMM())
                .amountCents(firstAllocation == null ? receipt.getTotalAmountCents() : firstAllocation.getAmountCents())
                .paymentMethod(receipt.getPaymentMethod())
                .receiptDate(receipt.getReceiptDate())
                .location(receipt.getLocation())
                .employeeResponsible(receipt.getEmployeeResponsible())
                .deviceId(receipt.getDeviceId())
                .terminalId(receipt.getTerminalId())
                .syncStatus(receipt.getSyncStatus() == null ? null : receipt.getSyncStatus().name())
                .status(receipt.getStatus() == null ? null : receipt.getStatus().name())
                .printCount(receipt.getPrintCount())
                .build();
    }

    @Transactional
    public ReceiptResponseDto reverseReceipt(String receiptId, String reason, String reversedBy) {
        ReceiptEntity receipt = getReceiptEntity(receiptId);

        if (receipt.getStatus() == ReceiptStatus.REVERSED) {
            return getReceipt(receiptId);
        }

        receipt.setStatus(ReceiptStatus.REVERSED);
        receipt.setNotes(appendNote(receipt.getNotes(), "Reversed: " + reason));
        receipt.setUpdatedAt(LocalDateTime.now());
        receipt.setUpdatedBy(reversedBy);
        receiptRepository.save(receipt);

        List<ReceiptAllocationEntity> allocations = receiptAllocationRepository.findByReceiptId(receiptId);
        for (ReceiptAllocationEntity allocation : allocations) {
            allocation.setStatus(ReceiptStatus.REVERSED);
            allocation.setUpdatedAt(LocalDateTime.now());
            allocation.setUpdatedBy(reversedBy);
            receiptAllocationRepository.save(allocation);
        }

        return receiptMapper.toDto(receipt, allocations);
    }

    private String appendNote(String current, String note) {
        if (current == null || current.isBlank()) {
            return note;
        }
        return current + "\n" + note;
    }
}