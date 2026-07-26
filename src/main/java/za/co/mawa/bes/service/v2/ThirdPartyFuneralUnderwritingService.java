package za.co.mawa.bes.service.v2;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    public ThirdPartyFuneralUnderwritingService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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
                           COALESCE(NULLIF(pv.partner_name,''),
                                    TRIM(CONCAT_WS(' ',NULLIF(p.name2,''),NULLIF(p.name3,''),NULLIF(p.name1,'')))) AS coveredPartyName,
                           COALESCE(NULLIF(pv.partner_no,''),p.number,'') AS partnerNumber,
                           COALESCE((SELECT pi.value FROM partner_identity pi WHERE pi.partner=p.id
                                     ORDER BY CASE WHEN pi.type='SA-ID' THEN 0 WHEN pi.type='PASSPORT' THEN 1 ELSE 2 END LIMIT 1),'') AS identityNumber,
                           m.status AS membershipStatus
                      FROM membership m
                      JOIN partner p ON p.id=m.member_id
                      LEFT JOIN partner_view pv ON pv.partner_id=p.id
                     WHERE UPPER(COALESCE(m.status,'')) NOT IN ('CANCELLED','LAPSED','TERMINATED')
                    UNION ALL
                    SELECT m.id AS membershipId,
                           m.membership_no AS membershipNo,
                           md.dependent_partner_id AS coveredPartnerId,
                           md.id AS membershipDependentId,
                           'DEPENDENT' AS coveredPartyType,
                           COALESCE(NULLIF(pv.partner_name,''),
                                    TRIM(CONCAT_WS(' ',NULLIF(p.name2,''),NULLIF(p.name3,''),NULLIF(p.name1,'')))) AS coveredPartyName,
                           COALESCE(NULLIF(pv.partner_no,''),p.number,'') AS partnerNumber,
                           COALESCE((SELECT pi.value FROM partner_identity pi WHERE pi.partner=p.id
                                     ORDER BY CASE WHEN pi.type='SA-ID' THEN 0 WHEN pi.type='PASSPORT' THEN 1 ELSE 2 END LIMIT 1),'') AS identityNumber,
                           m.status AS membershipStatus
                      FROM membership_dependent md
                      JOIN membership m ON m.id=md.membership_id
                      JOIN partner p ON p.id=md.dependent_partner_id
                      LEFT JOIN partner_view pv ON pv.partner_id=p.id
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
                       COALESCE(NULLIF(pv.partner_name,''),
                                TRIM(CONCAT_WS(' ',NULLIF(p.name2,''),NULLIF(p.name3,''),NULLIF(p.name1,''))),
                                c.holder_name) AS covered_party_name,
                       COALESCE(NULLIF(pv.partner_no,''),p.number,'') AS covered_partner_number,
                       COALESCE((SELECT pi.value FROM partner_identity pi WHERE pi.partner=c.covered_partner_id
                                 ORDER BY CASE WHEN pi.type='SA-ID' THEN 0 WHEN pi.type='PASSPORT' THEN 1 ELSE 2 END LIMIT 1),
                                c.holder_identity,'') AS covered_party_identity,
                       m.membership_no
                  FROM third_party_funeral_cover c
                  JOIN third_party_funeral_underwriter u ON u.id=c.underwriter_id
                  LEFT JOIN partner p ON p.id=c.covered_partner_id
                  LEFT JOIN partner_view pv ON pv.partner_id=p.id
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
                ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE
                    underwriter_id=VALUES(underwriter_id), external_policy_no=VALUES(external_policy_no),
                    membership_id=VALUES(membership_id), holder_name=VALUES(holder_name),
                    holder_identity=VALUES(holder_identity), deceased_name=VALUES(deceased_name),
                    deceased_identity=VALUES(deceased_identity), cover_amount_cents=VALUES(cover_amount_cents),
                    effective_from=VALUES(effective_from), effective_to=VALUES(effective_to),
                    status=VALUES(status), underwriting_notes=VALUES(underwriting_notes),
                    covered_partner_id=VALUES(covered_partner_id), covered_party_type=VALUES(covered_party_type),
                    membership_dependent_id=VALUES(membership_dependent_id)
                """, id, req(body, "underwriterId"), req(body, "externalPolicyNo"), membershipId,
                partyName, identity, body.get("deceasedName"), body.get("deceasedIdentity"), amount,
                req(body, "effectiveFrom"), body.get("effectiveTo"), val(body, "status", "PENDING_UNDERWRITING"),
                body.get("underwritingNotes"), coveredPartnerId, partyType,
                "DEPENDENT".equals(partyType) ? reqValue(dependentId, "membershipDependentId") : null);
        replaceBeneficiaries(id, body.get("beneficiaries"));
        return getCover(id);
    }

    @Transactional
    public Map<String, Object> decide(String id, Map<String, Object> body) {
        String status = req(body, "status").toUpperCase(Locale.ROOT);
        if (!Set.of("APPROVED", "DECLINED", "SUSPENDED", "ACTIVE").contains(status)) {
            throw new IllegalArgumentException("Invalid underwriting status");
        }
        jdbc.update("UPDATE third_party_funeral_cover SET status=?,underwriting_notes=? WHERE id=?",
                status, body.get("notes"), id);
        return getCover(id);
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
                       COALESCE(NULLIF(pv.partner_name,''),
                                TRIM(CONCAT_WS(' ',NULLIF(p.name2,''),NULLIF(p.name3,''),NULLIF(p.name1,'')))) AS coveredPartyName,
                       COALESCE((SELECT pi.value FROM partner_identity pi WHERE pi.partner=p.id
                                 ORDER BY CASE WHEN pi.type='SA-ID' THEN 0 WHEN pi.type='PASSPORT' THEN 1 ELSE 2 END LIMIT 1),'') AS identityNumber
                  FROM membership m
                  JOIN partner p ON p.id=?
                  LEFT JOIN partner_view pv ON pv.partner_id=p.id
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
