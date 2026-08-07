package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.dto.v2.group.*;
import za.co.mawa.bes.entity.v2.GroupSocietyEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.repository.v2.GroupSocietyRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GroupSocietyFuneralClaimService {
    private final GroupSocietyRepository groupSocietyRepository;
    private final GroupSocietyService groupSocietyService;
    private final ApprovalService approvalService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public GroupSocietyFuneralClaimResponse submit(String funeralServiceId,
                                                   GroupSocietyFuneralClaimRequest request) {
        if (request == null || blank(request.getGroupSocietyId())) {
            throw new IllegalArgumentException("groupSocietyId is required");
        }
        if (blank(request.getDeceasedFirstNames()) || blank(request.getDeceasedLastName())) {
            throw new IllegalArgumentException("Deceased first names and last name are required");
        }
        if (blank(request.getIdentityType()) || blank(request.getIdentityNumber())) {
            throw new IllegalArgumentException("Deceased identity type and identity number are required");
        }
        String identityType = request.getIdentityType().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("SA-ID", "PASSPORT", "OTHER").contains(identityType)) {
            throw new IllegalArgumentException("identityType must be SA-ID, PASSPORT or OTHER");
        }
        if (request.getRequestedCoverCents() == null || request.getRequestedCoverCents() <= 0) {
            throw new IllegalArgumentException("requestedCoverCents must be greater than zero");
        }
        List<Long> funeralTotals = jdbcTemplate.query(
                "SELECT COALESCE(total_amount_cents, 0) FROM funeral_service WHERE id=?",
                (rs, rowNum) -> rs.getLong(1),
                funeralServiceId);
        if (funeralTotals.isEmpty()) {
            throw new IllegalArgumentException("Funeral service not found: " + funeralServiceId);
        }
        long funeralTotal = funeralTotals.get(0);
        if (request.getRequestedCoverCents() > funeralTotal) {
            throw new IllegalArgumentException("Requested group society cover cannot exceed the funeral total");
        }
        Integer existingClaimCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM group_society_funeral_claim
                 WHERE funeral_service_id=? AND status IN ('PENDING_APPROVAL','APPROVED')
                """, Integer.class, funeralServiceId);
        if (existingClaimCount != null && existingClaimCount > 0) {
            throw new IllegalStateException("This funeral arrangement already has a group society cover request");
        }

        GroupSocietyEntity society = groupSocietyRepository.findByIdForUpdate(request.getGroupSocietyId())
                .orElseThrow(() -> new IllegalArgumentException("Group society not found: " + request.getGroupSocietyId()));
        if (!"ACTIVE".equalsIgnoreCase(society.getStatus())) {
            throw new IllegalStateException("Only an ACTIVE group society can fund a funeral");
        }
        if (value(society.getAvailableBalanceCents()) < request.getRequestedCoverCents()) {
            throw new IllegalArgumentException("Requested cover exceeds the available group society balance");
        }

        String id = UUID.randomUUID().toString();
        String claimNo = "GSC-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + id.substring(0, 4).toUpperCase(Locale.ROOT);
        String actor = actor(request.getRequestedBy());
        jdbcTemplate.update("""
                INSERT INTO group_society_funeral_claim(
                    id,claim_no,funeral_service_id,group_society_id,
                    deceased_first_names,deceased_last_name,identity_type,identity_number,
                    requested_cover_cents,approved_cover_cents,status,notes,requested_by,created_at
                ) VALUES(?,?,?,?,?,?,?,?,?,0,'PENDING_APPROVAL',?,?,CURRENT_TIMESTAMP)
                """, id, claimNo, funeralServiceId, society.getId(),
                request.getDeceasedFirstNames().trim(), request.getDeceasedLastName().trim(),
                identityType,
                request.getIdentityNumber().trim(), request.getRequestedCoverCents(),
                request.getNotes(), actor);

        String deceasedName = (request.getDeceasedFirstNames() + " " + request.getDeceasedLastName()).trim();
        String societyName = groupSocietyName(society.getId(), society.getGroupNo());
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("claimNumber", claimNo);
        summary.put("groupSocietyNumber", society.getGroupNo());
        summary.put("groupSocietyName", societyName);
        summary.put("deceasedName", deceasedName);
        summary.put("identityType", identityType);
        summary.put("identityNumber", request.getIdentityNumber().trim());
        summary.put("requestedCoverCents", request.getRequestedCoverCents());
        summary.put("funeralTotalCents", funeralTotal);
        summary.put("availableSocietyBalanceCents", society.getAvailableBalanceCents());
        summary.put("notes", request.getNotes());
        payload.put("requestSummary", summary);
        payload.put("funeralServiceId", funeralServiceId);
        payload.put("groupSocietyId", society.getId());
        payload.put("attachmentObjectIds", List.of(funeralServiceId, society.getId(), id));

        ApprovalSubmitRequest approval = new ApprovalSubmitRequest();
        approval.setApprovalType(ApprovalType.GROUP_SOCIETY_FUNERAL_CLAIM);
        approval.setReferenceId(id);
        approval.setReferenceNo(claimNo);
        approval.setTitle("Group society funeral cover - " + societyName + " (" + society.getGroupNo()
                + ") - " + deceasedName);
        approval.setDescription("Review the deceased details, requested cover, funeral total, and available group society balance.");
        approval.setRequesterId(actor);
        approval.setPayloadJson(toJson(payload));
        var response = approvalService.submitForApproval(approval);
        jdbcTemplate.update("UPDATE group_society_funeral_claim SET approval_request_id=? WHERE id=?",
                response.getId(), id);
        return get(id);
    }

    public List<GroupSocietyFuneralClaimResponse> findByFuneralService(String funeralServiceId) {
        return jdbcTemplate.query("""
                SELECT c.*,g.group_no,
                       TRIM(CONCAT_WS(' ',NULLIF(p.name1,''),NULLIF(p.name2,''),NULLIF(p.name3,''))) society_name
                  FROM group_society_funeral_claim c
                  JOIN group_society g ON g.id=c.group_society_id
                  JOIN partner p ON p.id=g.partner_id
                 WHERE c.funeral_service_id=?
                 ORDER BY c.created_at DESC
                """, (rs, row) -> map(rs), funeralServiceId);
    }

    public GroupSocietyFuneralClaimResponse get(String id) {
        List<GroupSocietyFuneralClaimResponse> rows = jdbcTemplate.query("""
                SELECT c.*,g.group_no,
                       TRIM(CONCAT_WS(' ',NULLIF(p.name1,''),NULLIF(p.name2,''),NULLIF(p.name3,''))) society_name
                  FROM group_society_funeral_claim c
                  JOIN group_society g ON g.id=c.group_society_id
                  JOIN partner p ON p.id=g.partner_id
                 WHERE c.id=?
                """, (rs, row) -> map(rs), id);
        if (rows.isEmpty()) throw new IllegalArgumentException("Group society funeral claim not found: " + id);
        return rows.get(0);
    }

    @Transactional
    public void complete(String claimId, boolean approved, String actor) {
        Map<String,Object> claim = jdbcTemplate.queryForMap(
                "SELECT * FROM group_society_funeral_claim WHERE id=? FOR UPDATE", claimId);
        if (!"PENDING_APPROVAL".equalsIgnoreCase(Objects.toString(claim.get("status"), ""))) return;
        if (!approved) {
            jdbcTemplate.update("""
                    UPDATE group_society_funeral_claim
                       SET status='REJECTED',completed_by=?,completed_at=CURRENT_TIMESTAMP
                     WHERE id=?
                    """, actor, claimId);
            return;
        }
        String groupSocietyId = Objects.toString(claim.get("group_society_id"));
        long amount = ((Number) claim.get("requested_cover_cents")).longValue();
        String claimNo = Objects.toString(claim.get("claim_no"));

        GroupSocietyClaimDebitRequest debit = new GroupSocietyClaimDebitRequest();
        debit.setClaimId(claimId);
        debit.setClaimNo(claimNo);
        debit.setClaimDate(LocalDate.now());
        debit.setAmountCents(amount);
        debit.setNotes("Approved group society funeral cover");
        groupSocietyService.debitClaim(groupSocietyId, debit);

        jdbcTemplate.update("""
                UPDATE group_society_funeral_claim
                   SET approved_cover_cents=?,status='APPROVED',
                       completed_by=?,completed_at=CURRENT_TIMESTAMP
                 WHERE id=?
                """, amount, actor, claimId);
    }

    private GroupSocietyFuneralClaimResponse map(java.sql.ResultSet rs) throws java.sql.SQLException {
        var created = rs.getTimestamp("created_at");
        return GroupSocietyFuneralClaimResponse.builder()
                .id(rs.getString("id"))
                .claimNo(rs.getString("claim_no"))
                .funeralServiceId(rs.getString("funeral_service_id"))
                .groupSocietyId(rs.getString("group_society_id"))
                .groupNo(rs.getString("group_no"))
                .societyName(rs.getString("society_name"))
                .deceasedFirstNames(rs.getString("deceased_first_names"))
                .deceasedLastName(rs.getString("deceased_last_name"))
                .identityType(rs.getString("identity_type"))
                .identityNumber(rs.getString("identity_number"))
                .requestedCoverCents(rs.getLong("requested_cover_cents"))
                .approvedCoverCents(rs.getLong("approved_cover_cents"))
                .status(rs.getString("status"))
                .approvalRequestId(rs.getString("approval_request_id"))
                .paymentRequestId(rs.getString("payment_request_id"))
                .notes(rs.getString("notes"))
                .createdAt(created == null ? null : created.toLocalDateTime())
                .build();
    }

    private String groupSocietyName(String societyId, String fallback) {
        List<String> values = jdbcTemplate.query("""
                SELECT COALESCE(
                    NULLIF(TRIM(CONCAT_WS(' ', NULLIF(p.name2,''), NULLIF(p.name3,''), NULLIF(p.name1,''))), ''),
                    NULLIF(TRIM(p.name1), ''),
                    g.group_no
                )
                  FROM group_society g
                  JOIN partner p ON p.id = g.partner_id
                 WHERE g.id = ?
                """, (rs, rowNum) -> rs.getString(1), societyId);
        return values.isEmpty() || values.get(0) == null || values.get(0).isBlank()
                ? fallback : values.get(0).trim();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create the group society funeral approval details", exception);
        }
    }

    private long value(Long value) { return value == null ? 0L : value; }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String actor(String value) { return blank(value) ? "SYSTEM" : value.trim(); }
}
