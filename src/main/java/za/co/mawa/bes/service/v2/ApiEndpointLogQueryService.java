package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.v2.ApiEndpointLogResponseDto;
import za.co.mawa.bes.entity.v2.ApiEndpointLogEntity;
import za.co.mawa.bes.repository.v2.ApiEndpointLogRepository;

@Service
@RequiredArgsConstructor
public class ApiEndpointLogQueryService {

    private final ApiEndpointLogRepository repository;

    public Page<ApiEndpointLogResponseDto> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(this::toDto);
    }

    private ApiEndpointLogResponseDto toDto(ApiEndpointLogEntity entity) {
        ApiEndpointLogResponseDto dto = new ApiEndpointLogResponseDto();

        dto.setId(entity.getId());
        dto.setRequestId(entity.getRequestId());

        dto.setUserId(entity.getUserId());
        dto.setUsername(entity.getUsername());

        dto.setMethod(entity.getMethod());
        dto.setEndpoint(entity.getEndpoint());
        dto.setQueryString(entity.getQueryString());

        dto.setStatusCode(entity.getStatusCode());

        dto.setRequestIp(entity.getRequestIp());
        dto.setUserAgent(entity.getUserAgent());

        dto.setDurationMs(entity.getDurationMs());

        dto.setSuccess(entity.getSuccess());
        dto.setErrorMessage(entity.getErrorMessage());

        dto.setCreatedAt(entity.getCreatedAt());

        return dto;
    }
}
