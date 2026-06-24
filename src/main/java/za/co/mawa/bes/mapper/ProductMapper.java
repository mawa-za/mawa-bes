package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.ProductEntity;
import za.co.mawa.bes.dto.ProductCreateRequestDto;
import za.co.mawa.bes.dto.ProductResponseDto;
import za.co.mawa.bes.dto.ProductUpdateRequestDto;

@Component
public class ProductMapper {

    public ProductResponseDto toResponse(ProductEntity entity) {
        if (entity == null) {
            return null;
        }

        return ProductResponseDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .description(entity.getDescription())
                .type(entity.getType())
                .uom(entity.getUom())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .build();
    }

    public ProductEntity toEntity(ProductCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return ProductEntity.builder()
                .code(request.getCode())
                .description(request.getDescription())
                .type(request.getType())
                .uom(request.getUom())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .build();
    }

    public void updateEntity(ProductEntity entity, ProductUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setCode(request.getCode());
        entity.setDescription(request.getDescription());
        entity.setType(request.getType());
        entity.setUom(request.getUom());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
    }
}
