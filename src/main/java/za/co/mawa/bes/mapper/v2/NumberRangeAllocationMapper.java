package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.NumberRangeAllocationEntity;
import za.co.mawa.bes.dto.v2.NumberRangeAllocationCreateRequestDto;
import za.co.mawa.bes.dto.v2.NumberRangeAllocationResponseDto;
import za.co.mawa.bes.dto.v2.NumberRangeAllocationUpdateRequestDto;

@Component
public class NumberRangeAllocationMapper {

    public NumberRangeAllocationResponseDto toResponse(NumberRangeAllocationEntity entity) {
        if (entity == null) {
            return null;
        }

        return NumberRangeAllocationResponseDto.builder()
                .id(entity.getId())
                .seqType(entity.getSeqType())
                .deviceId(entity.getDeviceId())
                .fromNo(entity.getFromNo())
                .toNo(entity.getToNo())
                .nextLocalNo(entity.getNextLocalNo())
                .allocationSize(entity.getAllocationSize())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public NumberRangeAllocationEntity toEntity(NumberRangeAllocationCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return NumberRangeAllocationEntity.builder()
                .seqType(request.getSeqType())
                .deviceId(request.getDeviceId())
                .fromNo(request.getFromNo())
                .toNo(request.getToNo())
                .nextLocalNo(request.getNextLocalNo())
                .allocationSize(request.getAllocationSize())
                .status(request.getStatus())
                .build();
    }

    public void updateEntity(NumberRangeAllocationEntity entity, NumberRangeAllocationUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setSeqType(request.getSeqType());
        entity.setDeviceId(request.getDeviceId());
        entity.setFromNo(request.getFromNo());
        entity.setToNo(request.getToNo());
        entity.setNextLocalNo(request.getNextLocalNo());
        entity.setAllocationSize(request.getAllocationSize());
        entity.setStatus(request.getStatus());
    }
}
