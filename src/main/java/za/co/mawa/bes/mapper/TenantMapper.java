package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.TenantEntity;
import za.co.mawa.bes.dto.TenantCreateRequestDto;
import za.co.mawa.bes.dto.TenantResponseDto;
import za.co.mawa.bes.dto.TenantUpdateRequestDto;

@Component
public class TenantMapper {

    public TenantResponseDto toResponse(TenantEntity entity) {
        if (entity == null) {
            return null;
        }

        return TenantResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .url(entity.getUrl())
                .host(entity.getHost())
                .status(entity.getStatus())
                .build();
    }

    public TenantEntity toEntity(TenantCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return TenantEntity.builder()
                .name(request.getName())
                .url(request.getUrl())
                .host(request.getHost())
                .status(request.getStatus())
                .build();
    }

    public void updateEntity(TenantEntity entity, TenantUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setName(request.getName());
        entity.setUrl(request.getUrl());
        entity.setHost(request.getHost());
        entity.setStatus(request.getStatus());
    }
}
