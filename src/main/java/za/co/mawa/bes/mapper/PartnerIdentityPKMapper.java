package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerIdentityPKEntity;
import za.co.mawa.bes.dto.PartnerIdentityPKCreateRequestDto;
import za.co.mawa.bes.dto.PartnerIdentityPKResponseDto;
import za.co.mawa.bes.dto.PartnerIdentityPKUpdateRequestDto;

@Component
public class PartnerIdentityPKMapper {

    public PartnerIdentityPKResponseDto toResponse(PartnerIdentityPKEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerIdentityPKResponseDto.builder()
                .value(entity.getValue())
                .type(entity.getType())
                .build();
    }

    public PartnerIdentityPKEntity toEntity(PartnerIdentityPKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerIdentityPKEntity.builder()
                .value(request.getValue())
                .type(request.getType())
                .build();
    }

    public void updateEntity(PartnerIdentityPKEntity entity, PartnerIdentityPKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setValue(request.getValue());
        entity.setType(request.getType());
    }
}
