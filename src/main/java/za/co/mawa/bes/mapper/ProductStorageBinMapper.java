package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.ProductStorageBinCreateRequestDto;
import za.co.mawa.bes.dto.ProductStorageBinResponseDto;
import za.co.mawa.bes.dto.ProductStorageBinUpdateRequestDto;
import za.co.mawa.bes.entity.ProductStorageBinEntity;
import za.co.mawa.bes.entity.ProductStorageBinPKEntity;

@Component
public class ProductStorageBinMapper {
    public ProductStorageBinResponseDto toResponse(ProductStorageBinEntity entity) {
        if (entity == null) return null;
        ProductStorageBinPKEntity id = entity.getId();
        return ProductStorageBinResponseDto.builder()
                .productId(id != null ? id.getProductId() : null)
                .binId(id != null ? id.getBinId() : null)
                .minQty(entity.getMinQty())
                .maxQty(entity.getMaxQty())
                .build();
    }
    public ProductStorageBinEntity toEntity(ProductStorageBinCreateRequestDto request) {
        if (request == null) return null;
        return ProductStorageBinEntity.builder()
                .id(new ProductStorageBinPKEntity(request.getBinId(), request.getProductId()))
                .minQty(request.getMinQty())
                .maxQty(request.getMaxQty())
                .build();
    }
    public void updateEntity(ProductStorageBinEntity entity, ProductStorageBinUpdateRequestDto request) {
        if (entity == null || request == null) return;
        entity.setId(new ProductStorageBinPKEntity(request.getBinId(), request.getProductId()));
        entity.setMinQty(request.getMinQty());
        entity.setMaxQty(request.getMaxQty());
    }
}
