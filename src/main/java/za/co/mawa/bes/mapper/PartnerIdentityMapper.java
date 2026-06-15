package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerIdentityEntity;
import za.co.mawa.bes.dto.PartnerIdentityCreateRequestDto;
import za.co.mawa.bes.dto.PartnerIdentityResponseDto;
import za.co.mawa.bes.dto.PartnerIdentityUpdateRequestDto;

@Component
public class PartnerIdentityMapper {

    public PartnerIdentityResponseDto toResponse(PartnerIdentityEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerIdentityResponseDto.builder()
                .partner(entity.getPartner())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .build();
    }

    public PartnerIdentityEntity toEntity(PartnerIdentityCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerIdentityEntity.builder()
                .partner(request.getPartner())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .build();
    }

    public void updateEntity(PartnerIdentityEntity entity, PartnerIdentityUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setPartner(request.getPartner());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
    }
}
