package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerContactEntity;
import za.co.mawa.bes.dto.PartnerContactCreateRequestDto;
import za.co.mawa.bes.dto.PartnerContactResponseDto;
import za.co.mawa.bes.dto.PartnerContactUpdateRequestDto;

@Component
public class PartnerContactMapper {

    public PartnerContactResponseDto toResponse(PartnerContactEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerContactResponseDto.builder()
                .value(entity.getValue())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .build();
    }

    public PartnerContactEntity toEntity(PartnerContactCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerContactEntity.builder()
                .value(request.getValue())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .build();
    }

    public void updateEntity(PartnerContactEntity entity, PartnerContactUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setValue(request.getValue());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
    }
}
