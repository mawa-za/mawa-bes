package za.co.mawa.bes.service.v2;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class PremiumGenerationService {

    static final String DAY_OF_MONTH = "DAY_OF_MONTH";
    static final String LEGACY_FIRST_DAY_OF_MONTH = "FIRST_DAY_OF_MONTH";
    static final String MONTH_AFTER_LAST_PAYMENT = "MONTH_AFTER_LAST_PAYMENT";
    static final ZoneId PREMIUM_ZONE = ZoneId.of("Africa/Johannesburg");

    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final Set<String> SUPPORTED_MODES = Set.of(DAY_OF_MONTH, MONTH_AFTER_LAST_PAYMENT);

    private static final int INSERT_BATCH_SIZE = 500;

    private final JdbcTemplate jdbc;
    private final MembershipChangeService membershipChanges;

    public PremiumGenerationService(
            JdbcTemplate jdbc,
            MembershipChangeService membershipChanges
    ) {
        this.jdbc = jdbc;
        this.membershipChanges = membershipChanges;
    }

    public Map<String, Object> configuration() {
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT id,
                       generation_mode,
                       generation_day_of_month,
                       enabled,
                       last_run_at,
                       last_generated_period,
                       updated_at,
                       updated_by
                  FROM premium_generation_configuration
                 WHERE id = 'DEFAULT'
                """);

        String mode = normalizeMode(Objects.toString(row.get("generation_mode"), DAY_OF_MONTH));
        int day = parseGenerationDay(row.get("generation_day_of_month"));
        boolean enabled = databaseBoolean(row.get("enabled"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", row.get("id"));
        response.put("generationMode", mode);
        response.put("generation_mode", mode);
        response.put("generationDayOfMonth", day);
        response.put("generation_day_of_month", day);
        response.put("enabled", enabled);
        response.put("lastRunAt", toIsoDateTime(row.get("last_run_at")));
        response.put("last_run_at", toIsoDateTime(row.get("last_run_at")));
        response.put("lastGeneratedPeriod", row.get("last_generated_period"));
        response.put("last_generated_period", row.get("last_generated_period"));
        response.put("updatedAt", toIsoDateTime(row.get("updated_at")));
        response.put("updatedBy", row.get("updated_by"));
        return response;
    }

    @Transactional
    public Map<String, Object> saveConfiguration(Map<String, Object> request, String user) {
        String requestedMode = Objects.toString(
                request == null ? null : request.get("generationMode"),
                DAY_OF_MONTH
        );
        if (request != null && request.get("generation_mode") != null) {
            requestedMode = Objects.toString(request.get("generation_mode"), requestedMode);
        }

        String mode = normalizeMode(requestedMode);
        if (!SUPPORTED_MODES.contains(mode)) {
            throw new IllegalArgumentException("Unsupported generation mode: " + requestedMode);
        }

        Object enabledValue = request == null ? null : request.get("enabled");
        boolean enabled = enabledValue == null || parseBoolean(enabledValue);

        Object dayValue = request == null ? null : request.get("generationDayOfMonth");
        if (dayValue == null && request != null) {
            dayValue = request.get("generation_day_of_month");
        }
        int generationDay = parseGenerationDay(dayValue);

        int updated = jdbc.update("""
                UPDATE premium_generation_configuration
                   SET generation_mode = ?,
                       generation_day_of_month = ?,
                       enabled = ?,
                       updated_at = CURRENT_TIMESTAMP,
                       updated_by = ?
                 WHERE id = 'DEFAULT'
                """, mode, generationDay, enabled, actor(user));

        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO premium_generation_configuration (
                        id,
                        generation_mode,
                        generation_day_of_month,
                        enabled,
                        updated_at,
                        updated_by
                    ) VALUES ('DEFAULT', ?, ?, ?, CURRENT_TIMESTAMP, ?)
                    """, mode, generationDay, enabled, actor(user));
        }

        return configuration();
    }

    @Transactional
    public Map<String, Object> backfillSixPeriods(String user) {
        YearMonth toPeriod = YearMonth.from(today());
        YearMonth fromPeriod = sixPeriodsEnding(toPeriod).get(0);
        GenerationResult result = generateRange(fromPeriod, toPeriod, actor(user));

        Map<String, Object> response = result.toMap();
        response.put("action", "BACKFILL_SIX_PERIODS");
        response.put("message", result.created() + " missing premium(s) generated for "
                + fromPeriod.format(PERIOD_FORMAT) + " to " + toPeriod.format(PERIOD_FORMAT));
        return response;
    }

    /**
     * Called by the central, tenant-aware scheduler in mawa-admin-bes.
     * The method checks this tenant's own premium-generation configuration before doing work.
     */
    @Transactional
    public Map<String, Object> runConfiguredAutomaticGeneration(String user) {
        lockConfiguration();
        Map<String, Object> config = configuration();
        if (!Boolean.TRUE.equals(config.get("enabled"))) {
            return skipped("Automatic premium generation is disabled", config);
        }

        LocalDate currentDate = today();
        String mode = normalizeMode(Objects.toString(config.get("generationMode"), DAY_OF_MONTH));
        int generationDay = parseGenerationDay(config.get("generationDayOfMonth"));

        if (MONTH_AFTER_LAST_PAYMENT.equals(mode)) {
            LocalDateTime lastRun = parseDateTime(config.get("lastRunAt"));
            if (lastRun != null && lastRun.toLocalDate().equals(currentDate)) {
                return skipped("Month-after-last-payment generation already ran today", config);
            }

            Map<String, Object> response = generateMonthAfterLastPayment(actor(user));
            markAutomaticRun(null);
            response.put("automatic", true);
            return response;
        }

        YearMonth currentPeriod = YearMonth.from(currentDate);
        int effectiveDay = effectiveGenerationDay(currentPeriod, generationDay);
        String currentPeriodValue = currentPeriod.format(PERIOD_FORMAT);
        String lastGeneratedPeriod = Objects.toString(config.get("lastGeneratedPeriod"), "");

        if (currentDate.getDayOfMonth() < effectiveDay) {
            return skipped("Configured generation day has not been reached", config);
        }
        if (currentPeriodValue.equals(lastGeneratedPeriod)) {
            return skipped("Premiums have already been generated for " + currentPeriodValue, config);
        }

        GenerationResult result = generateRange(currentPeriod, currentPeriod, actor(user));
        markAutomaticRun(currentPeriodValue);

        Map<String, Object> response = result.toMap();
        response.put("automatic", true);
        response.put("mode", DAY_OF_MONTH);
        response.put("generationDayOfMonth", generationDay);
        response.put("effectiveGenerationDay", effectiveDay);
        return response;
    }

    @Transactional
    public Map<String, Object> generateMonthAfterLastPayment(String user) {
        LocalDate currentDate = today();
        membershipChanges.applyDuePlanChanges(currentDate, actor(user));

        Map<String, Long> planAmounts = loadPlanAmounts();
        List<MembershipCandidate> memberships = loadActiveMembershipCandidates();
        Map<String, MembershipCandidate> membershipById = new HashMap<>();
        for (MembershipCandidate membership : memberships) {
            membershipById.put(membership.id(), membership);
        }
        Map<YearMonth, List<PremiumInsertCandidate>> insertsByPeriod = new LinkedHashMap<>();
        YearMonth currentPeriod = YearMonth.from(currentDate);

        for (MembershipCandidate membership : memberships) {
            YearMonth targetPeriod = nextPremiumPeriod(membership);
            if (targetPeriod == null || targetPeriod.isAfter(currentPeriod)) {
                continue;
            }
            if (membership.endDate() != null
                    && targetPeriod.isAfter(YearMonth.from(membership.endDate()))) {
                continue;
            }

            Long amount = membership.premiumCents() != null
                    ? membership.premiumCents()
                    : planAmounts.get(membership.planId());
            if (amount == null) {
                continue;
            }

            insertsByPeriod.computeIfAbsent(targetPeriod, ignored -> new ArrayList<>())
                    .add(new PremiumInsertCandidate(
                            membership.id(),
                            targetPeriod.format(PERIOD_FORMAT),
                            amount,
                            currentDate,
                            actor(user)
                    ));
        }

        int created = 0;
        for (Map.Entry<YearMonth, List<PremiumInsertCandidate>> entry : insertsByPeriod.entrySet()) {
            String periodValue = entry.getKey().format(PERIOD_FORMAT);
            Set<String> existingMembershipIds = loadExistingMembershipIds(periodValue);
            List<PremiumInsertCandidate> missing = new ArrayList<>();

            for (PremiumInsertCandidate candidate : entry.getValue()) {
                MembershipCandidate membership = membershipById.get(candidate.membershipId());
                if (membership == null || premiumExists(existingMembershipIds, membership)) {
                    continue;
                }
                missing.add(candidate);
                existingMembershipIds.add(candidate.membershipId());
            }
            created += insertPremiums(missing);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("created", created);
        response.put("mode", MONTH_AFTER_LAST_PAYMENT);
        response.put("runDate", currentDate.toString());
        response.put("message", created + " premium(s) generated");
        return response;
    }

    GenerationResult generateRange(YearMonth fromPeriod, YearMonth toPeriod, String user) {
        if (fromPeriod == null || toPeriod == null || fromPeriod.isAfter(toPeriod)) {
            throw new IllegalArgumentException("A valid premium period range is required");
        }

        LocalDate currentDate = today();
        membershipChanges.applyDuePlanChanges(currentDate, actor(user));

        Map<String, Object> config = configuration();
        int generationDay = parseGenerationDay(config.get("generationDayOfMonth"));
        LocalDate rangeStart = fromPeriod.atDay(1);
        LocalDate rangeEnd = toPeriod.atEndOfMonth();

        // Load each domain table independently. Some restored tenant schemas use
        // different collations for legacy IDs; Java-side ID matching avoids
        // cross-collation joins and the previous correlated SQL timeouts.
        List<MembershipCandidate> memberships = loadMembershipCandidates(rangeStart, rangeEnd);
        Map<String, Long> planAmounts = loadPlanAmounts();
        Map<String, List<PlanHistoryCandidate>> planHistory = loadPlanHistory(rangeStart, rangeEnd);

        int created = 0;
        int eligible = 0;
        int missingAmount = 0;
        List<String> periods = new ArrayList<>();

        YearMonth period = fromPeriod;
        while (!period.isAfter(toPeriod)) {
            periods.add(period.format(PERIOD_FORMAT));
            PeriodGenerationResult periodResult = generatePeriod(
                    period,
                    generationDay,
                    actor(user),
                    memberships,
                    planAmounts,
                    planHistory
            );
            created += periodResult.created();
            eligible += periodResult.eligible();
            missingAmount += periodResult.missingAmount();
            period = period.plusMonths(1);
        }

        int alreadyPresent = Math.max(eligible - missingAmount - created, 0);
        return new GenerationResult(
                created,
                eligible,
                alreadyPresent,
                missingAmount,
                fromPeriod,
                toPeriod,
                periods
        );
    }

    private PeriodGenerationResult generatePeriod(
            YearMonth period,
            int configuredDay,
            String user,
            List<MembershipCandidate> memberships,
            Map<String, Long> planAmounts,
            Map<String, List<PlanHistoryCandidate>> planHistory
    ) {
        LocalDate periodStart = period.atDay(1);
        LocalDate periodEnd = period.atEndOfMonth();
        LocalDate dueDate = period.atDay(effectiveGenerationDay(period, configuredDay));
        String periodValue = period.format(PERIOD_FORMAT);
        Set<String> existingMembershipIds = loadExistingMembershipIds(periodValue);

        int eligible = 0;
        int missingAmount = 0;
        List<PremiumInsertCandidate> inserts = new ArrayList<>();

        for (MembershipCandidate membership : memberships) {
            if (membership.startDate().isAfter(periodEnd)
                    || (membership.endDate() != null && membership.endDate().isBefore(periodStart))) {
                continue;
            }
            eligible++;

            Long amount = resolvePeriodAmount(
                    membership,
                    planHistory.get(membership.id()),
                    planAmounts,
                    periodStart,
                    periodEnd
            );
            if (amount == null) {
                missingAmount++;
                continue;
            }
            if (premiumExists(existingMembershipIds, membership)) {
                continue;
            }

            inserts.add(new PremiumInsertCandidate(
                    membership.id(),
                    periodValue,
                    amount,
                    dueDate,
                    user
            ));
            existingMembershipIds.add(membership.id());
        }

        int created = insertPremiums(inserts);
        return new PeriodGenerationResult(created, eligible, missingAmount);
    }

    private List<MembershipCandidate> loadMembershipCandidates(LocalDate rangeStart, LocalDate rangeEnd) {
        return jdbc.query("""
                SELECT id,
                       old_id,
                       plan_id,
                       premium_cents,
                       COALESCE(start_date, join_date) AS effective_start_date,
                       end_date,
                       paid_up_to_period
                  FROM membership
                 WHERE UPPER(TRIM(COALESCE(status, ''))) = 'ACTIVE'
                   AND COALESCE(start_date, join_date) <= ?
                   AND (end_date IS NULL OR end_date >= ?)
                """, (rs, rowNum) -> new MembershipCandidate(
                cleanId(rs.getString("id")),
                cleanId(rs.getString("old_id")),
                cleanId(rs.getString("plan_id")),
                nullableLong(rs.getObject("premium_cents")),
                rs.getDate("effective_start_date").toLocalDate(),
                rs.getDate("end_date") == null ? null : rs.getDate("end_date").toLocalDate(),
                rs.getString("paid_up_to_period")
        ), Date.valueOf(rangeEnd), Date.valueOf(rangeStart));
    }

    private List<MembershipCandidate> loadActiveMembershipCandidates() {
        return jdbc.query("""
                SELECT id,
                       old_id,
                       plan_id,
                       premium_cents,
                       COALESCE(start_date, join_date) AS effective_start_date,
                       end_date,
                       paid_up_to_period
                  FROM membership
                 WHERE UPPER(TRIM(COALESCE(status, ''))) = 'ACTIVE'
                   AND COALESCE(start_date, join_date) IS NOT NULL
                """, (rs, rowNum) -> new MembershipCandidate(
                cleanId(rs.getString("id")),
                cleanId(rs.getString("old_id")),
                cleanId(rs.getString("plan_id")),
                nullableLong(rs.getObject("premium_cents")),
                rs.getDate("effective_start_date").toLocalDate(),
                rs.getDate("end_date") == null ? null : rs.getDate("end_date").toLocalDate(),
                rs.getString("paid_up_to_period")
        ));
    }

    private Map<String, Long> loadPlanAmounts() {
        Map<String, Long> result = new HashMap<>();
        jdbc.query("""
                SELECT id, premium_cents
                  FROM membership_plan
                 WHERE premium_cents IS NOT NULL
                """, rs -> {
            String id = cleanId(rs.getString("id"));
            Long amount = nullableLong(rs.getObject("premium_cents"));
            if (id != null && amount != null) {
                result.put(id, amount);
            }
        });
        return result;
    }

    private Map<String, List<PlanHistoryCandidate>> loadPlanHistory(
            LocalDate rangeStart,
            LocalDate rangeEnd
    ) {
        Map<String, List<PlanHistoryCandidate>> result = new HashMap<>();
        jdbc.query("""
                SELECT membership_id,
                       plan_id,
                       effective_from,
                       effective_to
                  FROM membership_premium_plan_history
                 WHERE effective_from <= ?
                   AND (effective_to IS NULL OR effective_to >= ?)
                 ORDER BY membership_id, effective_from DESC, id DESC
                """, rs -> {
            String membershipId = cleanId(rs.getString("membership_id"));
            if (membershipId == null) {
                return;
            }
            result.computeIfAbsent(membershipId, ignored -> new ArrayList<>())
                    .add(new PlanHistoryCandidate(
                            membershipId,
                            cleanId(rs.getString("plan_id")),
                            rs.getDate("effective_from").toLocalDate(),
                            rs.getDate("effective_to") == null
                                    ? null
                                    : rs.getDate("effective_to").toLocalDate()
                    ));
        }, Date.valueOf(rangeEnd), Date.valueOf(rangeStart));
        return result;
    }

    private Set<String> loadExistingMembershipIds(String periodValue) {
        Set<String> result = new HashSet<>();
        jdbc.query("""
                SELECT membership_id
                  FROM membership_premium
                 WHERE period_yyyymm = ?
                """, rs -> {
            String membershipId = cleanId(rs.getString("membership_id"));
            if (membershipId != null) {
                result.add(membershipId);
            }
        }, periodValue);
        return result;
    }

    private Long resolvePeriodAmount(
            MembershipCandidate membership,
            List<PlanHistoryCandidate> histories,
            Map<String, Long> planAmounts,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        if (histories != null) {
            for (PlanHistoryCandidate history : histories) {
                boolean overlaps = !history.effectiveFrom().isAfter(periodEnd)
                        && (history.effectiveTo() == null
                        || !history.effectiveTo().isBefore(periodStart));
                if (overlaps) {
                    Long historicalAmount = planAmounts.get(history.planId());
                    if (historicalAmount != null) {
                        return historicalAmount;
                    }
                    break;
                }
            }
        }
        if (membership.premiumCents() != null) {
            return membership.premiumCents();
        }
        return planAmounts.get(membership.planId());
    }

    private int insertPremiums(List<PremiumInsertCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return 0;
        }

        final String sql = """
                INSERT IGNORE INTO membership_premium (
                    id,
                    membership_id,
                    period_yyyymm,
                    amount_cents,
                    paid_amount_cents,
                    balance_cents,
                    status,
                    due_date,
                    created_at,
                    created_by
                ) VALUES (?, ?, ?, ?, 0, ?, 'UNPAID', ?, CURRENT_TIMESTAMP, ?)
                """;

        int created = 0;
        for (int from = 0; from < candidates.size(); from += INSERT_BATCH_SIZE) {
            List<PremiumInsertCandidate> batch = candidates.subList(
                    from,
                    Math.min(from + INSERT_BATCH_SIZE, candidates.size())
            );
            int[] counts = jdbc.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int index) throws SQLException {
                    PremiumInsertCandidate candidate = batch.get(index);
                    ps.setString(1, UUID.randomUUID().toString().replace("-", ""));
                    ps.setString(2, candidate.membershipId());
                    ps.setString(3, candidate.periodValue());
                    ps.setLong(4, candidate.amountCents());
                    ps.setLong(5, candidate.amountCents());
                    ps.setDate(6, Date.valueOf(candidate.dueDate()));
                    ps.setString(7, candidate.createdBy());
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });
            for (int count : counts) {
                if (count > 0 || count == Statement.SUCCESS_NO_INFO) {
                    created++;
                }
            }
        }
        return created;
    }

    private boolean premiumExists(Set<String> existingMembershipIds, MembershipCandidate membership) {
        return existingMembershipIds.contains(membership.id())
                || (membership.oldId() != null && existingMembershipIds.contains(membership.oldId()));
    }

    private YearMonth nextPremiumPeriod(MembershipCandidate membership) {
        String paidUpTo = membership.paidUpToPeriod();
        if (paidUpTo != null && paidUpTo.matches("^[0-9]{6}$")) {
            try {
                int year = Integer.parseInt(paidUpTo.substring(0, 4));
                int month = Integer.parseInt(paidUpTo.substring(4, 6));
                return YearMonth.of(year, month).plusMonths(1);
            } catch (RuntimeException ignored) {
                // Fall back to membership start period below.
            }
        }
        return membership.startDate() == null ? null : YearMonth.from(membership.startDate());
    }

    private Long nullableLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private String cleanId(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private Map<String, Object> skipped(String reason, Map<String, Object> config) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("created", 0);
        response.put("skipped", true);
        response.put("reason", reason);
        response.put("generationMode", config.get("generationMode"));
        response.put("generationDayOfMonth", config.get("generationDayOfMonth"));
        return response;
    }

    private void lockConfiguration() {
        jdbc.queryForObject("""
                SELECT id
                  FROM premium_generation_configuration
                 WHERE id = 'DEFAULT'
                 FOR UPDATE
                """, String.class);
    }

    private void markAutomaticRun(String period) {
        jdbc.update("""
                UPDATE premium_generation_configuration
                   SET last_run_at = CURRENT_TIMESTAMP,
                       last_generated_period = COALESCE(?, last_generated_period)
                 WHERE id = 'DEFAULT'
                """, period);
    }

    static String normalizeMode(String mode) {
        String normalized = mode == null ? DAY_OF_MONTH : mode.trim().toUpperCase();
        return LEGACY_FIRST_DAY_OF_MONTH.equals(normalized) ? DAY_OF_MONTH : normalized;
    }

    static int effectiveGenerationDay(YearMonth period, int configuredDay) {
        int safeDay = Math.max(1, Math.min(configuredDay, 31));
        return Math.min(safeDay, period.lengthOfMonth());
    }

    static List<YearMonth> sixPeriodsEnding(YearMonth toPeriod) {
        List<YearMonth> periods = new ArrayList<>(6);
        YearMonth start = toPeriod.minusMonths(5);
        for (int i = 0; i < 6; i++) {
            periods.add(start.plusMonths(i));
        }
        return List.copyOf(periods);
    }

    private LocalDate today() {
        return LocalDate.now(PREMIUM_ZONE);
    }

    private int parseGenerationDay(Object value) {
        if (value == null) {
            return 1;
        }
        try {
            int day = value instanceof Number number
                    ? number.intValue()
                    : Integer.parseInt(value.toString().trim());
            if (day < 1 || day > 31) {
                throw new IllegalArgumentException("Generation day must be between 1 and 31");
            }
            return day;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Generation day must be a number between 1 and 31", ex);
        }
    }

    private boolean parseBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = Objects.toString(value, "").trim();
        return "true".equalsIgnoreCase(text)
                || "1".equals(text)
                || "y".equalsIgnoreCase(text)
                || "yes".equalsIgnoreCase(text);
    }

    private boolean databaseBoolean(Object value) {
        if (value instanceof byte[] bytes) {
            return bytes.length > 0 && bytes[0] != 0;
        }
        return parseBoolean(value);
    }

    private int number(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private String actor(String user) {
        return user == null || user.isBlank() ? "SYSTEM" : user.trim();
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

    private LocalDateTime parseDateTime(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private record MembershipCandidate(
            String id,
            String oldId,
            String planId,
            Long premiumCents,
            LocalDate startDate,
            LocalDate endDate,
            String paidUpToPeriod
    ) {
    }

    private record PlanHistoryCandidate(
            String membershipId,
            String planId,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {
    }

    private record PremiumInsertCandidate(
            String membershipId,
            String periodValue,
            long amountCents,
            LocalDate dueDate,
            String createdBy
    ) {
    }

    private record PeriodGenerationResult(int created, int eligible, int missingAmount) {
    }

    record GenerationResult(
            int created,
            int eligibleMembershipPeriods,
            int alreadyPresent,
            int skippedMissingPremiumAmount,
            YearMonth fromPeriod,
            YearMonth toPeriod,
            List<String> periods
    ) {
        Map<String, Object> toMap() {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("created", created);
            response.put("eligibleMembershipPeriods", eligibleMembershipPeriods);
            response.put("alreadyPresent", alreadyPresent);
            response.put("skippedMissingPremiumAmount", skippedMissingPremiumAmount);
            response.put("fromPeriod", fromPeriod.format(PERIOD_FORMAT));
            response.put("toPeriod", toPeriod.format(PERIOD_FORMAT));
            response.put("periods", periods);
            return response;
        }
    }
}
