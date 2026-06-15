package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.ReceiptRangeAllocationEntity;
import za.co.mawa.bes.dto.ReceiptRangeAllocationCreateRequestDto;
import za.co.mawa.bes.dto.ReceiptRangeAllocationResponseDto;
import za.co.mawa.bes.dto.ReceiptRangeAllocationUpdateRequestDto;

@Component
public class ReceiptRangeAllocationMapper {

    public ReceiptRangeAllocationResponseDto toResponse(ReceiptRangeAllocationEntity entity) {
        if (entity == null) {
            return null;
        }

        return ReceiptRangeAllocationResponseDto.builder()
                .id(entity.getId())
                .deviceId(entity.getDeviceId())
                .fromNo(entity.getFromNo())
                .toNo(entity.getToNo())
                .nextNo(entity.getNextNo())
                .status(entity.getStatus())
                .allocatedAt(entity.getAllocatedAt())
                .build();
    }

    public ReceiptRangeAllocationEntity toEntity(ReceiptRangeAllocationCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return ReceiptRangeAllocationEntity.builder()
                .deviceId(request.getDeviceId())
                .fromNo(request.getFromNo())
                .toNo(request.getToNo())
                .nextNo(request.getNextNo())
                .status(request.getStatus())
                .allocatedAt(request.getAllocatedAt())
                .build();
    }

    public void updateEntity(ReceiptRangeAllocationEntity entity, ReceiptRangeAllocationUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setDeviceId(request.getDeviceId());
        entity.setFromNo(request.getFromNo());
        entity.setToNo(request.getToNo());
        entity.setNextNo(request.getNextNo());
        entity.setStatus(request.getStatus());
        entity.setAllocatedAt(request.getAllocatedAt());
    }
}
