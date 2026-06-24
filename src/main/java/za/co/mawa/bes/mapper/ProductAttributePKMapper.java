package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.ProductAttributePKEntity;
import za.co.mawa.bes.dto.ProductAttributePKCreateRequestDto;
import za.co.mawa.bes.dto.ProductAttributePKResponseDto;
import za.co.mawa.bes.dto.ProductAttributePKUpdateRequestDto;

@Component
public class ProductAttributePKMapper {

    public ProductAttributePKResponseDto toResponse(ProductAttributePKEntity entity) {
        if (entity == null) {
            return null;
        }

        return ProductAttributePKResponseDto.builder()
                .product(entity.getProduct())
                .attribute(entity.getAttribute())
                .build();
    }

    public ProductAttributePKEntity toEntity(ProductAttributePKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return ProductAttributePKEntity.builder()
                .product(request.getProduct())
                .attribute(request.getAttribute())
                .build();
    }

    public void updateEntity(ProductAttributePKEntity entity, ProductAttributePKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setProduct(request.getProduct());
        entity.setAttribute(request.getAttribute());
    }
}
