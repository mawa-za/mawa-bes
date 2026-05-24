package za.co.mawa.bes.service.v2;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.entity.v2.ApiEndpointLogEntity;
import za.co.mawa.bes.repository.v2.ApiEndpointLogRepository;

@Service
@RequiredArgsConstructor
public class ApiEndpointLogService {

    private final ApiEndpointLogRepository repository;

    public void saveAsync(ApiEndpointLogEntity endpointLog) {
        String tenant = TenantContext.getCurrentTenant();
        asyncSave(endpointLog, tenant);
    }

    @Async
    public void asyncSave(ApiEndpointLogEntity endpointLog, String tenant) {
        try {
            TenantContext.setCurrentTenant(tenant);
            repository.save(endpointLog);
        } finally {
            TenantContext.clear();
        }
    }
}
