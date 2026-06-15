package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerRelationPKEntity;
import za.co.mawa.bes.dto.PartnerRelationPKCreateRequestDto;
import za.co.mawa.bes.dto.PartnerRelationPKResponseDto;
import za.co.mawa.bes.dto.PartnerRelationPKUpdateRequestDto;

@Component
public class PartnerRelationPKMapper {

    public PartnerRelationPKResponseDto toResponse(PartnerRelationPKEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerRelationPKResponseDto.builder()
                .type(entity.getType())
                .partner1(entity.getPartner1())
                .partner2(entity.getPartner2())
                .build();
    }

    public PartnerRelationPKEntity toEntity(PartnerRelationPKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerRelationPKEntity.builder()
                .type(request.getType())
                .partner1(request.getPartner1())
                .partner2(request.getPartner2())
                .build();
    }

    public void updateEntity(PartnerRelationPKEntity entity, PartnerRelationPKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setType(request.getType());
        entity.setPartner1(request.getPartner1());
        entity.setPartner2(request.getPartner2());
    }
}
