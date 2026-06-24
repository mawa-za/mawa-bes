package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.AddressEntity;
import za.co.mawa.bes.dto.AddressCreateRequestDto;
import za.co.mawa.bes.dto.AddressResponseDto;
import za.co.mawa.bes.dto.AddressUpdateRequestDto;

@Component
public class AddressMapper {

    public AddressResponseDto toResponse(AddressEntity entity) {
        if (entity == null) {
            return null;
        }

        return AddressResponseDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .objectId(entity.getObjectId())
                .addressLine1(entity.getAddressLine1())
                .addressLine2(entity.getAddressLine2())
                .addressLine3(entity.getAddressLine3())
                .addressLine4(entity.getAddressLine4())
                .suburb(entity.getSuburb())
                .town(entity.getTown())
                .city(entity.getCity())
                .province(entity.getProvince())
                .postalCode(entity.getPostalCode())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .build();
    }

    public AddressEntity toEntity(AddressCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return AddressEntity.builder()
                .type(request.getType())
                .objectId(request.getObjectId())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .addressLine3(request.getAddressLine3())
                .addressLine4(request.getAddressLine4())
                .suburb(request.getSuburb())
                .town(request.getTown())
                .city(request.getCity())
                .province(request.getProvince())
                .postalCode(request.getPostalCode())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .build();
    }

    public void updateEntity(AddressEntity entity, AddressUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setType(request.getType());
        entity.setObjectId(request.getObjectId());
        entity.setAddressLine1(request.getAddressLine1());
        entity.setAddressLine2(request.getAddressLine2());
        entity.setAddressLine3(request.getAddressLine3());
        entity.setAddressLine4(request.getAddressLine4());
        entity.setSuburb(request.getSuburb());
        entity.setTown(request.getTown());
        entity.setCity(request.getCity());
        entity.setProvince(request.getProvince());
        entity.setPostalCode(request.getPostalCode());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
    }
}
