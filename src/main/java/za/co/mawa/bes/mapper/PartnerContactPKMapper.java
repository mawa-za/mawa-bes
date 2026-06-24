package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerContactPKEntity;
import za.co.mawa.bes.dto.PartnerContactPKCreateRequestDto;
import za.co.mawa.bes.dto.PartnerContactPKResponseDto;
import za.co.mawa.bes.dto.PartnerContactPKUpdateRequestDto;

@Component
public class PartnerContactPKMapper {

    public PartnerContactPKResponseDto toResponse(PartnerContactPKEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerContactPKResponseDto.builder()
                .partner(entity.getPartner())
                .type(entity.getType())
                .build();
    }

    public PartnerContactPKEntity toEntity(PartnerContactPKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerContactPKEntity.builder()
                .partner(request.getPartner())
                .type(request.getType())
                .build();
    }

    public void updateEntity(PartnerContactPKEntity entity, PartnerContactPKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setPartner(request.getPartner());
        entity.setType(request.getType());
    }
}
