package za.co.mawa.bes.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables MAWA scheduled/background jobs only for normal API runtime.
 *
 * Maintenance jobs can disable scheduling with mawa.scheduler.enabled=false.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "mawa.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfiguration {
}
