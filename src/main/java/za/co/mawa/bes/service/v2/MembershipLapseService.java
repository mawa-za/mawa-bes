package za.co.mawa.bes.service.v2;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.membership.lapse.MembershipLapseConfigurationDto;
import za.co.mawa.bes.dto.v2.membership.lapse.MembershipLapseRunResultDto;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MembershipLapseService {

    static final ZoneId LAPSE_ZONE = ZoneId.of("Africa/Johannesburg");
    static final int DEFAULT_MISSED_PREMIUM_THRESHOLD = 3;
    static final int MAX_MISSED_PREMIUM_THRESHOLD = 24;

    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final Set<String> SETTLED_STATUSES = Set.of(
            "PAID", "CANCELLED", "WRITTEN_OFF", "REVERSED"
    );

    static final String LEGACY_TRANSACTION_LAPSE_SQL = """
            UPDATE `transaction`
               SET status = 'INACTIVE',
                   status_reason = 'LAPSED',
                   changed_by = ?
             WHERE BINARY id = BINARY ?
            """;

    private final JdbcTemplate jdbc;

    public MembershipLapseService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public MembershipLapseConfigurationDto configuration() {
        ensureDefaultConfiguration();
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT id,
                       enabled,
                       missed_premiums_before_lapse,
                       last_run_at,
                       last_lapsed_count,
                       updated_at,
                       updated_by
                  FROM membership_lapse_configuration
                 WHERE id = 'DEFAULT'
                """);

        return MembershipLapseConfigurationDto.builder()
                .id(stringValue(row.get("id")))
                .enabled(databaseBoolean(row.get("enabled")))
                .missedPremiumsBeforeLapse(parseThreshold(row.get("missed_premiums_before_lapse")))
                .lastRunAt(toIsoDateTime(row.get("last_run_at")))
                .lastLapsedCount(number(row.get("last_lapsed_count")))
                .updatedAt(toIsoDateTime(row.get("updated_at")))
                .updatedBy(stringValue(row.get("updated_by")))
                .build();
    }

    @Transactional
    public MembershipLapseConfigurationDto saveConfiguration(
            MembershipLapseConfigurationDto request,
            String user
    ) {
        int threshold = parseThreshold(
                request == null ? null : request.getMissedPremiumsBeforeLapse()
        );
        boolean enabled = request == null || request.isEnabled();

        ensureDefaultConfiguration();
        jdbc.update("""
                UPDATE membership_lapse_configuration
                   SET enabled = ?,
                       missed_premiums_before_lapse = ?,
                       updated_at = CURRENT_TIMESTAMP,
                       updated_by = ?
                 WHERE id = 'DEFAULT'
                """, enabled, threshold, actor(user));
        return configuration();
    }

    /**
     * Scheduler entry point. Disabled tenant policies are skipped without
     * changing membership data.
     */
    @Transactional
    public MembershipLapseRunResultDto runConfiguredAutomaticLapse(String user) {
        ensureDefaultConfiguration();
        lockConfiguration();
        MembershipLapseConfigurationDto config = configuration();
        if (!config.isEnabled()) {
            return MembershipLapseRunResultDto.builder()
                    .skipped(true)
                    .reason("Automatic membership lapse is disabled")
                    .threshold(config.getMissedPremiumsBeforeLapse())
                    .runDate(today().toString())
                    .build();
        }
        return evaluateAndLapse(null, config.getMissedPremiumsBeforeLapse(), actor(user), true);
    }

    /**
     * Explicit user action. It uses the configured threshold even when the
     * automatic schedule is disabled.
     */
    @Transactional
    public MembershipLapseRunResultDto runNow(String user) {
        ensureDefaultConfiguration();
        lockConfiguration();
        MembershipLapseConfigurationDto config = configuration();
        return evaluateAndLapse(null, config.getMissedPremiumsBeforeLapse(), actor(user), true);
    }

    /**
     * Compatibility entry point for the legacy single-membership lapse APIs.
     */
    @Transactional
    public MembershipLapseRunResultDto runForMembership(String membershipId, String user) {
        if (membershipId == null || membershipId.isBlank()) {
            throw new IllegalArgumentException("Membership id is required");
        }
        MembershipLapseConfigurationDto config = configuration();
        return evaluateAndLapse(
                membershipId.trim(),
                config.getMissedPremiumsBeforeLapse(),
                actor(user),
                false
        );
    }

    private MembershipLapseRunResultDto evaluateAndLapse(
            String membershipId,
            int threshold,
            String user,
            boolean markConfigurationRun
    ) {
        LocalDate runDate = today();
        List<MembershipCandidate> memberships = loadActiveMemberships(membershipId, runDate);
        Map<String, List<PremiumPeriod>> premiumsByMembership = loadOverduePremiums(runDate);

        int withOverduePremiums = 0;
        List<String> lapsedIds = new ArrayList<>();

        for (MembershipCandidate membership : memberships) {
            List<PremiumPeriod> periods = mergePremiumPeriods(membership, premiumsByMembership);
            if (periods.stream().anyMatch(PremiumPeriod::outstanding)) {
                withOverduePremiums++;
            }

            int missedPremiums = countConsecutiveMissedPremiums(periods);
            if (missedPremiums < threshold) {
                continue;
            }

            int updated = jdbc.update("""
                    UPDATE membership
                       SET status = 'LAPSED',
                           updated_at = CURRENT_TIMESTAMP,
                           updated_by = ?
                     WHERE BINARY id = BINARY ?
                       AND UPPER(TRIM(COALESCE(status, ''))) = 'ACTIVE'
                    """, user, membership.id());
            if (updated == 0) {
                continue;
            }

            updateLegacyMembershipTransaction(membership.id(), user);
            updateLegacyMembershipTransaction(membership.oldId(), user);
            insertLapseAudit(membership.id(), missedPremiums, threshold, periods, user);
            lapsedIds.add(membership.id());
        }

        if (markConfigurationRun) {
            jdbc.update("""
                    UPDATE membership_lapse_configuration
                       SET last_run_at = CURRENT_TIMESTAMP,
                           last_lapsed_count = ?,
                           updated_at = CURRENT_TIMESTAMP,
                           updated_by = ?
                     WHERE id = 'DEFAULT'
                    """, lapsedIds.size(), user);
        }

        return MembershipLapseRunResultDto.builder()
                .skipped(false)
                .threshold(threshold)
                .evaluatedMemberships(memberships.size())
                .membershipsWithOverduePremiums(withOverduePremiums)
                .lapsedMemberships(lapsedIds.size())
                .runDate(runDate.toString())
                .lapsedMembershipIds(List.copyOf(lapsedIds))
                .build();
    }

    private List<MembershipCandidate> loadActiveMemberships(String membershipId, LocalDate runDate) {
        String sql = """
                SELECT id, old_id
                  FROM membership
                 WHERE UPPER(TRIM(COALESCE(status, ''))) = 'ACTIVE'
                   AND (end_date IS NULL OR end_date >= ?)
                """;
        List<Object> args = new ArrayList<>();
        args.add(Date.valueOf(runDate));
        if (membershipId != null) {
            sql += " AND (BINARY id = BINARY ? OR BINARY old_id = BINARY ?)";
            args.add(membershipId);
            args.add(membershipId);
        }

        return jdbc.query(sql, (rs, rowNum) -> new MembershipCandidate(
                cleanId(rs.getString("id")),
                cleanId(rs.getString("old_id"))
        ), args.toArray());
    }

    private Map<String, List<PremiumPeriod>> loadOverduePremiums(LocalDate runDate) {
        Map<String, List<PremiumPeriod>> result = new HashMap<>();
        jdbc.query("""
                SELECT membership_id,
                       period_yyyymm,
                       status,
                       balance_cents,
                       due_date
                  FROM membership_premium
                 WHERE due_date IS NOT NULL
                   AND due_date < ?
                   AND period_yyyymm REGEXP '^[0-9]{6}$'
                   AND CAST(SUBSTRING(period_yyyymm, 5, 2) AS UNSIGNED) BETWEEN 1 AND 12
                 ORDER BY membership_id, period_yyyymm DESC
                """, rs -> {
            String id = cleanId(rs.getString("membership_id"));
            if (id == null) {
                return;
            }
            YearMonth period = parsePeriod(rs.getString("period_yyyymm"));
            if (period == null) {
                return;
            }
            long balance = rs.getObject("balance_cents") instanceof Number number
                    ? number.longValue()
                    : 0L;
            String status = normalizeStatus(rs.getString("status"));
            boolean settled = balance <= 0 || SETTLED_STATUSES.contains(status);
            boolean outstanding = !settled && balance > 0;
            result.computeIfAbsent(id, ignored -> new ArrayList<>())
                    .add(new PremiumPeriod(period, outstanding, settled));
        }, Date.valueOf(runDate));
        return result;
    }

    private List<PremiumPeriod> mergePremiumPeriods(
            MembershipCandidate membership,
            Map<String, List<PremiumPeriod>> premiumsByMembership
    ) {
        Map<YearMonth, PremiumPeriod> byPeriod = new LinkedHashMap<>();
        mergePeriods(byPeriod, premiumsByMembership.get(membership.id()));
        if (membership.oldId() != null) {
            mergePeriods(byPeriod, premiumsByMembership.get(membership.oldId()));
        }
        return byPeriod.values().stream()
                .sorted(Comparator.comparing(PremiumPeriod::period).reversed())
                .toList();
    }

    private void mergePeriods(Map<YearMonth, PremiumPeriod> target, List<PremiumPeriod> source) {
        if (source == null) {
            return;
        }
        for (PremiumPeriod candidate : source) {
            target.merge(candidate.period(), candidate, (existing, incoming) -> {
                boolean settled = existing.settled() || incoming.settled();
                boolean outstanding = !settled
                        && (existing.outstanding() || incoming.outstanding());
                return new PremiumPeriod(existing.period(), outstanding, settled);
            });
        }
    }

    static int countConsecutiveMissedPremiums(List<PremiumPeriod> periods) {
        if (periods == null || periods.isEmpty()) {
            return 0;
        }
        List<PremiumPeriod> ordered = periods.stream()
                .sorted(Comparator.comparing(PremiumPeriod::period).reversed())
                .toList();

        int missed = 0;
        YearMonth previous = null;
        for (PremiumPeriod period : ordered) {
            if (previous != null && !period.period().equals(previous.minusMonths(1))) {
                break;
            }
            if (period.settled() || !period.outstanding()) {
                break;
            }
            missed++;
            previous = period.period();
        }
        return missed;
    }

    private void updateLegacyMembershipTransaction(String transactionId, String user) {
        if (transactionId == null) {
            return;
        }
        jdbc.update(LEGACY_TRANSACTION_LAPSE_SQL, user, transactionId);
    }

    private void insertLapseAudit(
            String membershipId,
            int missedPremiums,
            int threshold,
            List<PremiumPeriod> periods,
            String user
    ) {
        String latestMissedPeriod = periods.isEmpty()
                ? null
                : periods.get(0).period().format(PERIOD_FORMAT);
        String details = "Automatically lapsed after " + missedPremiums
                + " consecutive missed premium(s); configured threshold=" + threshold
                + (latestMissedPeriod == null ? "" : "; latest missed period=" + latestMissedPeriod);

        jdbc.update("""
                INSERT INTO membership_change_audit (
                    id,
                    membership_id,
                    change_request_id,
                    event_type,
                    old_values_json,
                    new_values_json,
                    details,
                    performed_by,
                    performed_at
                ) VALUES (
                    ?, ?, NULL, 'MEMBERSHIP_LAPSED',
                    JSON_OBJECT('status', 'ACTIVE'),
                    JSON_OBJECT(
                        'status', 'LAPSED',
                        'missedPremiums', ?,
                        'configuredThreshold', ?,
                        'latestMissedPeriod', ?
                    ),
                    ?, ?, CURRENT_TIMESTAMP
                )
                """,
                UUID.randomUUID().toString().replace("-", ""),
                membershipId,
                missedPremiums,
                threshold,
                latestMissedPeriod,
                details,
                user
        );
    }

    private void ensureDefaultConfiguration() {
        jdbc.update("""
                INSERT INTO membership_lapse_configuration (
                    id,
                    enabled,
                    missed_premiums_before_lapse,
                    last_lapsed_count,
                    updated_at,
                    updated_by
                )
                SELECT 'DEFAULT', ?, ?, 0, CURRENT_TIMESTAMP, 'SYSTEM'
                WHERE NOT EXISTS (
                    SELECT 1
                      FROM membership_lapse_configuration
                     WHERE id = 'DEFAULT'
                )
                """, true, DEFAULT_MISSED_PREMIUM_THRESHOLD);
    }

    private void lockConfiguration() {
        jdbc.queryForObject("""
                SELECT id
                  FROM membership_lapse_configuration
                 WHERE id = 'DEFAULT'
                 FOR UPDATE
                """, String.class);
    }

    private int parseThreshold(Object value) {
        int threshold;
        try {
            threshold = value instanceof Number number
                    ? number.intValue()
                    : Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignored) {
            threshold = DEFAULT_MISSED_PREMIUM_THRESHOLD;
        }
        if (threshold < 1 || threshold > MAX_MISSED_PREMIUM_THRESHOLD) {
            throw new IllegalArgumentException(
                    "Missed premiums before lapse must be between 1 and "
                            + MAX_MISSED_PREMIUM_THRESHOLD
            );
        }
        return threshold;
    }

    private LocalDate today() {
        return LocalDate.now(LAPSE_ZONE);
    }

    private YearMonth parsePeriod(String value) {
        try {
            return YearMonth.parse(value, PERIOD_FORMAT);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalizeStatus(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String actor(String user) {
        return user == null || user.isBlank() ? "SYSTEM" : user.trim();
    }

    private boolean databaseBoolean(Object value) {
        if (value instanceof byte[] bytes) {
            return bytes.length > 0 && bytes[0] != 0;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = value == null ? "" : value.toString().trim();
        return "true".equalsIgnoreCase(text)
                || "1".equals(text)
                || "y".equalsIgnoreCase(text)
                || "yes".equalsIgnoreCase(text);
    }

    private int number(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private String toIsoDateTime(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toString();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toString();
        }
        return value == null ? null : value.toString();
    }

    private String cleanId(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    record MembershipCandidate(String id, String oldId) {
    }

    record PremiumPeriod(YearMonth period, boolean outstanding, boolean settled) {
    }
}
