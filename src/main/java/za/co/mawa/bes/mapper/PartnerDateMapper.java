package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerDateEntity;
import za.co.mawa.bes.dto.PartnerDateCreateRequestDto;
import za.co.mawa.bes.dto.PartnerDateResponseDto;
import za.co.mawa.bes.dto.PartnerDateUpdateRequestDto;

@Component
public class PartnerDateMapper {

    public PartnerDateResponseDto toResponse(PartnerDateEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerDateResponseDto.builder()
                .value(entity.getValue())
                .build();
    }

    public PartnerDateEntity toEntity(PartnerDateCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerDateEntity.builder()
                .value(request.getValue())
                .build();
    }

    public void updateEntity(PartnerDateEntity entity, PartnerDateUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setValue(request.getValue());
    }
}
