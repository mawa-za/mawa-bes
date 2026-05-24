package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.v2.ApiEndpointLogEntity;
import za.co.mawa.bes.repository.v2.ApiEndpointLogRepository;

@Service
@RequiredArgsConstructor
public class ApiEndpointLogService {

    private final ApiEndpointLogRepository repository;

    @Async
    public void save(ApiEndpointLogEntity log) {
        try {
            repository.save(log);
        } catch (Exception e) {
            // Do not break the actual API call if logging fails
            System.err.println("Failed to save endpoint log: " + e.getMessage());
        }
    }
}
