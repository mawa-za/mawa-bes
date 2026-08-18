package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.ReceiptPrintDto;
import za.co.mawa.bes.dto.v2.ReceiptResponseDto;
import za.co.mawa.bes.dto.v2.ReceiptVerificationDto;
import za.co.mawa.bes.entity.v2.ReceiptAllocationEntity;
import za.co.mawa.bes.entity.v2.ReceiptEntity;
import za.co.mawa.bes.enums.ReceiptAllocationType;
import za.co.mawa.bes.enums.ReceiptStatus;
import za.co.mawa.bes.enums.ReceiptSourceType;
import za.co.mawa.bes.repository.v2.ReceiptAllocationRepository;
import za.co.mawa.bes.repository.v2.ReceiptRepository;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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

    public ReceiptVerificationDto verifyReceipt(String traceId) {
        String normalized = traceId == null ? "" : traceId.trim().toUpperCase(Locale.ROOT);
        ReceiptEntity receipt = receiptRepository.findByTraceId(normalized)
                .orElseThrow(() -> new RuntimeException("Receipt trace ID not found: " + normalized));
        return ReceiptVerificationDto.builder()
                .traceId(receipt.getTraceId())
                .receiptNo(receipt.getReceiptNo())
                .receiptDate(receipt.getReceiptDate())
                .amountCents(receipt.getTotalAmountCents())
                .status(receipt.getStatus() == null ? null : receipt.getStatus().name())
                .authentic(true)
                .build();
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
        if (receipt.getMembershipId() != null && !receipt.getMembershipId().isBlank()) {
            String membershipReference = receipt.getMembershipId().trim();
            var rows=jdbcTemplate.queryForList("""
                SELECT m.id membership_id,m.membership_no,
                       TRIM(CONCAT_WS(' ', NULLIF(p.name2,''), NULLIF(p.name3,''), NULLIF(p.name1,''))) member_name,
                       (SELECT pi.value FROM partner_identity pi WHERE pi.partner=p.id
                         AND UPPER(TRIM(pi.type)) IN ('SA-ID','PASSPORT')
                         ORDER BY CASE WHEN UPPER(TRIM(pi.type))='SA-ID' THEN 0 ELSE 1 END, pi.type, pi.value LIMIT 1) identity_number,
                       mp.name plan_name
                  FROM membership m
                  JOIN partner p ON p.id=m.member_id
             LEFT JOIN membership_plan mp ON mp.id=m.plan_id
                 WHERE m.id=? OR m.old_id=?
                 ORDER BY CASE WHEN m.id=? THEN 0 ELSE 1 END
                 LIMIT 1
                """, membershipReference, membershipReference, membershipReference);
            if(!rows.isEmpty()) member=rows.get(0);
        }

        java.util.Map<String,Object> groupSociety = new java.util.HashMap<>();
        if (receipt.getSourceType() == ReceiptSourceType.GROUP_SOCIETY && firstAllocation != null
                && firstAllocation.getReferenceNo() != null && !firstAllocation.getReferenceNo().isBlank()) {
            var rows = jdbcTemplate.queryForList("""
                SELECT g.id group_society_id, g.group_no group_society_no,
                       TRIM(CONCAT_WS(' ',NULLIF(p.name1,''),NULLIF(p.name2,''),NULLIF(p.name3,''))) group_society_name,
                       COALESCE(g.society_type,'Group Society') group_society_type
                  FROM group_society g
                  JOIN partner p ON p.id=g.partner_id
                 WHERE g.group_no=?
                 LIMIT 1
                """, firstAllocation.getReferenceNo());
            if (!rows.isEmpty()) groupSociety = rows.get(0);
        }

        java.util.Map<String,Object> invoice = new java.util.HashMap<>();
        if (receipt.getSourceType() == ReceiptSourceType.INVOICE && firstAllocation != null
                && firstAllocation.getReferenceId() != null && !firstAllocation.getReferenceId().isBlank()) {
            var rows = jdbcTemplate.queryForList("""
                SELECT i.id invoice_id, i.invoice_no, i.external_ref,
                       p.number customer_number,
                       TRIM(CONCAT_WS(' ',NULLIF(p.name2,''),NULLIF(p.name3,''),NULLIF(p.name1,''))) customer_name
                  FROM invoice i
             LEFT JOIN partner p ON p.id=i.partner_id
                 WHERE i.id=?
                """, firstAllocation.getReferenceId());
            if (!rows.isEmpty()) invoice = rows.get(0);
        }
        return ReceiptPrintDto.builder()
                .receiptNo(receipt.getReceiptNo())
                .traceId(receipt.getTraceId())
                .paymentBatchNo(receipt.getPaymentBatchNo())
                .sourceType(receipt.getSourceType() == null ? null : receipt.getSourceType().name())
                .membershipId(java.util.Objects.toString(member.get("membership_id"), receipt.getMembershipId()))
                .memberName(java.util.Objects.toString(member.get("member_name"),""))
                .membershipNo(java.util.Objects.toString(member.get("membership_no"), receipt.getMembershipId() == null ? "" : receipt.getMembershipId()))
                .identityNumber(java.util.Objects.toString(member.get("identity_number"),""))
                .planName(java.util.Objects.toString(member.get("plan_name"),""))
                .premiumPeriodYYYYMM(firstAllocation == null ? null : firstAllocation.getPeriodYYYYMM())
                .periodDescription(formatPeriodDescription(
                        firstAllocation == null ? null : firstAllocation.getPeriodYYYYMM()))
                .invoiceId(java.util.Objects.toString(invoice.get("invoice_id"),
                        firstAllocation != null && firstAllocation.getAllocationType() == za.co.mawa.bes.enums.ReceiptAllocationType.INVOICE
                                ? firstAllocation.getReferenceId() : ""))
                .invoiceNo(java.util.Objects.toString(invoice.get("invoice_no"),
                        firstAllocation != null && firstAllocation.getAllocationType() == za.co.mawa.bes.enums.ReceiptAllocationType.INVOICE
                                ? firstAllocation.getReferenceNo() : ""))
                .invoiceReference(java.util.Objects.toString(invoice.get("external_ref"), ""))
                .customerName(java.util.Objects.toString(invoice.get("customer_name"), ""))
                .customerNumber(java.util.Objects.toString(invoice.get("customer_number"), ""))
                .groupSocietyId(java.util.Objects.toString(groupSociety.get("group_society_id"), ""))
                .groupSocietyNo(java.util.Objects.toString(groupSociety.get("group_society_no"),
                        firstAllocation != null && firstAllocation.getAllocationType() == za.co.mawa.bes.enums.ReceiptAllocationType.GROUP_SOCIETY_BALANCE
                                ? firstAllocation.getReferenceNo() : ""))
                .groupSocietyName(java.util.Objects.toString(groupSociety.get("group_society_name"), ""))
                .groupSocietyType(java.util.Objects.toString(groupSociety.get("group_society_type"), ""))
                .referenceType(firstAllocation == null || firstAllocation.getAllocationType() == null
                        ? receipt.getSourceType() == null ? "" : receipt.getSourceType().name()
                        : firstAllocation.getAllocationType().name())
                .referenceNo(firstAllocation == null ? "" : firstAllocation.getReferenceNo())
                .amountCents(firstAllocation == null ? receipt.getTotalAmountCents() : firstAllocation.getAmountCents())
                .paymentMethod(receipt.getPaymentMethod())
                .receiptDate(receipt.getReceiptDate())
                .location(firstNonBlank(receipt.getLocationName(), receipt.getLocation()))
                .employeeResponsible(resolveCashierDisplayName(receipt))
                .deviceId(receipt.getDeviceId())
                .terminalId(receipt.getTerminalId())
                .syncStatus(receipt.getSyncStatus() == null ? null : receipt.getSyncStatus().name())
                .status(receipt.getStatus() == null ? null : receipt.getStatus().name())
                .printCount(receipt.getPrintCount())
                .build();
    }

    private String resolveCashierDisplayName(ReceiptEntity receipt) {
        String explicitCollector = firstNonBlank(receipt.getOriginalCollector());
        if (!explicitCollector.isBlank()) {
            String resolved = resolveUserDisplayName(explicitCollector);
            if (!resolved.isBlank()) return resolved;
            if (!looksLikeUserIdentifier(explicitCollector)) return explicitCollector;
        }

        for (String candidate : List.of(
                firstNonBlank(receipt.getEmployeeResponsible()),
                firstNonBlank(receipt.getCapturedBy()),
                firstNonBlank(receipt.getCreatedBy()))) {
            if (candidate.isBlank()) continue;
            String resolved = resolveUserDisplayName(candidate);
            if (!resolved.isBlank()) return resolved;
            if (!looksLikeUserIdentifier(candidate)) return candidate;
        }
        return "System";
    }

    private String resolveUserDisplayName(String userReference) {
        if (userReference == null || userReference.isBlank()) return "";
        try {
            var rows = jdbcTemplate.queryForList("""
                    SELECT TRIM(CONCAT_WS(' ', NULLIF(p.name2,''), NULLIF(p.name3,''), NULLIF(p.name1,''))) display_name,
                           u.username
                      FROM `user` u
                 LEFT JOIN partner p ON p.id=u.partner
                     WHERE u.id=? OR LOWER(TRIM(u.username))=LOWER(TRIM(?))
                     LIMIT 1
                    """, userReference, userReference);
            if (rows.isEmpty()) return "";
            String display = java.util.Objects.toString(rows.get(0).get("display_name"), "").trim();
            if (!display.isBlank()) return display;
            return java.util.Objects.toString(rows.get(0).get("username"), "").trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean looksLikeUserIdentifier(String value) {
        if (value == null) return false;
        String normalized = value.trim();
        return normalized.matches("(?i)^[0-9a-f]{32}$")
                || normalized.matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }


    private String formatPeriodDescription(String rawPeriods) {
        if (rawPeriods == null || rawPeriods.isBlank()) {
            return "-";
        }
        return Arrays.stream(rawPeriods.split("[,;|]"))
                .map(String::trim)
                .filter(period -> !period.isEmpty())
                .distinct()
                .map(this::formatPeriodCode)
                .collect(Collectors.joining(", "));
    }

    private String formatPeriodCode(String period) {
        try {
            YearMonth yearMonth = YearMonth.parse(
                    period,
                    DateTimeFormatter.ofPattern("yyyyMM", Locale.ENGLISH));
            return yearMonth.format(
                    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
                    + " (" + period + ")";
        } catch (DateTimeParseException ignored) {
            return period;
        }
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
