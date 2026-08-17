package za.co.mawa.bes.service.v2;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Executes tenant-bound work on a clean worker thread.
 *
 * <p>The application uses Hibernate schema multitenancy together with
 * OpenEntityManagerInView. Changing {@link TenantContext} inside an existing
 * request does not retarget the EntityManager that was already opened for the
 * request tenant. A fresh worker thread is therefore required before invoking
 * another transactional Spring bean for a different tenant.</p>
 *
 * <p>Cross-tenant work runs as the protected BGUSER background identity. The
 * initiating user's tenant roles are intentionally not propagated into the
 * target tenant. Business requests that need to retain the human initiator
 * should carry that value in their request/audit payload.</p>
 */
@Service
public class CrossTenantExecutionService {

    private final Executor executor;
    private final BackgroundExecutionContextService backgroundExecutionContextService;

    public CrossTenantExecutionService(
            @Qualifier("crossTenantTaskExecutor") Executor executor,
            BackgroundExecutionContextService backgroundExecutionContextService
    ) {
        this.executor = executor;
        this.backgroundExecutionContextService = backgroundExecutionContextService;
    }

    public <T> T execute(
            String tenantId,
            String initiatingUsername,
            String initiatingUserId,
            Supplier<T> operation
    ) {
        if (tenantId == null || !tenantId.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new IllegalArgumentException("Invalid tenant identifier");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Cross-tenant operation is required");
        }

        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    TenantContext.setCurrentTenant(tenantId);
                    backgroundExecutionContextService.establish();
                    return operation.get();
                } finally {
                    backgroundExecutionContextService.clear();
                    TenantContext.clear();
                }
            }, executor).join();
        } catch (CompletionException exception) {
            Throwable cause = rootCause(exception);
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Cross-tenant operation failed", cause);
        }
    }

    private Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null
                && (current instanceof CompletionException
                || current instanceof java.util.concurrent.ExecutionException)) {
            current = current.getCause();
        }
        return current;
    }
}
