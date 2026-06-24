package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.RoleEntity;
import za.co.mawa.bes.dto.RoleCreateRequestDto;
import za.co.mawa.bes.dto.RoleResponseDto;
import za.co.mawa.bes.dto.RoleUpdateRequestDto;

@Component
public class RoleMapper {

    public RoleResponseDto toResponse(RoleEntity entity) {
        if (entity == null) {
            return null;
        }

        return RoleResponseDto.builder()
                .id(entity.getId())
                .description(entity.getDescription())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .build();
    }

    public RoleEntity toEntity(RoleCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return RoleEntity.builder()
                .description(request.getDescription())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .build();
    }

    public void updateEntity(RoleEntity entity, RoleUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setDescription(request.getDescription());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
    }
}
