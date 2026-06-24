package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.CashupRangeAllocationEntity;
import za.co.mawa.bes.dto.CashupRangeAllocationCreateRequestDto;
import za.co.mawa.bes.dto.CashupRangeAllocationResponseDto;
import za.co.mawa.bes.dto.CashupRangeAllocationUpdateRequestDto;

@Component
public class CashupRangeAllocationMapper {

    public CashupRangeAllocationResponseDto toResponse(CashupRangeAllocationEntity entity) {
        if (entity == null) {
            return null;
        }

        return CashupRangeAllocationResponseDto.builder()
                .id(entity.getId())
                .deviceId(entity.getDeviceId())
                .fromNo(entity.getFromNo())
                .toNo(entity.getToNo())
                .nextNo(entity.getNextNo())
                .status(entity.getStatus())
                .allocatedAt(entity.getAllocatedAt())
                .build();
    }

    public CashupRangeAllocationEntity toEntity(CashupRangeAllocationCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return CashupRangeAllocationEntity.builder()
                .deviceId(request.getDeviceId())
                .fromNo(request.getFromNo())
                .toNo(request.getToNo())
                .nextNo(request.getNextNo())
                .status(request.getStatus())
                .allocatedAt(request.getAllocatedAt())
                .build();
    }

    public void updateEntity(CashupRangeAllocationEntity entity, CashupRangeAllocationUpdateRequestDto request) {
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
