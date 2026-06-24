package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.ProductStorageBinPKEntity;
import za.co.mawa.bes.dto.ProductStorageBinPKCreateRequestDto;
import za.co.mawa.bes.dto.ProductStorageBinPKResponseDto;
import za.co.mawa.bes.dto.ProductStorageBinPKUpdateRequestDto;

@Component
public class ProductStorageBinPKMapper {

    public ProductStorageBinPKResponseDto toResponse(ProductStorageBinPKEntity entity) {
        if (entity == null) {
            return null;
        }

        return ProductStorageBinPKResponseDto.builder()
                .binId(entity.getBinId())
                .productId(entity.getProductId())
                .build();
    }

    public ProductStorageBinPKEntity toEntity(ProductStorageBinPKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return ProductStorageBinPKEntity.builder()
                .binId(request.getBinId())
                .productId(request.getProductId())
                .build();
    }

    public void updateEntity(ProductStorageBinPKEntity entity, ProductStorageBinPKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setBinId(request.getBinId());
        entity.setProductId(request.getProductId());
    }
}
