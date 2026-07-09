package za.co.mawa.bes.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables MAWA scheduled/background jobs only for normal API runtime.
 *
 * Dedicated migration jobs must run with mawa.scheduler.enabled=false so
 * message queue and legacy membership schedulers do not execute while Flyway is
 * still bringing tenant schemas up to date.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "mawa.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfiguration {
}
