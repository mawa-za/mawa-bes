package za.co.mawa.bes.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * API activity logging is best-effort and must never create an unbounded
     * number of threads or retain an unbounded number of request objects.
     */
    @Bean(name = "apiLogTaskExecutor")
    public Executor apiLogTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(250);
        executor.setThreadNamePrefix("api-log-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Cross-tenant domain work must run on a fresh thread so Hibernate opens a
     * new tenant-bound EntityManager instead of reusing the request tenant's
     * OpenEntityManagerInView session. Calls using this executor are still
     * synchronous from the caller's perspective; the executor only provides
     * the clean persistence context boundary required by schema multitenancy.
     */
    @Bean(name = "crossTenantTaskExecutor")
    public Executor crossTenantTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cross-tenant-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
