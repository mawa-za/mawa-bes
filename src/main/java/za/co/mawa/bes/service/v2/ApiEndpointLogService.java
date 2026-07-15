package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.entity.v2.ApiEndpointLogEntity;

@Service
@RequiredArgsConstructor
public class ApiEndpointLogService {

    private final ApiEndpointLogAsyncWriter asyncWriter;

    public void saveAsync(ApiEndpointLogEntity endpointLog) {
        String tenant = TenantContext.getCurrentTenant();
        asyncWriter.save(endpointLog, tenant);
    }
}
