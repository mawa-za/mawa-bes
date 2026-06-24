package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.FuneralMortuaryInventoryEntity;
import za.co.mawa.bes.dto.v2.FuneralMortuaryInventoryCreateRequestDto;
import za.co.mawa.bes.dto.v2.FuneralMortuaryInventoryResponseDto;
import za.co.mawa.bes.dto.v2.FuneralMortuaryInventoryUpdateRequestDto;

@Component
public class FuneralMortuaryInventoryMapper {

    public FuneralMortuaryInventoryResponseDto toResponse(FuneralMortuaryInventoryEntity entity) {
        if (entity == null) {
            return null;
        }

        return FuneralMortuaryInventoryResponseDto.builder()
                .id(entity.getId())
                .pickupRequestId(entity.getPickupRequestId())
                .deceasedPartnerId(entity.getDeceasedPartnerId())
                .deceasedName(entity.getDeceasedName())
                .tagNumber(entity.getTagNumber())
                .checkInDate(entity.getCheckInDate())
                .status(entity.getStatus())
                .releaseTo(entity.getReleaseTo())
                .identityNumber(entity.getIdentityNumber())
                .checkoutDate(entity.getCheckoutDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public FuneralMortuaryInventoryEntity toEntity(FuneralMortuaryInventoryCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return FuneralMortuaryInventoryEntity.builder()
                .pickupRequestId(request.getPickupRequestId())
                .deceasedPartnerId(request.getDeceasedPartnerId())
                .deceasedName(request.getDeceasedName())
                .tagNumber(request.getTagNumber())
                .checkInDate(request.getCheckInDate())
                .status(request.getStatus())
                .releaseTo(request.getReleaseTo())
                .identityNumber(request.getIdentityNumber())
                .checkoutDate(request.getCheckoutDate())
                .build();
    }

    public void updateEntity(FuneralMortuaryInventoryEntity entity, FuneralMortuaryInventoryUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setPickupRequestId(request.getPickupRequestId());
        entity.setDeceasedPartnerId(request.getDeceasedPartnerId());
        entity.setDeceasedName(request.getDeceasedName());
        entity.setTagNumber(request.getTagNumber());
        entity.setCheckInDate(request.getCheckInDate());
        entity.setStatus(request.getStatus());
        entity.setReleaseTo(request.getReleaseTo());
        entity.setIdentityNumber(request.getIdentityNumber());
        entity.setCheckoutDate(request.getCheckoutDate());
    }
}
