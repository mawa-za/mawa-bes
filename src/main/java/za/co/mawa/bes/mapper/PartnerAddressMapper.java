package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerAddressEntity;
import za.co.mawa.bes.dto.PartnerAddressCreateRequestDto;
import za.co.mawa.bes.dto.PartnerAddressResponseDto;
import za.co.mawa.bes.dto.PartnerAddressUpdateRequestDto;

@Component
public class PartnerAddressMapper {

    public PartnerAddressResponseDto toResponse(PartnerAddressEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerAddressResponseDto.builder()
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .build();
    }

    public PartnerAddressEntity toEntity(PartnerAddressCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerAddressEntity.builder()
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .build();
    }

    public void updateEntity(PartnerAddressEntity entity, PartnerAddressUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
    }
}
