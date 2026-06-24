package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.ProductPricingEntity;
import za.co.mawa.bes.dto.ProductPricingCreateRequestDto;
import za.co.mawa.bes.dto.ProductPricingResponseDto;
import za.co.mawa.bes.dto.ProductPricingUpdateRequestDto;

@Component
public class ProductPricingMapper {

    public ProductPricingResponseDto toResponse(ProductPricingEntity entity) {
        if (entity == null) {
            return null;
        }

        return ProductPricingResponseDto.builder()
                .value(entity.getValue())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .build();
    }

    public ProductPricingEntity toEntity(ProductPricingCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return ProductPricingEntity.builder()
                .value(request.getValue())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .build();
    }

    public void updateEntity(ProductPricingEntity entity, ProductPricingUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setValue(request.getValue());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
    }
}
