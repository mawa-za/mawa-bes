package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerAddressPKEntity;
import za.co.mawa.bes.dto.PartnerAddressPKCreateRequestDto;
import za.co.mawa.bes.dto.PartnerAddressPKResponseDto;
import za.co.mawa.bes.dto.PartnerAddressPKUpdateRequestDto;

@Component
public class PartnerAddressPKMapper {

    public PartnerAddressPKResponseDto toResponse(PartnerAddressPKEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerAddressPKResponseDto.builder()
                .partner(entity.getPartner())
                .addressId(entity.getAddressId())
                .addressUsage(entity.getAddressUsage())
                .build();
    }

    public PartnerAddressPKEntity toEntity(PartnerAddressPKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerAddressPKEntity.builder()
                .partner(request.getPartner())
                .addressId(request.getAddressId())
                .addressUsage(request.getAddressUsage())
                .build();
    }

    public void updateEntity(PartnerAddressPKEntity entity, PartnerAddressPKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setPartner(request.getPartner());
        entity.setAddressId(request.getAddressId());
        entity.setAddressUsage(request.getAddressUsage());
    }
}
