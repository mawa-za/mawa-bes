package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.entity.v2.ApiEndpointLogEntity;
import za.co.mawa.bes.repository.v2.ApiEndpointLogRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiEndpointLogAsyncWriter {

    private final ApiEndpointLogRepository repository;

    @Async("apiLogTaskExecutor")
    public void save(ApiEndpointLogEntity endpointLog, String tenant) {
        try {
            if (tenant == null || tenant.isBlank() || endpointLog == null) {
                return;
            }
            TenantContext.setCurrentTenant(tenant);
            repository.save(endpointLog);
        } catch (Exception e) {
            log.warn(
                    "API activity log was not persisted for tenant {} and request {}: {}",
                    tenant,
                    endpointLog == null ? null : endpointLog.getRequestId(),
                    rootMessage(e)
            );
        } finally {
            TenantContext.clear();
        }
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }
}
