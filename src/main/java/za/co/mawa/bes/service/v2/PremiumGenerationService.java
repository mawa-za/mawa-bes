package za.co.mawa.bes.service.v2;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class PremiumGenerationService {

    static final String DAY_OF_MONTH = "DAY_OF_MONTH";
    static final String LEGACY_FIRST_DAY_OF_MONTH = "FIRST_DAY_OF_MONTH";
    static final String MONTH_AFTER_LAST_PAYMENT = "MONTH_AFTER_LAST_PAYMENT";
    static final ZoneId PREMIUM_ZONE = ZoneId.of("Africa/Johannesburg");

    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final Set<String> SUPPORTED_MODES = Set.of(DAY_OF_MONTH, MONTH_AFTER_LAST_PAYMENT);

    private static final String PERIOD_AMOUNT_EXPRESSION = """
            COALESCE(
                historical_plan.premium_cents,
                m.premium_cents,
                current_plan.premium_cents
            )
            """;

    private static final String PERIOD_JOIN_SQL = """
            FROM membership m
            LEFT JOIN membership_plan current_plan
                   ON current_plan.id = m.plan_id
            LEFT JOIN membership_plan_history history
                   ON history.id = (
                       SELECT h.id
                         FROM membership_plan_history h
                        WHERE h.membership_id = m.id
                          AND h.effective_from <= ?
                          AND (h.effective_to IS NULL OR h.effective_to >= ?)
                        ORDER BY h.effective_from DESC
                        LIMIT 1
                   )
            LEFT JOIN membership_plan historical_plan
                   ON historical_plan.id = history.plan_id
            """;

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

        int created = jdbc.update("""
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
                )
                SELECT UUID(),
                       candidate.id,
                       DATE_FORMAT(candidate.target_month, '%Y%m'),
                       candidate.amount_cents,
                       0,
                       candidate.amount_cents,
                       'UNPAID',
                       ?,
                       CURRENT_TIMESTAMP,
                       ?
                  FROM (
                       SELECT m.id,
                              m.old_id,
                              m.end_date,
                              COALESCE(m.premium_cents, plan.premium_cents) AS amount_cents,
                              CASE
                                  WHEN m.paid_up_to_period REGEXP '^[0-9]{6}$'
                                  THEN DATE_ADD(
                                           STR_TO_DATE(CONCAT(m.paid_up_to_period, '01'), '%Y%m%d'),
                                           INTERVAL 1 MONTH
                                       )
                                  ELSE STR_TO_DATE(
                                           DATE_FORMAT(
                                               COALESCE(m.start_date, m.join_date, CURRENT_DATE),
                                               '%Y%m01'
                                           ),
                                           '%Y%m%d'
                                       )
                              END AS target_month
                         FROM membership m
                         LEFT JOIN membership_plan plan ON plan.id = m.plan_id
                        WHERE UPPER(TRIM(COALESCE(m.status, ''))) = 'ACTIVE'
                  ) candidate
                 WHERE candidate.target_month <= ?
                   AND (candidate.end_date IS NULL
                        OR candidate.target_month <= LAST_DAY(candidate.end_date))
                   AND candidate.amount_cents IS NOT NULL
                   AND NOT EXISTS (
                       SELECT 1
                         FROM membership_premium existing
                        WHERE existing.period_yyyymm = DATE_FORMAT(candidate.target_month, '%Y%m')
                          AND (
                              existing.membership_id = candidate.id
                              OR (candidate.old_id IS NOT NULL
                                  AND existing.membership_id = candidate.old_id)
                          )
                   )
                """, Date.valueOf(currentDate), actor(user), Date.valueOf(currentDate.withDayOfMonth(1)));

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

        int created = 0;
        int eligible = 0;
        int missingAmount = 0;
        List<String> periods = new ArrayList<>();

        YearMonth period = fromPeriod;
        while (!period.isAfter(toPeriod)) {
            periods.add(period.format(PERIOD_FORMAT));
            PeriodGenerationResult periodResult = generatePeriod(period, generationDay, actor(user));
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

    private PeriodGenerationResult generatePeriod(YearMonth period, int configuredDay, String user) {
        LocalDate periodStart = period.atDay(1);
        LocalDate periodEnd = period.atEndOfMonth();
        LocalDate dueDate = period.atDay(effectiveGenerationDay(period, configuredDay));
        String periodValue = period.format(PERIOD_FORMAT);

        String candidateStatsSql = """
                SELECT COUNT(*) AS eligible,
                       COALESCE(SUM(CASE WHEN candidate.amount_cents IS NULL THEN 1 ELSE 0 END), 0)
                           AS missing_amount
                  FROM (
                       SELECT %s AS amount_cents
                       %s
                        WHERE UPPER(TRIM(COALESCE(m.status, ''))) = 'ACTIVE'
                          AND COALESCE(m.start_date, m.join_date) <= ?
                          AND (m.end_date IS NULL OR m.end_date >= ?)
                  ) candidate
                """.formatted(PERIOD_AMOUNT_EXPRESSION, PERIOD_JOIN_SQL);

        Map<String, Object> candidateStats = jdbc.queryForMap(
                candidateStatsSql,
                Date.valueOf(periodEnd),
                Date.valueOf(periodStart),
                Date.valueOf(periodEnd),
                Date.valueOf(periodStart)
        );

        int eligible = number(candidateStats.get("eligible"));
        int missingAmount = number(candidateStats.get("missing_amount"));

        String insertSql = """
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
                )
                SELECT UUID(),
                       m.id,
                       ?,
                       %s,
                       0,
                       %s,
                       'UNPAID',
                       ?,
                       CURRENT_TIMESTAMP,
                       ?
                %s
                 WHERE UPPER(TRIM(COALESCE(m.status, ''))) = 'ACTIVE'
                   AND COALESCE(m.start_date, m.join_date) <= ?
                   AND (m.end_date IS NULL OR m.end_date >= ?)
                   AND %s IS NOT NULL
                   AND NOT EXISTS (
                       SELECT 1
                         FROM membership_premium existing
                        WHERE existing.period_yyyymm = ?
                          AND (
                              existing.membership_id = m.id
                              OR (m.old_id IS NOT NULL AND existing.membership_id = m.old_id)
                          )
                   )
                """.formatted(
                PERIOD_AMOUNT_EXPRESSION,
                PERIOD_AMOUNT_EXPRESSION,
                PERIOD_JOIN_SQL,
                PERIOD_AMOUNT_EXPRESSION
        );

        int created = jdbc.update(
                insertSql,
                periodValue,
                Date.valueOf(dueDate),
                user,
                Date.valueOf(periodEnd),
                Date.valueOf(periodStart),
                Date.valueOf(periodEnd),
                Date.valueOf(periodStart),
                periodValue
        );

        return new PeriodGenerationResult(created, eligible, missingAmount);
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
