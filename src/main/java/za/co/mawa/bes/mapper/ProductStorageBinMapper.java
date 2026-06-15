package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.ProductStorageBinEntity;
import za.co.mawa.bes.dto.ProductStorageBinCreateRequestDto;
import za.co.mawa.bes.dto.ProductStorageBinResponseDto;
import za.co.mawa.bes.dto.ProductStorageBinUpdateRequestDto;

@Component
public class ProductStorageBinMapper {

    public ProductStorageBinResponseDto toResponse(ProductStorageBinEntity entity) {
        if (entity == null) {
            return null;
        }

        return ProductStorageBinResponseDto.builder()
                .id(entity.getId())
                .minQty(entity.getMinQty())
                .maxQty(entity.getMaxQty())
                .build();
    }

    public ProductStorageBinEntity toEntity(ProductStorageBinCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return ProductStorageBinEntity.builder()
                .minQty(request.getMinQty())
                .maxQty(request.getMaxQty())
                .build();
    }

    public void updateEntity(ProductStorageBinEntity entity, ProductStorageBinUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setMinQty(request.getMinQty());
        entity.setMaxQty(request.getMaxQty());
    }
}
