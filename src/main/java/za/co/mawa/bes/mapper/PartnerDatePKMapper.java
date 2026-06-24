package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerDatePKEntity;
import za.co.mawa.bes.dto.PartnerDatePKCreateRequestDto;
import za.co.mawa.bes.dto.PartnerDatePKResponseDto;
import za.co.mawa.bes.dto.PartnerDatePKUpdateRequestDto;

@Component
public class PartnerDatePKMapper {

    public PartnerDatePKResponseDto toResponse(PartnerDatePKEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerDatePKResponseDto.builder()
                .partner_no(entity.getPartnerNumber())
                .type(entity.getType())
                .build();
    }

    public PartnerDatePKEntity toEntity(PartnerDatePKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerDatePKEntity.builder()
                .partnerNumber(request.getPartner_no())
                .type(request.getType())
                .build();
    }

    public void updateEntity(PartnerDatePKEntity entity, PartnerDatePKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setPartnerNumber(request.getPartner_no());
        entity.setType(request.getType());
    }
}
