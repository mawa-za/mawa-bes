package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerRolePKEntity;
import za.co.mawa.bes.dto.PartnerRolePKCreateRequestDto;
import za.co.mawa.bes.dto.PartnerRolePKResponseDto;
import za.co.mawa.bes.dto.PartnerRolePKUpdateRequestDto;

@Component
public class PartnerRolePKMapper {

    public PartnerRolePKResponseDto toResponse(PartnerRolePKEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerRolePKResponseDto.builder()
                .id(entity.getId())
                .role(entity.getRole())
                .build();
    }

    public PartnerRolePKEntity toEntity(PartnerRolePKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerRolePKEntity.builder()
                .role(request.getRole())
                .build();
    }

    public void updateEntity(PartnerRolePKEntity entity, PartnerRolePKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setRole(request.getRole());
    }
}
