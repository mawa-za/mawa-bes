package za.co.mawa.bes.mapper.product;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.product.ProductCategoryEntity;
import za.co.mawa.bes.dto.product.ProductCategoryCreateRequestDto;
import za.co.mawa.bes.dto.product.ProductCategoryResponseDto;
import za.co.mawa.bes.dto.product.ProductCategoryUpdateRequestDto;

@Component
public class ProductCategoryMapper {

    public ProductCategoryResponseDto toResponse(ProductCategoryEntity entity) {
        if (entity == null) {
            return null;
        }

        return ProductCategoryResponseDto.builder()
                .id(entity.getId())
                .product(entity.getProduct())
                .category(entity.getCategory())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .build();
    }

    public ProductCategoryEntity toEntity(ProductCategoryCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return ProductCategoryEntity.builder()
                .product(request.getProduct())
                .category(request.getCategory())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .build();
    }

    public void updateEntity(ProductCategoryEntity entity, ProductCategoryUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setProduct(request.getProduct());
        entity.setCategory(request.getCategory());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
    }
}
