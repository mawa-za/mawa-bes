package za.co.mawa.bes.service.v2;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.v2.payapp.PayAppFieldOptionKeyDto;
import za.co.mawa.bes.dto.v2.payapp.PayAppFieldOptionSyncDto;
import za.co.mawa.bes.dto.v2.payapp.PayAppMasterDataChangesResponse;
import za.co.mawa.bes.dto.v2.payapp.PayAppMasterDataSnapshotResponse;
import za.co.mawa.bes.dto.v2.payapp.PayAppMemberSyncDto;
import za.co.mawa.bes.dto.v2.payapp.PayAppPartnerSyncDto;
import za.co.mawa.bes.dto.v2.payapp.PayAppPlanSyncDto;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PayAppMasterDataService {
    private static final int MAX_PAGE_SIZE = 1000;
    private static final String SNAPSHOT_PARTNERS = "P";
    private static final String SNAPSHOT_MEMBERSHIPS = "M";

    private final JdbcTemplate jdbcTemplate;

    public PayAppMasterDataService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PayAppMasterDataSnapshotResponse snapshot(String cursor, int requestedSize) {
        int size = safeSize(requestedSize);
        SnapshotCursor decoded = decodeSnapshotCursor(cursor);
        boolean firstPage = decoded == null;
        long watermark = firstPage ? currentWatermark() : decoded.watermark();
        long totalPartners = firstPage ? countSyncPartners() : decoded.totalPartners();
        long totalMemberships = firstPage ? countMemberships() : decoded.totalMemberships();
        String phase = firstPage ? SNAPSHOT_PARTNERS : decoded.phase();
        String lastId = firstPage ? null : blankToNull(decoded.lastId());

        List<PayAppPartnerSyncDto> partners = new ArrayList<>();
        List<PayAppMemberSyncDto> memberships = new ArrayList<>();
        String nextCursor = null;
        int remaining = size;

        if (SNAPSHOT_PARTNERS.equals(phase)) {
            List<PayAppPartnerSyncDto> rows = queryPartnersAfter(lastId, remaining + 1);
            boolean partnerHasMore = rows.size() > remaining;
            if (partnerHasMore) {
                rows = new ArrayList<>(rows.subList(0, remaining));
            }
            partners.addAll(rows);
            remaining -= rows.size();

            if (partnerHasMore) {
                nextCursor = encodeSnapshotCursor(SNAPSHOT_PARTNERS,
                        rows.get(rows.size() - 1).getPartnerId(), watermark,
                        totalPartners, totalMemberships);
            } else if (remaining == 0) {
                nextCursor = encodeSnapshotCursor(SNAPSHOT_MEMBERSHIPS, "", watermark,
                        totalPartners, totalMemberships);
            } else {
                List<PayAppMemberSyncDto> memberRows = queryMembershipsAfter(null, remaining + 1);
                boolean membershipHasMore = memberRows.size() > remaining;
                if (membershipHasMore) {
                    memberRows = new ArrayList<>(memberRows.subList(0, remaining));
                }
                memberships.addAll(memberRows);
                if (membershipHasMore) {
                    nextCursor = encodeSnapshotCursor(SNAPSHOT_MEMBERSHIPS,
                            memberRows.get(memberRows.size() - 1).getMembershipId(), watermark,
                            totalPartners, totalMemberships);
                }
            }
        } else if (SNAPSHOT_MEMBERSHIPS.equals(phase)) {
            List<PayAppMemberSyncDto> rows = queryMembershipsAfter(lastId, remaining + 1);
            boolean hasMore = rows.size() > remaining;
            if (hasMore) {
                rows = new ArrayList<>(rows.subList(0, remaining));
            }
            memberships.addAll(rows);
            if (hasMore) {
                nextCursor = encodeSnapshotCursor(SNAPSHOT_MEMBERSHIPS,
                        rows.get(rows.size() - 1).getMembershipId(), watermark,
                        totalPartners, totalMemberships);
            }
        } else {
            throw new IllegalArgumentException("Invalid snapshot cursor phase");
        }

        return PayAppMasterDataSnapshotResponse.builder()
                .partners(partners)
                .memberships(memberships)
                .plans(firstPage ? queryAllPlans() : List.of())
                .fieldOptions(firstPage ? queryAllFieldOptions() : List.of())
                .nextCursor(nextCursor)
                .hasMore(nextCursor != null)
                .snapshotWatermark(watermark)
                .totalPartners(totalPartners)
                .totalMemberships(totalMemberships)
                .build();
    }

    public PayAppMasterDataChangesResponse changes(long requestedAfter, String cursor, int requestedSize) {
        int size = safeSize(requestedSize);
        long current = currentWatermark();
        ChangeCursor decoded = decodeChangeCursor(cursor);
        long after = Math.max(0, requestedAfter);
        if (decoded != null && decoded.afterWatermark() != after) {
            throw new IllegalArgumentException("Change cursor does not match the requested watermark");
        }
        long scanAfter = decoded == null ? after : decoded.lastScannedWatermark();
        long until = decoded == null ? current : decoded.untilWatermark();
        long totalPartners = decoded == null ? countSyncPartners() : decoded.totalPartners();
        long totalMemberships = decoded == null ? countMemberships() : decoded.totalMemberships();

        long minimum = minimumWatermark();
        boolean resetRequired = after > current || (after > 0 && minimum > 0 && after + 1 < minimum);
        if (resetRequired) {
            return emptyChanges(after, current, true, countSyncPartners(), countMemberships());
        }

        List<ChangeEvent> fetched = jdbcTemplate.query("""
                SELECT watermark, entity_type, entity_id, entity_sub_id, operation
                  FROM mawa_pay_master_data_change
                 WHERE watermark > ?
                   AND watermark <= ?
                 ORDER BY watermark
                 LIMIT ?
                """, (rs, rowNum) -> new ChangeEvent(
                rs.getLong("watermark"),
                rs.getString("entity_type"),
                rs.getString("entity_id"),
                rs.getString("entity_sub_id"),
                rs.getString("operation")
        ), scanAfter, until, size + 1);

        boolean hasMore = fetched.size() > size;
        List<ChangeEvent> processed = hasMore
                ? new ArrayList<>(fetched.subList(0, size))
                : fetched;
        long lastScanned = processed.isEmpty() ? scanAfter : processed.get(processed.size() - 1).watermark();

        Map<String, ChangeEvent> latest = new LinkedHashMap<>();
        for (ChangeEvent event : processed) {
            latest.put(event.entityType() + "\u001f" + event.entityId() + "\u001f" + safe(event.entitySubId()), event);
        }

        Set<String> partnerUpsertIds = new LinkedHashSet<>();
        Set<String> membershipUpsertIds = new LinkedHashSet<>();
        Set<String> planUpsertIds = new LinkedHashSet<>();
        List<PayAppFieldOptionKeyDto> fieldUpsertKeys = new ArrayList<>();
        Set<String> deletedPartnerIds = new LinkedHashSet<>();
        Set<String> deletedMembershipIds = new LinkedHashSet<>();
        Set<String> deletedPlanIds = new LinkedHashSet<>();
        List<PayAppFieldOptionKeyDto> deletedFieldOptions = new ArrayList<>();

        for (ChangeEvent event : latest.values()) {
            boolean deleted = "DELETE".equalsIgnoreCase(event.operation());
            switch (event.entityType()) {
                case "PARTNER" -> {
                    if (deleted) deletedPartnerIds.add(event.entityId());
                    else partnerUpsertIds.add(event.entityId());
                }
                case "MEMBERSHIP" -> {
                    if (deleted) deletedMembershipIds.add(event.entityId());
                    else membershipUpsertIds.add(event.entityId());
                }
                case "PLAN" -> {
                    if (deleted) deletedPlanIds.add(event.entityId());
                    else planUpsertIds.add(event.entityId());
                }
                case "FIELD_OPTION" -> {
                    PayAppFieldOptionKeyDto key = new PayAppFieldOptionKeyDto(event.entityId(), event.entitySubId());
                    if (deleted) deletedFieldOptions.add(key);
                    else fieldUpsertKeys.add(key);
                }
                default -> {
                    // Forward compatibility: unknown event types are skipped while the watermark advances.
                }
            }
        }

        List<PayAppMemberSyncDto> membershipUpserts = queryMembershipsByIds(membershipUpsertIds);
        Set<String> foundMemberships = new LinkedHashSet<>();
        membershipUpserts.forEach(m -> {
            foundMemberships.add(m.getMembershipId());
            if (m.getPartnerId() != null && !m.getPartnerId().isBlank()) {
                partnerUpsertIds.add(m.getPartnerId());
            }
        });
        membershipUpsertIds.stream().filter(id -> !foundMemberships.contains(id)).forEach(deletedMembershipIds::add);

        // A membership can be assigned to a partner that existed before the
        // device's last sync. Always include referenced partners in the same
        // response window so membership application never depends on event order.
        List<PayAppPartnerSyncDto> partnerUpserts = queryPartnersByIds(partnerUpsertIds);
        Set<String> foundPartners = new LinkedHashSet<>();
        partnerUpserts.forEach(p -> foundPartners.add(p.getPartnerId()));
        partnerUpsertIds.stream().filter(id -> !foundPartners.contains(id)).forEach(deletedPartnerIds::add);

        List<PayAppPlanSyncDto> planUpserts = queryPlansByIds(planUpsertIds);
        Set<String> foundPlans = new LinkedHashSet<>();
        planUpserts.forEach(p -> foundPlans.add(p.getId()));
        planUpsertIds.stream().filter(id -> !foundPlans.contains(id)).forEach(deletedPlanIds::add);

        List<PayAppFieldOptionSyncDto> fieldOptionUpserts = queryFieldOptionsByKeys(fieldUpsertKeys);
        Set<String> foundOptions = new LinkedHashSet<>();
        fieldOptionUpserts.forEach(o -> foundOptions.add(optionKey(o.getField(), o.getCode())));
        fieldUpsertKeys.stream()
                .filter(k -> !foundOptions.contains(optionKey(k.getField(), k.getCode())))
                .forEach(deletedFieldOptions::add);

        String nextCursor = hasMore
                ? encodeChangeCursor(after, lastScanned, until, totalPartners, totalMemberships)
                : null;
        return PayAppMasterDataChangesResponse.builder()
                .partnerUpserts(partnerUpserts)
                .membershipUpserts(membershipUpserts)
                .planUpserts(planUpserts)
                .fieldOptionUpserts(fieldOptionUpserts)
                .deletedPartnerIds(new ArrayList<>(deletedPartnerIds))
                .deletedMembershipIds(new ArrayList<>(deletedMembershipIds))
                .deletedPlanIds(new ArrayList<>(deletedPlanIds))
                .deletedFieldOptions(deletedFieldOptions)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .resetRequired(false)
                .afterWatermark(after)
                .nextWatermark(until)
                .totalPartners(totalPartners)
                .totalMemberships(totalMemberships)
                .build();
    }

    private PayAppMasterDataChangesResponse emptyChanges(long after, long next, boolean resetRequired,
                                                         long totalPartners, long totalMemberships) {
        return PayAppMasterDataChangesResponse.builder()
                .partnerUpserts(List.of())
                .membershipUpserts(List.of())
                .planUpserts(List.of())
                .fieldOptionUpserts(List.of())
                .deletedPartnerIds(List.of())
                .deletedMembershipIds(List.of())
                .deletedPlanIds(List.of())
                .deletedFieldOptions(List.of())
                .hasMore(false)
                .resetRequired(resetRequired)
                .afterWatermark(after)
                .nextWatermark(next)
                .totalPartners(totalPartners)
                .totalMemberships(totalMemberships)
                .build();
    }

    private List<PayAppPartnerSyncDto> queryPartnersAfter(String lastId, int limit) {
        String where = lastId == null
                ? " WHERE " + syncablePartnerPredicate()
                : " WHERE p.id > ? AND " + syncablePartnerPredicate();
        String sql = partnerSelect() + where + " ORDER BY p.id LIMIT ?";
        if (lastId == null) {
            return jdbcTemplate.query(sql, this::mapPartner, limit);
        }
        return jdbcTemplate.query(sql, this::mapPartner, lastId, limit);
    }

    private List<PayAppMemberSyncDto> queryMembershipsAfter(String lastId, int limit) {
        String where = lastId == null ? "" : " WHERE m.id > ? ";
        String sql = membershipSelect() + where + " ORDER BY m.id LIMIT ?";
        if (lastId == null) {
            return jdbcTemplate.query(sql, this::mapMembership, limit);
        }
        return jdbcTemplate.query(sql, this::mapMembership, lastId, limit);
    }

    private List<PayAppPartnerSyncDto> queryPartnersByIds(Set<String> ids) {
        if (ids.isEmpty()) return List.of();
        String sql = partnerSelect() + " WHERE p.id IN (" + placeholders(ids.size())
                + ") AND " + syncablePartnerPredicate() + " ORDER BY p.id";
        return jdbcTemplate.query(sql, this::mapPartner, ids.toArray());
    }

    private List<PayAppMemberSyncDto> queryMembershipsByIds(Set<String> ids) {
        if (ids.isEmpty()) return List.of();
        String sql = membershipSelect() + " WHERE m.id IN (" + placeholders(ids.size()) + ") ORDER BY m.id";
        return jdbcTemplate.query(sql, this::mapMembership, ids.toArray());
    }

    private List<PayAppPlanSyncDto> queryAllPlans() {
        return jdbcTemplate.query("""
                SELECT id, plan_code, name, description, premium_cents, active
                  FROM membership_plan
                 ORDER BY id
                """, (rs, rowNum) -> PayAppPlanSyncDto.builder()
                .id(rs.getString("id"))
                .planCode(rs.getString("plan_code"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .premiumCents(rs.getLong("premium_cents"))
                .active(rs.getBoolean("active"))
                .build());
    }

    private List<PayAppPlanSyncDto> queryPlansByIds(Set<String> ids) {
        if (ids.isEmpty()) return List.of();
        String sql = "SELECT id, plan_code, name, description, premium_cents, active FROM membership_plan WHERE id IN ("
                + placeholders(ids.size()) + ") ORDER BY id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> PayAppPlanSyncDto.builder()
                .id(rs.getString("id"))
                .planCode(rs.getString("plan_code"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .premiumCents(rs.getLong("premium_cents"))
                .active(rs.getBoolean("active"))
                .build(), ids.toArray());
    }

    private List<PayAppFieldOptionSyncDto> queryAllFieldOptions() {
        return jdbcTemplate.query("""
                SELECT field, code, MAX(description) description
                  FROM field_option
                 GROUP BY field, code
                 ORDER BY field, code
                """, (rs, rowNum) -> PayAppFieldOptionSyncDto.builder()
                .field(rs.getString("field"))
                .code(rs.getString("code"))
                .description(rs.getString("description"))
                .build());
    }

    private List<PayAppFieldOptionSyncDto> queryFieldOptionsByKeys(List<PayAppFieldOptionKeyDto> keys) {
        if (keys.isEmpty()) return List.of();
        StringBuilder where = new StringBuilder();
        List<Object> args = new ArrayList<>();
        for (PayAppFieldOptionKeyDto key : keys) {
            if (where.length() > 0) where.append(" OR ");
            where.append("(field = ? AND code = ?)");
            args.add(key.getField());
            args.add(key.getCode());
        }
        String sql = "SELECT field, code, MAX(description) description FROM field_option WHERE " + where
                + " GROUP BY field, code ORDER BY field, code";
        return jdbcTemplate.query(sql, (rs, rowNum) -> PayAppFieldOptionSyncDto.builder()
                .field(rs.getString("field"))
                .code(rs.getString("code"))
                .description(rs.getString("description"))
                .build(), args.toArray());
    }

    private String partnerSelect() {
        return """
                SELECT p.id partner_id,
                       p.number partner_no,
                       p.type partner_type,
                       p.name1,
                       p.name2,
                       p.name3,
                       p.status partner_status,
                       p.birth_date,
                       p.gender,
                       COALESCE(
                           (SELECT UPPER(TRIM(pi.type))
                              FROM partner_identity pi
                             WHERE pi.partner = p.id
                               AND UPPER(TRIM(pi.type)) IN ('SA-ID', 'PASSPORT')
                               AND NULLIF(TRIM(pi.value), '') IS NOT NULL
                             ORDER BY CASE WHEN UPPER(TRIM(pi.type)) = 'SA-ID' THEN 0 ELSE 1 END,
                                      pi.type, pi.value
                             LIMIT 1),
                           'SA-ID'
                       ) identity_type,
                       COALESCE(
                           (SELECT TRIM(pi.value)
                              FROM partner_identity pi
                             WHERE pi.partner = p.id
                               AND UPPER(TRIM(pi.type)) IN ('SA-ID', 'PASSPORT')
                               AND NULLIF(TRIM(pi.value), '') IS NOT NULL
                             ORDER BY CASE WHEN UPPER(TRIM(pi.type)) = 'SA-ID' THEN 0 ELSE 1 END,
                                      pi.type, pi.value
                             LIMIT 1),
                           p.number,
                           p.id
                       ) identity_number,
                       (SELECT pc.value
                          FROM partner_contact pc
                         WHERE pc.partner = p.id
                           AND UPPER(pc.type) IN ('EMAIL', 'E-MAIL')
                         ORDER BY pc.type
                         LIMIT 1) email,
                       (SELECT pc.value
                          FROM partner_contact pc
                         WHERE pc.partner = p.id
                           AND UPPER(pc.type) IN ('MOBILE', 'CELLPHONE', 'CELL', 'PHONE')
                         ORDER BY CASE WHEN UPPER(pc.type) IN ('MOBILE', 'CELLPHONE', 'CELL') THEN 0 ELSE 1 END, pc.type
                         LIMIT 1) mobile_number
                  FROM partner p
                """;
    }

    private String membershipSelect() {
        return """
                SELECT m.id membership_id,
                       m.membership_no,
                       m.member_id partner_id,
                       COALESCE(primary_membership.plan_id, m.plan_id) plan_id,
                       CASE WHEN m.status = 'MERGED' THEN 'ACTIVE' ELSE m.status END membership_status,
                       COALESCE(NULLIF(primary_membership.paid_up_to_period, ''), m.paid_up_to_period) paid_up_to_period,
                       COALESCE(primary_membership.start_date, m.start_date) start_date,
                       COALESCE(primary_membership.join_date, m.join_date) join_date,
                       GREATEST(
                           COALESCE(m.updated_at, m.created_at),
                           COALESCE(primary_membership.updated_at, primary_membership.created_at,
                                    m.updated_at, m.created_at)
                       ) updated_at
                  FROM membership m
                  LEFT JOIN membership primary_membership
                    ON primary_membership.id = m.merged_into_membership_id
                """;
    }

    private PayAppPartnerSyncDto mapPartner(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Date birthDate = rs.getDate("birth_date");
        return PayAppPartnerSyncDto.builder()
                .partnerId(rs.getString("partner_id"))
                .partnerNo(rs.getString("partner_no"))
                .partnerType(rs.getString("partner_type"))
                .firstName(rs.getString("name1"))
                .lastName(rs.getString("name2"))
                .middleName(rs.getString("name3"))
                .identityType(rs.getString("identity_type"))
                .identityNumber(normalizeIdentityNumber(
                        rs.getString("identity_type"), rs.getString("identity_number")))
                .partnerStatus(rs.getString("partner_status"))
                .birthDate(birthDate == null ? null : birthDate.toLocalDate())
                .gender(rs.getString("gender"))
                .email(rs.getString("email"))
                .mobileNumber(rs.getString("mobile_number"))
                .build();
    }

    private PayAppMemberSyncDto mapMembership(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Date startDate = rs.getDate("start_date");
        Date joinDate = rs.getDate("join_date");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        return PayAppMemberSyncDto.builder()
                .membershipId(rs.getString("membership_id"))
                .membershipNo(rs.getString("membership_no"))
                .partnerId(rs.getString("partner_id"))
                .planId(rs.getString("plan_id"))
                .membershipStatus(rs.getString("membership_status"))
                .paidUpToPeriod(rs.getString("paid_up_to_period"))
                .startDate(startDate == null ? null : startDate.toLocalDate())
                .joinDate(joinDate == null ? null : joinDate.toLocalDate())
                .updatedAt(updatedAt == null ? LocalDateTime.now() : updatedAt.toLocalDateTime())
                .build();
    }

    private String normalizeIdentityNumber(String type, String value) {
        if (value == null) return null;
        String normalized = value.trim();
        if ("PASSPORT".equalsIgnoreCase(type)) {
            return normalized.toUpperCase(java.util.Locale.ROOT);
        }
        return normalized;
    }

    private int safeSize(int requested) {
        return Math.min(Math.max(requested, 1), MAX_PAGE_SIZE);
    }

    private long currentWatermark() {
        Long value = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(watermark), 0) FROM mawa_pay_master_data_change", Long.class);
        return value == null ? 0 : value;
    }

    private long minimumWatermark() {
        Long value = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MIN(watermark), 0) FROM mawa_pay_master_data_change", Long.class);
        return value == null ? 0 : value;
    }

    private long countSyncPartners() {
        Long value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM partner p WHERE " + syncablePartnerPredicate(), Long.class);
        return value == null ? 0 : value;
    }

    /**
     * MAWA Pay needs all existing membership partners for referential integrity,
     * plus every individual that can be looked up using a South African ID or
     * passport even when that person does not yet have a membership.
     */
    private String syncablePartnerPredicate() {
        return """
                (
                    EXISTS (
                        SELECT 1
                          FROM membership mx
                         WHERE mx.member_id = p.id
                    )
                    OR EXISTS (
                        SELECT 1
                          FROM partner_identity pix
                         WHERE pix.partner = p.id
                           AND UPPER(TRIM(pix.type)) IN ('SA-ID', 'PASSPORT')
                           AND NULLIF(TRIM(pix.value), '') IS NOT NULL
                    )
                )
                """;
    }

    private long countMemberships() {
        Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM membership", Long.class);
        return value == null ? 0 : value;
    }

    private String placeholders(int size) {
        return String.join(",", java.util.Collections.nCopies(size, "?"));
    }

    private String optionKey(String field, String code) {
        return safe(field) + "\u001f" + safe(code);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String encodeSnapshotCursor(String phase, String lastId, long watermark,
                                        long totalPartners, long totalMemberships) {
        return encode("S|" + phase + "|" + safe(lastId) + "|" + watermark
                + "|" + totalPartners + "|" + totalMemberships);
    }

    private SnapshotCursor decodeSnapshotCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        String[] parts = decode(cursor).split("\\|", -1);
        if (parts.length != 6 || !"S".equals(parts[0])) {
            throw new IllegalArgumentException("Invalid snapshot cursor");
        }
        return new SnapshotCursor(parts[1], parts[2], Long.parseLong(parts[3]),
                Long.parseLong(parts[4]), Long.parseLong(parts[5]));
    }

    private String encodeChangeCursor(long after, long lastScanned, long until,
                                      long totalPartners, long totalMemberships) {
        return encode("C|" + after + "|" + lastScanned + "|" + until
                + "|" + totalPartners + "|" + totalMemberships);
    }

    private ChangeCursor decodeChangeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        String[] parts = decode(cursor).split("\\|", -1);
        if (parts.length != 6 || !"C".equals(parts[0])) {
            throw new IllegalArgumentException("Invalid change cursor");
        }
        return new ChangeCursor(Long.parseLong(parts[1]), Long.parseLong(parts[2]),
                Long.parseLong(parts[3]), Long.parseLong(parts[4]), Long.parseLong(parts[5]));
    }

    private String encode(String raw) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String encoded) {
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    private record SnapshotCursor(String phase, String lastId, long watermark,
                                  long totalPartners, long totalMemberships) {}
    private record ChangeCursor(long afterWatermark, long lastScannedWatermark, long untilWatermark,
                                long totalPartners, long totalMemberships) {}
    private record ChangeEvent(long watermark, String entityType, String entityId,
                               String entitySubId, String operation) {}
}
