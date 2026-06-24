package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.TenantPropertyEntity;
import za.co.mawa.bes.dto.TenantPropertyCreateRequestDto;
import za.co.mawa.bes.dto.TenantPropertyResponseDto;
import za.co.mawa.bes.dto.TenantPropertyUpdateRequestDto;

@Component
public class TenantPropertyMapper {

    public TenantPropertyResponseDto toResponse(TenantPropertyEntity entity) {
        if (entity == null) {
            return null;
        }

        return TenantPropertyResponseDto.builder()
                .value(entity.getValue())
                .build();
    }

    public TenantPropertyEntity toEntity(TenantPropertyCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return TenantPropertyEntity.builder()
                .value(request.getValue())
                .build();
    }

    public void updateEntity(TenantPropertyEntity entity, TenantPropertyUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setValue(request.getValue());
    }
}
