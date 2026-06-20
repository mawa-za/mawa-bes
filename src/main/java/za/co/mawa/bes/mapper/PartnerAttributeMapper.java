package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerAttributeEntity;
import za.co.mawa.bes.dto.PartnerAttributeCreateRequestDto;
import za.co.mawa.bes.dto.PartnerAttributeResponseDto;
import za.co.mawa.bes.dto.PartnerAttributeUpdateRequestDto;

@Component
public class PartnerAttributeMapper {

    public PartnerAttributeResponseDto toResponse(PartnerAttributeEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerAttributeResponseDto.builder()
                .value(entity.getValue())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .build();
    }

    public PartnerAttributeEntity toEntity(PartnerAttributeCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerAttributeEntity.builder()
                .value(request.getValue())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .build();
    }

    public void updateEntity(PartnerAttributeEntity entity, PartnerAttributeUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setValue(request.getValue());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
    }
}
