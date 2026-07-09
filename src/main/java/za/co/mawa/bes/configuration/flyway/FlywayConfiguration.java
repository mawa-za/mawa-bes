package za.co.mawa.bes.configuration.flyway;

import jakarta.annotation.PreDestroy;
import org.apache.commons.dbcp.BasicDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.hibernate.cfg.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.service.EncryptionService;
import za.co.mawa.bes.service.TenantAdminService;
import za.co.mawa.bes.service.TenantService;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class FlywayConfiguration {
    private static final Logger log = LoggerFactory.getLogger(FlywayConfiguration.class);

    @Autowired
    DataSource dataSource;
    @Autowired
    EncryptionService encryptionService;
    @Value("${mawa.encryption.secret:${jwt.secret}}")
    private String encryptionSecret;
    @Autowired
    TenantAdminService tenantAdminService;
    @Autowired
    TenantService tenantService;

    @Value("${spring.flyway.locations}")
    private String DB_MIGRATION_TENANTS;
    private static final String DEFAULT_SCHEMA = "mawa";

    public static final String HIBERNATE_PROPERTIES_PATH = "/application-default.properties";

    @Value("${mawa.flyway.startup.enabled:true}")
    private boolean startupEnabled;

    /**
     * Supported values:
     * - async: start the API first, then run migrations in a background thread.
     * - blocking: keep the old behaviour and block startup until migrations finish.
     * - disabled: do not run custom MAWA Flyway migrations from this service instance.
     */
    @Value("${mawa.flyway.startup.mode:async}")
    private String startupMode;

    @Value("${mawa.flyway.default-schema.enabled:true}")
    private boolean defaultSchemaEnabled;

    @Value("${mawa.flyway.tenant-schemas.enabled:true}")
    private boolean tenantSchemasEnabled;

    @Value("${mawa.flyway.continue-on-error:true}")
    private boolean continueOnError;

    @Value("${mawa.flyway.repair-failed:false}")
    private boolean repairFailed;

    @Value("${mawa.flyway.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;

    @Value("${mawa.flyway.max-tenant-errors:0}")
    private int maxTenantErrors;

    private final AtomicBoolean migrationRunning = new AtomicBoolean(false);
    private final Map<String, FlywaySchemaMigrationStatus> schemaStatuses = new ConcurrentHashMap<>();
    private volatile FlywayRunStatus lastRunStatus = FlywayRunStatus.notStarted();
    private final ExecutorService flywayExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "mawa-flyway-runner");
            thread.setDaemon(true);
            return thread;
        }
    });

    @EventListener(ApplicationReadyEvent.class)
    public void tenantSchemaFlyway() {
        if (!startupEnabled || isMode("disabled")) {
            lastRunStatus = FlywayRunStatus.skipped("Flyway startup migration is disabled");
            log.info("MAWA Flyway startup migration is disabled. startupEnabled={}, mode={}", startupEnabled, startupMode);
            return;
        }

        if (isMode("blocking")) {
            runMigrations("startup-blocking", true);
            return;
        }

        runMigrationsAsync("startup-async");
    }

    public FlywayRunStatus runMigrationsAsync(String trigger) {
        if (!migrationRunning.compareAndSet(false, true)) {
            return FlywayRunStatus.alreadyRunning(lastRunStatus.getRunId());
        }
        String runId = UUID.randomUUID().toString();
        lastRunStatus = FlywayRunStatus.running(runId, trigger, "async");
        CompletableFuture.runAsync(() -> {
            try {
                doRunMigrations(runId, trigger, "async", false);
            } finally {
                migrationRunning.set(false);
            }
        }, flywayExecutor);
        return lastRunStatus;
    }

    public FlywayRunStatus runMigrationsBlocking(String trigger) {
        return runMigrations(trigger, false);
    }

    private FlywayRunStatus runMigrations(String trigger, boolean startup) {
        if (!migrationRunning.compareAndSet(false, true)) {
            return FlywayRunStatus.alreadyRunning(lastRunStatus.getRunId());
        }
        String runId = UUID.randomUUID().toString();
        lastRunStatus = FlywayRunStatus.running(runId, trigger, startup ? "blocking" : "manual-blocking");
        String runMode = startup ? "blocking" : "manual-blocking";
        try {
            doRunMigrations(runId, trigger, runMode, startup);
            return lastRunStatus;
        } finally {
            migrationRunning.set(false);
        }
    }

    private void doRunMigrations(String runId, String trigger, String runMode, boolean throwOnFailure) {
        Instant startedAt = Instant.now();
        int successCount = 0;
        int failureCount = 0;
        List<String> failedSchemas = new ArrayList<>();

        log.info("Starting MAWA Flyway migration run {}. trigger={}, mode={}, defaultSchemaEnabled={}, tenantSchemasEnabled={}, continueOnError={}",
                runId, trigger, startupMode, defaultSchemaEnabled, tenantSchemasEnabled, continueOnError);

        if (defaultSchemaEnabled) {
            FlywaySchemaMigrationStatus status = migrateSchema(DEFAULT_SCHEMA, "DEFAULT", dataSource);
            schemaStatuses.put(DEFAULT_SCHEMA, status);
            if (status.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
                failedSchemas.add(DEFAULT_SCHEMA);
                if (!continueOnError) {
                    finishRun(runId, trigger, runMode, startedAt, successCount, failureCount, failedSchemas);
                    RuntimeException failure = new RuntimeException("Default schema Flyway migration failed: " + status.getMessage());
                    if (throwOnFailure || !continueOnError) {
                        throw failure;
                    }
                    log.error(failure.getMessage(), failure);
                    return;
                }
            }
        }

        if (tenantSchemasEnabled) {
            List<TenantDto> tenants = loadTenants();
            int tenantErrorCount = 0;
            for (TenantDto tenant : tenants) {
                if (tenant == null || tenant.getId() == null || tenant.getId().isBlank()) {
                    continue;
                }
                FlywaySchemaMigrationStatus status = migrateSchema(tenant.getId(), "TENANT", dataSource);
                schemaStatuses.put(tenant.getId(), status);
                if (status.isSuccess()) {
                    successCount++;
                } else {
                    failureCount++;
                    tenantErrorCount++;
                    failedSchemas.add(tenant.getId());
                    if (!continueOnError || shouldStopAfterTenantErrors(tenantErrorCount)) {
                        finishRun(runId, trigger, runMode, startedAt, successCount, failureCount, failedSchemas);
                        RuntimeException failure = new RuntimeException("Tenant schema Flyway migration failed: " + tenant.getId() + " - " + status.getMessage());
                        if (throwOnFailure || !continueOnError) {
                            throw failure;
                        }
                        log.error(failure.getMessage(), failure);
                        return;
                    }
                }
            }
        }

        finishRun(runId, trigger, runMode, startedAt, successCount, failureCount, failedSchemas);
        if (failureCount > 0) {
            log.error("MAWA Flyway migration run {} completed with {} failures. failedSchemas={}", runId, failureCount, failedSchemas);
        } else {
            log.info("MAWA Flyway migration run {} completed successfully. schemasMigrated={}", runId, successCount);
        }
    }

    private void finishRun(String runId,
                           String trigger,
                           String runMode,
                           Instant startedAt,
                           int successCount,
                           int failureCount,
                           List<String> failedSchemas) {
        lastRunStatus = FlywayRunStatus.finished(
                runId,
                trigger,
                runMode,
                startedAt,
                Instant.now(),
                successCount,
                failureCount,
                failedSchemas
        );
    }

    private boolean shouldStopAfterTenantErrors(int tenantErrorCount) {
        return maxTenantErrors > 0 && tenantErrorCount >= maxTenantErrors;
    }

    private List<TenantDto> loadTenants() {
        try {
            List<TenantDto> tenants = tenantAdminService.getAll();
            if (tenants == null) {
                return Collections.emptyList();
            }
            return tenants;
        } catch (Exception e) {
            log.error("Unable to load tenants for Flyway migration", e);
            if (!continueOnError) {
                throw e;
            }
            schemaStatuses.put("__TENANT_LIST__", FlywaySchemaMigrationStatus.failed("__TENANT_LIST__", "TENANT_LIST", e));
            return Collections.emptyList();
        }
    }

    private FlywaySchemaMigrationStatus migrateSchema(String schema, String schemaType, DataSource migrationDataSource) {
        FlywaySchemaMigrationStatus status = FlywaySchemaMigrationStatus.running(schema, schemaType);
        schemaStatuses.put(schema, status);
        try {
            log.info("Running Flyway migration for {} schema `{}`", schemaType, schema);
            Flyway flyway = Flyway.configure()
                    .locations(DB_MIGRATION_TENANTS)
                    .baselineOnMigrate(baselineOnMigrate)
                    .dataSource(migrationDataSource)
                    .schemas(schema)
                    .createSchemas(true)
                    .load();

            if (repairFailed) {
                log.warn("Running Flyway repair before migrate for schema `{}` because mawa.flyway.repair-failed=true", schema);
                flyway.repair();
            }

            MigrateResult result = flyway.migrate();
            MigrationInfo current = flyway.info().current();
            return FlywaySchemaMigrationStatus.success(
                    schema,
                    schemaType,
                    result.migrationsExecuted,
                    current == null ? null : current.getVersion() == null ? null : current.getVersion().toString(),
                    current == null ? null : current.getDescription()
            );
        } catch (Exception e) {
            log.error("Flyway migration failed for {} schema `{}`", schemaType, schema, e);
            return FlywaySchemaMigrationStatus.failed(schema, schemaType, e);
        }
    }

    private boolean isMode(String expected) {
        return expected.equalsIgnoreCase(startupMode == null ? "" : startupMode.trim());
    }

    public Map<String, Object> getMigrationStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startupEnabled", startupEnabled);
        result.put("startupMode", startupMode);
        result.put("defaultSchemaEnabled", defaultSchemaEnabled);
        result.put("tenantSchemasEnabled", tenantSchemasEnabled);
        result.put("continueOnError", continueOnError);
        result.put("repairFailed", repairFailed);
        result.put("baselineOnMigrate", baselineOnMigrate);
        result.put("maxTenantErrors", maxTenantErrors);
        result.put("running", migrationRunning.get());
        result.put("lastRun", lastRunStatus);
        List<FlywaySchemaMigrationStatus> schemas = new ArrayList<>(schemaStatuses.values());
        schemas.sort(Comparator.comparing(FlywaySchemaMigrationStatus::getSchemaType).thenComparing(FlywaySchemaMigrationStatus::getSchema));
        result.put("schemas", schemas);
        return result;
    }

    public Pair<String, BasicDataSource> dataSource(String tenantId) {
        try {
            Properties properties = tenantService.getTenantProperties(tenantId);
            BasicDataSource dataSource = new BasicDataSource();
            dataSource.setDriverClassName(properties.get(Environment.DRIVER).toString());
            dataSource.setUrl(properties.get(Environment.URL).toString());
            dataSource.setUsername(properties.get(Environment.USER).toString());
            String password = encryptionService.decrypt(properties.get(Environment.PASS).toString(), resolveEncryptionSecret(properties));
            dataSource.setPassword(password);
            return Pair.of(properties.get(Environment.DEFAULT_SCHEMA).toString(), dataSource);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private String resolveEncryptionSecret(Properties properties) {
        Object tenantEncryptionSecret = properties.get("mawa.encryption.secret");
        if (tenantEncryptionSecret != null && !tenantEncryptionSecret.toString().isBlank()) {
            return tenantEncryptionSecret.toString();
        }
        Object legacyTenantJwtSecret = properties.get("jwt.secret");
        if (legacyTenantJwtSecret != null && !legacyTenantJwtSecret.toString().isBlank()) {
            return legacyTenantJwtSecret.toString();
        }
        return encryptionSecret;
    }

    @PreDestroy
    public void shutdownFlywayExecutor() {
        flywayExecutor.shutdownNow();
    }

    public static class FlywayRunStatus {
        private String runId;
        private String trigger;
        private String mode;
        private String state;
        private Instant startedAt;
        private Instant finishedAt;
        private int successCount;
        private int failureCount;
        private List<String> failedSchemas = new ArrayList<>();
        private String message;

        public static FlywayRunStatus notStarted() {
            FlywayRunStatus status = new FlywayRunStatus();
            status.state = "NOT_STARTED";
            status.message = "No MAWA Flyway run has started yet";
            return status;
        }

        public static FlywayRunStatus skipped(String message) {
            FlywayRunStatus status = new FlywayRunStatus();
            status.state = "SKIPPED";
            status.message = message;
            status.finishedAt = Instant.now();
            return status;
        }

        public static FlywayRunStatus alreadyRunning(String runId) {
            FlywayRunStatus status = new FlywayRunStatus();
            status.runId = runId;
            status.state = "ALREADY_RUNNING";
            status.message = "A MAWA Flyway migration run is already in progress";
            return status;
        }

        public static FlywayRunStatus running(String runId, String trigger, String mode) {
            FlywayRunStatus status = new FlywayRunStatus();
            status.runId = runId;
            status.trigger = trigger;
            status.mode = mode;
            status.state = "RUNNING";
            status.startedAt = Instant.now();
            status.message = "MAWA Flyway migration run is running";
            return status;
        }

        public static FlywayRunStatus finished(String runId,
                                               String trigger,
                                               String mode,
                                               Instant startedAt,
                                               Instant finishedAt,
                                               int successCount,
                                               int failureCount,
                                               List<String> failedSchemas) {
            FlywayRunStatus status = new FlywayRunStatus();
            status.runId = runId;
            status.trigger = trigger;
            status.mode = mode;
            status.state = failureCount == 0 ? "SUCCESS" : "COMPLETED_WITH_ERRORS";
            status.startedAt = startedAt;
            status.finishedAt = finishedAt;
            status.successCount = successCount;
            status.failureCount = failureCount;
            status.failedSchemas = failedSchemas == null ? new ArrayList<>() : new ArrayList<>(failedSchemas);
            status.message = failureCount == 0
                    ? "MAWA Flyway migration run completed successfully"
                    : "MAWA Flyway migration run completed with errors";
            return status;
        }

        public String getRunId() { return runId; }
        public String getTrigger() { return trigger; }
        public String getMode() { return mode; }
        public String getState() { return state; }
        public Instant getStartedAt() { return startedAt; }
        public Instant getFinishedAt() { return finishedAt; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public List<String> getFailedSchemas() { return failedSchemas; }
        public String getMessage() { return message; }
    }

    public static class FlywaySchemaMigrationStatus {
        private String schema;
        private String schemaType;
        private String state;
        private Instant startedAt;
        private Instant finishedAt;
        private int migrationsExecuted;
        private String currentVersion;
        private String currentDescription;
        private String message;

        public static FlywaySchemaMigrationStatus running(String schema, String schemaType) {
            FlywaySchemaMigrationStatus status = new FlywaySchemaMigrationStatus();
            status.schema = schema;
            status.schemaType = schemaType;
            status.state = "RUNNING";
            status.startedAt = Instant.now();
            status.message = "Migration is running";
            return status;
        }

        public static FlywaySchemaMigrationStatus success(String schema,
                                                          String schemaType,
                                                          int migrationsExecuted,
                                                          String currentVersion,
                                                          String currentDescription) {
            FlywaySchemaMigrationStatus status = new FlywaySchemaMigrationStatus();
            status.schema = schema;
            status.schemaType = schemaType;
            status.state = "SUCCESS";
            status.startedAt = Instant.now();
            status.finishedAt = Instant.now();
            status.migrationsExecuted = migrationsExecuted;
            status.currentVersion = currentVersion;
            status.currentDescription = currentDescription;
            status.message = "Migration completed successfully";
            return status;
        }

        public static FlywaySchemaMigrationStatus failed(String schema, String schemaType, Exception e) {
            FlywaySchemaMigrationStatus status = new FlywaySchemaMigrationStatus();
            status.schema = schema;
            status.schemaType = schemaType;
            status.state = "FAILED";
            status.startedAt = Instant.now();
            status.finishedAt = Instant.now();
            status.message = e == null ? "Migration failed" : e.getMessage();
            return status;
        }

        public boolean isSuccess() {
            return "SUCCESS".equalsIgnoreCase(state);
        }

        public String getSchema() { return schema; }
        public String getSchemaType() { return schemaType; }
        public String getState() { return state; }
        public Instant getStartedAt() { return startedAt; }
        public Instant getFinishedAt() { return finishedAt; }
        public int getMigrationsExecuted() { return migrationsExecuted; }
        public String getCurrentVersion() { return currentVersion; }
        public String getCurrentDescription() { return currentDescription; }
        public String getMessage() { return message; }
    }
}
