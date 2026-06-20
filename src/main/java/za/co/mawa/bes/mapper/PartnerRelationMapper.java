package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerRelationEntity;
import za.co.mawa.bes.dto.PartnerRelationCreateRequestDto;
import za.co.mawa.bes.dto.PartnerRelationResponseDto;
import za.co.mawa.bes.dto.PartnerRelationUpdateRequestDto;

@Component
public class PartnerRelationMapper {

    public PartnerRelationResponseDto toResponse(PartnerRelationEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerRelationResponseDto.builder()
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .build();
    }

    public PartnerRelationEntity toEntity(PartnerRelationCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerRelationEntity.builder()
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .build();
    }

    public void updateEntity(PartnerRelationEntity entity, PartnerRelationUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
    }
}
