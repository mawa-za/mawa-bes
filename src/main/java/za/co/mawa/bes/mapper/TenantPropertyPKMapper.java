package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.TenantPropertyPKEntity;
import za.co.mawa.bes.dto.TenantPropertyPKCreateRequestDto;
import za.co.mawa.bes.dto.TenantPropertyPKResponseDto;
import za.co.mawa.bes.dto.TenantPropertyPKUpdateRequestDto;

@Component
public class TenantPropertyPKMapper {

    public TenantPropertyPKResponseDto toResponse(TenantPropertyPKEntity entity) {
        if (entity == null) {
            return null;
        }

        return TenantPropertyPKResponseDto.builder()
                .tenant(entity.getTenant())
                .property(entity.getProperty())
                .build();
    }

    public TenantPropertyPKEntity toEntity(TenantPropertyPKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return TenantPropertyPKEntity.builder()
                .tenant(request.getTenant())
                .property(request.getProperty())
                .build();
    }

    public void updateEntity(TenantPropertyPKEntity entity, TenantPropertyPKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setTenant(request.getTenant());
        entity.setProperty(request.getProperty());
    }
}
