package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerAttributePKEntity;
import za.co.mawa.bes.dto.PartnerAttributePKCreateRequestDto;
import za.co.mawa.bes.dto.PartnerAttributePKResponseDto;
import za.co.mawa.bes.dto.PartnerAttributePKUpdateRequestDto;

@Component
public class PartnerAttributePKMapper {

    public PartnerAttributePKResponseDto toResponse(PartnerAttributePKEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerAttributePKResponseDto.builder()
                .partner(entity.getPartner())
                .attribute(entity.getAttribute())
                .build();
    }

    public PartnerAttributePKEntity toEntity(PartnerAttributePKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerAttributePKEntity.builder()
                .partner(request.getPartner())
                .attribute(request.getAttribute())
                .build();
    }

    public void updateEntity(PartnerAttributePKEntity entity, PartnerAttributePKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setPartner(request.getPartner());
        entity.setAttribute(request.getAttribute());
    }
}
