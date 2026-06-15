package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.ProductAttributeEntity;
import za.co.mawa.bes.dto.ProductAttributeCreateRequestDto;
import za.co.mawa.bes.dto.ProductAttributeResponseDto;
import za.co.mawa.bes.dto.ProductAttributeUpdateRequestDto;

@Component
public class ProductAttributeMapper {

    public ProductAttributeResponseDto toResponse(ProductAttributeEntity entity) {
        if (entity == null) {
            return null;
        }

        return ProductAttributeResponseDto.builder()
                .value(entity.getValue())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .build();
    }

    public ProductAttributeEntity toEntity(ProductAttributeCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return ProductAttributeEntity.builder()
                .value(request.getValue())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .build();
    }

    public void updateEntity(ProductAttributeEntity entity, ProductAttributeUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setValue(request.getValue());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
    }
}
