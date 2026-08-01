package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.enums.ApprovalType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class ThirdPartyFuneralUnderwritingService {
    private final JdbcTemplate jdbc;
    private final ApprovalService approvalService;
    private final ObjectMapper objectMapper;

    public ThirdPartyFuneralUnderwritingService(
            JdbcTemplate jdbc,
            ApprovalService approvalService,
            ObjectMapper objectMapper
    ) {
        this.jdbc = jdbc;
        this.approvalService = approvalService;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> underwriters() {
        return jdbc.queryForList("SELECT * FROM third_party_funeral_underwriter ORDER BY name");
    }

    @Transactional
    public Map<String, Object> saveUnderwriter(Map<String, Object> body) {
        String id = id(body);
        jdbc.update("""
                INSERT INTO third_party_funeral_underwriter(
                    id, partner_id, code, name, status, integration_mode,
                    settlement_terms_days, notes
                ) VALUES(?,?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE
                    partner_id=VALUES(partner_id), code=VALUES(code), name=VALUES(name),
                    status=VALUES(status), integration_mode=VALUES(integration_mode),
                    settlement_terms_days=VALUES(settlement_terms_days), notes=VALUES(notes)
                """, id, req(body, "partnerId"), req(body, "code"), req(body, "name"),
                val(body, "status", "ACTIVE"), val(body, "integrationMode", "MANUAL"),
                num(body, "settlementTermsDays", 0), body.get("notes"));
        return jdbc.queryForMap("SELECT * FROM third_party_funeral_underwriter WHERE id=?", id);
    }

    public List<Map<String, Object>> eligibleParties(String query) {
        String q = query == null ? "" : query.trim();
        return jdbc.queryForList("""
                SELECT * FROM (
                    SELECT m.id AS membershipId,
                           m.membership_no AS membershipNo,
                           m.member_id AS coveredPartnerId,
                           NULL AS membershipDependentId,
                           'MEMBER' AS coveredPartyType,
                           COALESCE(NULLIF(TRIM(CONCAT_WS(' ',NULLIF(p.name2,''),NULLIF(p.name3,''),NULLIF(p.name1,''))), ''),
                                    p.name1, '') AS coveredPartyName,
                           COALESCE(p.number,'') AS partnerNumber,
                           COALESCE((SELECT pi.value FROM partner_identity pi WHERE pi.partner=p.id
                                     ORDER BY CASE WHEN pi.type='SA-ID' THEN 0 WHEN pi.type='PASSPORT' THEN 1 ELSE 2 END LIMIT 1),'') AS identityNumber,
                           m.status AS membershipStatus
                      FROM membership m
                      JOIN partner p ON p.id=m.member_id
                     WHERE UPPER(COALESCE(m.status,'')) NOT IN ('CANCELLED','LAPSED','TERMINATED')
                    UNION ALL
                    SELECT m.id AS membershipId,
                           m.membership_no AS membershipNo,
                           md.dependent_partner_id AS coveredPartnerId,
                           md.id AS membershipDependentId,
                           'DEPENDENT' AS coveredPartyType,
                           COALESCE(NULLIF(TRIM(CONCAT_WS(' ',NULLIF(p.name2,''),NULLIF(p.name3,''),NULLIF(p.name1,''))), ''),
                                    p.name1, '') AS coveredPartyName,
                           COALESCE(p.number,'') AS partnerNumber,
                           COALESCE((SELECT pi.value FROM partner_identity pi WHERE pi.partner=p.id
                                     ORDER BY CASE WHEN pi.type='SA-ID' THEN 0 WHEN pi.type='PASSPORT' THEN 1 ELSE 2 END LIMIT 1),'') AS identityNumber,
                           m.status AS membershipStatus
                      FROM membership_dependent md
                      JOIN membership m ON m.id=md.membership_id
                      JOIN partner p ON p.id=md.dependent_partner_id
                     WHERE COALESCE(md.active,1)=1
                       AND UPPER(COALESCE(md.status,'ACTIVE')) NOT IN ('REMOVED','REPLACED','DECEASED','INACTIVE')
                       AND UPPER(COALESCE(m.status,'')) NOT IN ('CANCELLED','LAPSED','TERMINATED')
                ) party
                WHERE ?=''
                   OR LOWER(COALESCE(party.membershipNo,'')) LIKE LOWER(CONCAT('%',?,'%'))
                   OR LOWER(COALESCE(party.coveredPartyName,'')) LIKE LOWER(CONCAT('%',?,'%'))
                   OR LOWER(COALESCE(party.partnerNumber,'')) LIKE LOWER(CONCAT('%',?,'%'))
                   OR LOWER(COALESCE(party.identityNumber,'')) LIKE LOWER(CONCAT('%',?,'%'))
                ORDER BY party.coveredPartyName, party.membershipNo
                LIMIT 100
                """, q, q, q, q, q);
    }

    public List<Map<String, Object>> covers(String status) {
        String base = """
                SELECT c.*, u.name AS underwriter_name, u.code AS underwriter_code,
                       COALESCE(NULLIF(TRIM(CONCAT_WS(' ',NULLIF(p.name2,''),NULLIF(p.name3,''),NULLIF(p.name1,''))), ''),
                                p.name1, c.holder_name) AS covered_party_name,
                       COALESCE(p.number,'') AS covered_partner_number,
                       COALESCE((SELECT pi.value FROM partner_identity pi WHERE pi.partner=c.covered_partner_id
                                 ORDER BY CASE WHEN pi.type='SA-ID' THEN 0 WHEN pi.type='PASSPORT' THEN 1 ELSE 2 END LIMIT 1),
                                c.holder_identity,'') AS covered_party_identity,
                       m.membership_no
                  FROM third_party_funeral_cover c
                  JOIN third_party_funeral_underwriter u ON u.id=c.underwriter_id
                  LEFT JOIN partner p ON p.id=c.covered_partner_id
                  LEFT JOIN membership m ON m.id=c.membership_id
                """;
        if (!StringUtils.hasText(status)) {
            return jdbc.queryForList(base + " ORDER BY c.created_at DESC");
        }
        return jdbc.queryForList(base + " WHERE c.status=? ORDER BY c.created_at DESC", status.trim().toUpperCase(Locale.ROOT));
    }

    @Transactional
    public Map<String, Object> saveCover(Map<String, Object> body) {
        String id = id(body);
        List<Map<String,Object>> existingRows = jdbc.queryForList(
                "SELECT status,pending_action FROM third_party_funeral_cover WHERE id=?", id);
        String previousStatus = existingRows.isEmpty()
                ? "DRAFT"
                : Objects.toString(existingRows.get(0).get("status"), "DRAFT");
        if (!existingRows.isEmpty()
                && StringUtils.hasText(Objects.toString(existingRows.get(0).get("pending_action"), null))) {
            throw new IllegalStateException("This funeral cover already has an approval request in progress");
        }
        String membershipId = req(body, "membershipId");
        String coveredPartnerId = req(body, "coveredPartnerId");
        String partyType = req(body, "coveredPartyType").toUpperCase(Locale.ROOT);
        if (!Set.of("MEMBER", "DEPENDENT").contains(partyType)) {
            throw new IllegalArgumentException("coveredPartyType must be MEMBER or DEPENDENT");
        }
        String dependentId = blank(body.get("membershipDependentId"));
        Map<String, Object> party = verifyEligibleParty(membershipId, coveredPartnerId, partyType, dependentId);
        long amount = num(body, "coverAmountCents", 0);
        if (amount <= 0) throw new IllegalArgumentException("Cover amount must be greater than zero");

        String partyName = Objects.toString(party.get("coveredPartyName"), "").trim();
        String identity = Objects.toString(party.get("identityNumber"), "").trim();
        jdbc.update("""
                INSERT INTO third_party_funeral_cover(
                    id, underwriter_id, external_policy_no, membership_id,
                    holder_name, holder_identity, deceased_name, deceased_identity,
                    cover_amount_cents, effective_from, effective_to, status,
                    underwriting_notes, covered_partner_id, covered_party_type,
                    membership_dependent_id
                ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) AS incoming
                ON DUPLICATE KEY UPDATE
                    underwriter_id=incoming.underwriter_id, external_policy_no=incoming.external_policy_no,
                    membership_id=incoming.membership_id, holder_name=incoming.holder_name,
                    holder_identity=incoming.holder_identity, deceased_name=incoming.deceased_name,
                    deceased_identity=incoming.deceased_identity, cover_amount_cents=incoming.cover_amount_cents,
                    effective_from=incoming.effective_from, effective_to=incoming.effective_to,
                    status=incoming.status, underwriting_notes=incoming.underwriting_notes,
                    covered_partner_id=incoming.covered_partner_id, covered_party_type=incoming.covered_party_type,
                    membership_dependent_id=incoming.membership_dependent_id
                """, id, req(body, "underwriterId"), req(body, "externalPolicyNo"), membershipId,
                partyName, identity, body.get("deceasedName"), body.get("deceasedIdentity"), amount,
                req(body, "effectiveFrom"), body.get("effectiveTo"), previousStatus,
                body.get("underwritingNotes"), coveredPartnerId, partyType,
                "DEPENDENT".equals(partyType) ? reqValue(dependentId, "membershipDependentId") : null);
        replaceBeneficiaries(id, body.get("beneficiaries"));
        submitApproval(id, "UNDERWRITE", "ACTIVE", val(body, "requestedBy", "SYSTEM"),
                Objects.toString(body.get("underwritingNotes"), null), ApprovalType.FUNERAL_UNDERWRITING,
                previousStatus);
        return getCover(id);
    }

    @Transactional
    public Map<String, Object> decide(String id, Map<String, Object> body) {
        Map<String,Object> cover = getCover(id);
        String requested = req(body, "status").toUpperCase(Locale.ROOT);
        if (!Set.of("ACTIVE", "SUSPENDED", "CANCELLED").contains(requested)) {
            throw new IllegalArgumentException("Status must be ACTIVE, SUSPENDED or CANCELLED");
        }
        String current = Objects.toString(cover.get("status"), "");
        if (current.startsWith("PENDING_")) {
            throw new IllegalStateException("This funeral cover already has an approval request in progress");
        }
        if (requested.equals(current)) {
            throw new IllegalArgumentException("The funeral cover is already " + requested);
        }
        submitApproval(id, "STATUS_CHANGE", requested, val(body, "requestedBy", "SYSTEM"),
                Objects.toString(body.get("notes"), null), ApprovalType.FUNERAL_COVER_STATUS_CHANGE,
                current);
        return getCover(id);
    }

    @Transactional
    public void completeApproval(String actionId, boolean approved, String actor) {
        List<Map<String,Object>> rows = jdbc.queryForList(
                "SELECT * FROM funeral_cover_approval_action WHERE id=?", actionId);
        if (rows.isEmpty()) throw new IllegalArgumentException("Funeral cover approval action not found: " + actionId);
        Map<String,Object> action = rows.get(0);
        if (!"PENDING_APPROVAL".equalsIgnoreCase(Objects.toString(action.get("status"), ""))) return;
        String coverId = Objects.toString(action.get("cover_id"));
        String actionType = Objects.toString(action.get("action_type"));
        String previous = Objects.toString(action.get("previous_status"), "PENDING_UNDERWRITING");
        String requested = Objects.toString(action.get("requested_status"), "ACTIVE");
        String finalStatus;
        if (approved) {
            finalStatus = requested;
        } else if ("UNDERWRITE".equalsIgnoreCase(actionType)
                && ("DRAFT".equalsIgnoreCase(previous)
                    || "PENDING_UNDERWRITING".equalsIgnoreCase(previous))) {
            finalStatus = "DECLINED";
        } else {
            finalStatus = previous;
        }
        jdbc.update("""
                UPDATE third_party_funeral_cover
                   SET status=?,approval_request_id=NULL,pending_action=NULL,
                       requested_status=NULL,previous_status=NULL,updated_at=CURRENT_TIMESTAMP
                 WHERE id=?
                """, finalStatus, coverId);
        jdbc.update("""
                UPDATE funeral_cover_approval_action
                   SET status=?,completed_by=?,completed_at=CURRENT_TIMESTAMP
                 WHERE id=?
                """, approved ? "APPROVED" : "REJECTED", actor, actionId);
    }

    private void submitApproval(String coverId, String actionType, String requestedStatus,
                                String actor, String notes, ApprovalType approvalType,
                                String previousStatus) {
        Map<String,Object> cover = getCover(coverId);
        String current = StringUtils.hasText(previousStatus)
                ? previousStatus
                : Objects.toString(cover.get("status"), "DRAFT");
        if (Objects.toString(cover.get("pending_action"), "").length() > 0) {
            throw new IllegalStateException("This funeral cover already has an approval request in progress");
        }
        String actionId = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO funeral_cover_approval_action(
                    id,cover_id,action_type,previous_status,requested_status,status,
                    notes,requested_by,created_at
                ) VALUES(?,?,?,?,?,'PENDING_APPROVAL',?,?,CURRENT_TIMESTAMP)
                """, actionId, coverId, actionType, current, requestedStatus, notes, actor);

        String policyNumber = Objects.toString(cover.get("external_policy_no"), coverId);
        String coveredPartyName = firstNonBlank(
                Objects.toString(cover.get("covered_party_name"), null),
                Objects.toString(cover.get("holder_name"), null),
                "Covered member");
        String underwriterName = firstNonBlank(
                Objects.toString(cover.get("underwriter_name"), null),
                Objects.toString(cover.get("underwriter_code"), null),
                "Underwriter");

        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("externalPolicyNumber", policyNumber);
        summary.put("coveredPartyName", coveredPartyName);
        summary.put("coveredPartyNumber", cover.get("covered_partner_number"));
        summary.put("coveredPartyIdentity", cover.get("covered_party_identity"));
        summary.put("membershipNumber", cover.get("membership_no"));
        summary.put("underwriterName", underwriterName);
        summary.put("coverAmountCents", cover.get("cover_amount_cents"));
        summary.put("effectiveFrom", cover.get("effective_from"));
        summary.put("effectiveTo", cover.get("effective_to"));
        summary.put("notes", notes);
        payload.put("coverSummary", summary);
        payload.put("currentValues", Map.of("status", current));
        payload.put("proposedValues", Map.of("status", requestedStatus));
        payload.put("coverId", coverId);
        payload.put("attachmentObjectIds", List.of(coverId));

        ApprovalSubmitRequest request = new ApprovalSubmitRequest();
        request.setApprovalType(approvalType);
        request.setReferenceId(actionId);
        request.setReferenceNo(policyNumber);
        request.setTitle("UNDERWRITE".equals(actionType)
                ? "Funeral cover underwriting - " + coveredPartyName + " - " + policyNumber
                : "Funeral cover status change - " + coveredPartyName + " - " + current + " to " + requestedStatus);
        request.setDescription("Review the cover, member, underwriter, and requested status before approval.");
        request.setRequesterId(actor);
        request.setPayloadJson(toJson(payload));
        var response = approvalService.submitForApproval(request);
        jdbc.update("UPDATE funeral_cover_approval_action SET approval_request_id=? WHERE id=?",
                response.getId(), actionId);
        String pending = "UNDERWRITE".equals(actionType)
                ? "PENDING_UNDERWRITING"
                : "PENDING_" + ("CANCELLED".equals(requestedStatus) ? "CANCELLATION" : requestedStatus);
        jdbc.update("""
                UPDATE third_party_funeral_cover
                   SET status=?,approval_request_id=?,pending_action=?,
                       previous_status=?,requested_status=?,updated_at=CURRENT_TIMESTAMP
                 WHERE id=?
                """, pending, response.getId(), actionType, current, requestedStatus, coverId);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "Not specified";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create the funeral cover approval details", exception);
        }
    }

    public Map<String, Object> getCover(String id) {
        List<Map<String, Object>> rows = covers(null).stream()
                .filter(row -> id.equals(Objects.toString(row.get("id"), null)))
                .toList();
        if (rows.isEmpty()) throw new IllegalArgumentException("Funeral cover not found: " + id);
        Map<String, Object> result = new LinkedHashMap<>(rows.get(0));
        result.put("beneficiaries", jdbc.queryForList(
                "SELECT * FROM third_party_funeral_cover_beneficiary WHERE cover_id=? ORDER BY full_name", id));
        return result;
    }

    private Map<String, Object> verifyEligibleParty(String membershipId, String partnerId,
                                                    String partyType, String dependentId) {
        String q = """
                SELECT m.id AS membershipId, m.membership_no AS membershipNo, p.id AS coveredPartnerId,
                       ? AS coveredPartyType,
                       COALESCE(NULLIF(TRIM(CONCAT_WS(' ',NULLIF(p.name2,''),NULLIF(p.name3,''),NULLIF(p.name1,''))), ''),
                                p.name1, '') AS coveredPartyName,
                       COALESCE((SELECT pi.value FROM partner_identity pi WHERE pi.partner=p.id
                                 ORDER BY CASE WHEN pi.type='SA-ID' THEN 0 WHEN pi.type='PASSPORT' THEN 1 ELSE 2 END LIMIT 1),'') AS identityNumber
                  FROM membership m
                  JOIN partner p ON p.id=?
                 WHERE m.id=?
                """;
        List<Map<String, Object>> rows;
        if ("MEMBER".equals(partyType)) {
            rows = jdbc.queryForList(q + " AND m.member_id=p.id", partyType, partnerId, membershipId);
        } else {
            rows = jdbc.queryForList(q + " AND EXISTS (SELECT 1 FROM membership_dependent md WHERE md.id=? AND md.membership_id=m.id AND md.dependent_partner_id=p.id AND COALESCE(md.active,1)=1)",
                    partyType, partnerId, membershipId, reqValue(dependentId, "membershipDependentId"));
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("The selected member or dependent is not active on the selected membership");
        }
        return rows.get(0);
    }

    @SuppressWarnings("unchecked")
    private void replaceBeneficiaries(String cover, Object raw) {
        jdbc.update("DELETE FROM third_party_funeral_cover_beneficiary WHERE cover_id=?", cover);
        if (!(raw instanceof List<?> list)) return;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) continue;
            Map<String, Object> value = (Map<String, Object>) map;
            jdbc.update("""
                    INSERT INTO third_party_funeral_cover_beneficiary(
                        id,cover_id,full_name,identity_number,relationship,cover_amount_cents
                    ) VALUES(?,?,?,?,?,?)
                    """, UUID.randomUUID().toString(), cover, req(value, "fullName"),
                    req(value, "identityNumber"), value.get("relationship"),
                    num(value, "coverAmountCents", 0));
        }
    }

    private static String id(Map<String, Object> body) {
        String value = Objects.toString(body.get("id"), "").trim();
        return value.isBlank() ? UUID.randomUUID().toString() : value;
    }

    private static String req(Map<String, Object> body, String key) {
        return reqValue(Objects.toString(body.get(key), "").trim(), key);
    }

    private static String reqValue(String value, String key) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException(key + " is required");
        return value.trim();
    }

    private static String val(Map<String, Object> body, String key, String fallback) {
        String value = Objects.toString(body.get(key), "").trim();
        return value.isBlank() ? fallback : value;
    }

    private static String blank(Object value) {
        String text = Objects.toString(value, "").trim();
        return text.isBlank() ? null : text;
    }

    private static long num(Map<String, Object> body, String key, long fallback) {
        Object value = body.get(key);
        return value == null ? fallback : Long.parseLong(value.toString());
    }
}
