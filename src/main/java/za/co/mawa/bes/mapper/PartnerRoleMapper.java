package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerRoleEntity;
import za.co.mawa.bes.dto.PartnerRoleCreateRequestDto;
import za.co.mawa.bes.dto.PartnerRoleResponseDto;
import za.co.mawa.bes.dto.PartnerRoleUpdateRequestDto;

@Component
public class PartnerRoleMapper {

    public PartnerRoleResponseDto toResponse(PartnerRoleEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerRoleResponseDto.builder()
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .build();
    }

    public PartnerRoleEntity toEntity(PartnerRoleCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerRoleEntity.builder()
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .build();
    }

    public void updateEntity(PartnerRoleEntity entity, PartnerRoleUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
    }
}
