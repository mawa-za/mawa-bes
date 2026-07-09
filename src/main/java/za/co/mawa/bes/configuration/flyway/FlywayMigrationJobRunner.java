package za.co.mawa.bes.configuration.flyway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Dedicated Flyway runner for Cloud Run Jobs / one-off migration commands.
 *
 * Normal API services should keep MAWA_FLYWAY_STARTUP_ENABLED=false so service
 * readiness is not blocked by tenant migrations. A migration job starts the same
 * application image with MAWA_FLYWAY_JOB_ENABLED=true, runs migrations in
 * blocking mode, then exits with code 0 or 1 for CI/CD gating.
 */
@Component
@Order(0)
public class FlywayMigrationJobRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(FlywayMigrationJobRunner.class);

    @Autowired
    private FlywayConfiguration flywayConfiguration;

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${mawa.flyway.job.enabled:false}")
    private boolean jobEnabled;

    @Value("${mawa.flyway.job.exit-on-complete:true}")
    private boolean exitOnComplete;

    @Override
    public void run(ApplicationArguments args) {
        if (!jobEnabled) {
            return;
        }

        int exitCode = 0;
        try {
            log.info("Starting dedicated MAWA Flyway migration job. exitOnComplete={}", exitOnComplete);
            FlywayConfiguration.FlywayRunStatus status = flywayConfiguration.runMigrationsBlocking("cloud-run-job");
            if (status != null && status.getFailureCount() > 0) {
                exitCode = 1;
                log.error("Dedicated MAWA Flyway migration job completed with {} failed schema(s): {}",
                        status.getFailureCount(), status.getFailedSchemas());
            } else {
                log.info("Dedicated MAWA Flyway migration job completed successfully");
            }
        } catch (Exception e) {
            exitCode = 1;
            log.error("Dedicated MAWA Flyway migration job failed", e);
        }

        if (exitOnComplete) {
            final int finalExitCode = exitCode;
            int springExitCode = SpringApplication.exit(applicationContext, new ExitCodeGenerator() {
                @Override
                public int getExitCode() {
                    return finalExitCode;
                }
            });
            System.exit(springExitCode);
        }
    }
}
