package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.ProductPricingPKEntity;
import za.co.mawa.bes.dto.ProductPricingPKCreateRequestDto;
import za.co.mawa.bes.dto.ProductPricingPKResponseDto;
import za.co.mawa.bes.dto.ProductPricingPKUpdateRequestDto;

@Component
public class ProductPricingPKMapper {

    public ProductPricingPKResponseDto toResponse(ProductPricingPKEntity entity) {
        if (entity == null) {
            return null;
        }

        return ProductPricingPKResponseDto.builder()
                .product(entity.getProduct())
                .pricing(entity.getPricing())
                .build();
    }

    public ProductPricingPKEntity toEntity(ProductPricingPKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return ProductPricingPKEntity.builder()
                .product(request.getProduct())
                .pricing(request.getPricing())
                .build();
    }

    public void updateEntity(ProductPricingPKEntity entity, ProductPricingPKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setProduct(request.getProduct());
        entity.setPricing(request.getPricing());
    }
}
