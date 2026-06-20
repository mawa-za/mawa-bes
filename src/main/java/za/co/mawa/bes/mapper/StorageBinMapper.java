package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.StorageBinEntity;
import za.co.mawa.bes.dto.StorageBinCreateRequestDto;
import za.co.mawa.bes.dto.StorageBinResponseDto;
import za.co.mawa.bes.dto.StorageBinUpdateRequestDto;

@Component
public class StorageBinMapper {

    public StorageBinResponseDto toResponse(StorageBinEntity entity) {
        if (entity == null) {
            return null;
        }

        return StorageBinResponseDto.builder()
                .binId(entity.getBinId())
                .warehouseId(entity.getWarehouseId())
                .description(entity.getDescription())
                .aisle(entity.getAisle())
                .stack(entity.getStack())
                .shelf(entity.getShelf())
                .binCode(entity.getBinCode())
                .binType(entity.getBinType())
                .capacity(entity.getCapacity())
                .unitOfMeasure(entity.getUnitOfMeasure())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .published(entity.getPublished())
                .productId(entity.getProductId())
                .batchNumber(entity.getBatchNumber())
                .expiryDate(entity.getExpiryDate())
                .build();
    }

    public StorageBinEntity toEntity(StorageBinCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return StorageBinEntity.builder()
                .binId(request.getBinId())
                .warehouseId(request.getWarehouseId())
                .description(request.getDescription())
                .aisle(request.getAisle())
                .stack(request.getStack())
                .shelf(request.getShelf())
                .binCode(request.getBinCode())
                .binType(request.getBinType())
                .capacity(request.getCapacity())
                .unitOfMeasure(request.getUnitOfMeasure())
                .status(request.getStatus())
                .published(request.getPublished())
                .productId(request.getProductId())
                .batchNumber(request.getBatchNumber())
                .expiryDate(request.getExpiryDate())
                .build();
    }

    public void updateEntity(StorageBinEntity entity, StorageBinUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setBinId(request.getBinId());
        entity.setWarehouseId(request.getWarehouseId());
        entity.setDescription(request.getDescription());
        entity.setAisle(request.getAisle());
        entity.setStack(request.getStack());
        entity.setShelf(request.getShelf());
        entity.setBinCode(request.getBinCode());
        entity.setBinType(request.getBinType());
        entity.setCapacity(request.getCapacity());
        entity.setUnitOfMeasure(request.getUnitOfMeasure());
        entity.setStatus(request.getStatus());
        entity.setPublished(request.getPublished());
        entity.setProductId(request.getProductId());
        entity.setBatchNumber(request.getBatchNumber());
        entity.setExpiryDate(request.getExpiryDate());
    }
}
