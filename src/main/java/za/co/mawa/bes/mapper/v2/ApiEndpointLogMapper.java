package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApiEndpointLogEntity;
import za.co.mawa.bes.dto.v2.ApiEndpointLogCreateRequestDto;
import za.co.mawa.bes.dto.v2.ApiEndpointLogResponseDto;
import za.co.mawa.bes.dto.v2.ApiEndpointLogUpdateRequestDto;

@Component
public class ApiEndpointLogMapper {

    public ApiEndpointLogResponseDto toResponse(ApiEndpointLogEntity entity) {
        if (entity == null) {
            return null;
        }

        return ApiEndpointLogResponseDto.builder()
                .id(entity.getId())
                .requestId(entity.getRequestId())
                .userId(entity.getUserId())
                .username(entity.getUsername())
                .method(entity.getMethod())
                .endpoint(entity.getEndpoint())
                .queryString(entity.getQueryString())
                .statusCode(entity.getStatusCode())
                .requestIp(entity.getRequestIp())
                .userAgent(entity.getUserAgent())
                .requestBody(entity.getRequestBody())
                .responseBody(entity.getResponseBody())
                .durationMs(entity.getDurationMs())
                .success(entity.getSuccess())
                .errorMessage(entity.getErrorMessage())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public ApiEndpointLogEntity toEntity(ApiEndpointLogCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return ApiEndpointLogEntity.builder()
                .requestId(request.getRequestId())
                .userId(request.getUserId())
                .username(request.getUsername())
                .method(request.getMethod())
                .endpoint(request.getEndpoint())
                .queryString(request.getQueryString())
                .statusCode(request.getStatusCode())
                .requestIp(request.getRequestIp())
                .userAgent(request.getUserAgent())
                .requestBody(request.getRequestBody())
                .responseBody(request.getResponseBody())
                .durationMs(request.getDurationMs())
                .success(request.getSuccess())
                .errorMessage(request.getErrorMessage())
                .build();
    }

    public void updateEntity(ApiEndpointLogEntity entity, ApiEndpointLogUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setRequestId(request.getRequestId());
        entity.setUserId(request.getUserId());
        entity.setUsername(request.getUsername());
        entity.setMethod(request.getMethod());
        entity.setEndpoint(request.getEndpoint());
        entity.setQueryString(request.getQueryString());
        entity.setStatusCode(request.getStatusCode());
        entity.setRequestIp(request.getRequestIp());
        entity.setUserAgent(request.getUserAgent());
        entity.setRequestBody(request.getRequestBody());
        entity.setResponseBody(request.getResponseBody());
        entity.setDurationMs(request.getDurationMs());
        entity.setSuccess(request.getSuccess());
        entity.setErrorMessage(request.getErrorMessage());
    }
}
