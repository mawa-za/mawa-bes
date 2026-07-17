package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbcTemplate;

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

    @Transactional(readOnly = true)
    public ReceiptPrintDto getPrintData(String receiptId) {
        return previewPrintData(receiptId);
    }

    @Transactional(readOnly = true)
    public ReceiptPrintDto previewPrintData(String receiptId) {
        ReceiptEntity receipt = getReceiptEntity(receiptId);
        List<ReceiptAllocationEntity> allocations = receiptAllocationRepository.findByReceiptId(receiptId);

        ReceiptAllocationEntity firstAllocation = allocations.isEmpty() ? null : allocations.get(0);

        java.util.Map<String,Object> member = new java.util.HashMap<>();
        if (receipt.getMembershipId() != null) {
            var rows=jdbcTemplate.queryForList("""
                SELECT m.membership_no,TRIM(CONCAT_WS(' ', NULLIF(p.name2,''), NULLIF(p.name3,''), NULLIF(p.name1,''))) member_name,
                       (SELECT pi.value FROM partner_identity pi WHERE pi.partner=p.id ORDER BY CASE WHEN pi.type='SA-ID' THEN 0 WHEN pi.type='PASSPORT' THEN 1 ELSE 2 END, pi.type, pi.value LIMIT 1) identity_number,
                       mp.name plan_name
                  FROM membership m JOIN partner p ON p.id=m.member_id LEFT JOIN membership_plan mp ON mp.id=m.plan_id WHERE m.id=?
                """,receipt.getMembershipId());
            if(!rows.isEmpty()) member=rows.get(0);
        }
        return ReceiptPrintDto.builder()
                .receiptNo(receipt.getReceiptNo())
                .paymentBatchNo(receipt.getPaymentBatchNo())
                .sourceType(receipt.getSourceType() == null ? null : receipt.getSourceType().name())
                .membershipId(receipt.getMembershipId())
                .memberName(java.util.Objects.toString(member.get("member_name"),""))
                .membershipNo(java.util.Objects.toString(member.get("membership_no"),receipt.getMembershipId()))
                .identityNumber(java.util.Objects.toString(member.get("identity_number"),""))
                .planName(java.util.Objects.toString(member.get("plan_name"),""))
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
    public void recordSpooledPrint(String receiptId) {
        ReceiptEntity receipt = getReceiptEntity(receiptId);
        receipt.setPrinted(true);
        receipt.setPrintCount(receipt.getPrintCount() == null ? 1 : receipt.getPrintCount() + 1);
        receipt.setUpdatedAt(LocalDateTime.now());
        receiptRepository.save(receipt);
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