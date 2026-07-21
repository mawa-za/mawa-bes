package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.v2.NumberSequenceCreateRequestDto;
import za.co.mawa.bes.dto.v2.NumberSequenceResponseDto;
import za.co.mawa.bes.dto.v2.NumberSequenceUpdateRequestDto;
import za.co.mawa.bes.entity.v2.NumberSequenceEntity;

@Component
public class NumberSequenceMapper {

    public NumberSequenceResponseDto toResponse(NumberSequenceEntity entity) {
        if (entity == null) return null;
        long remaining = Math.max(0L, entity.getEndNo() - entity.getNextNo() + 1L);
        boolean exhausted = entity.getNextNo() > entity.getEndNo();
        boolean lowRange = !exhausted && remaining <= entity.getWarningThreshold();
        return NumberSequenceResponseDto.builder()
                .id(entity.getId())
                .seqType(entity.getSeqType())
                .description(entity.getDescription())
                .startNo(entity.getStartNo())
                .nextNo(entity.getNextNo())
                .endNo(entity.getEndNo())
                .remainingNumbers(remaining)
                .defaultAllocationSize(entity.getDefaultAllocationSize())
                .warningThreshold(entity.getWarningThreshold())
                .active(entity.getActive())
                .exhausted(exhausted)
                .lowRange(lowRange)
                .lockVersion(entity.getLockVersion())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public NumberSequenceEntity toEntity(NumberSequenceCreateRequestDto request) {
        if (request == null) return null;
        return NumberSequenceEntity.builder()
                .seqType(request.getSeqType())
                .description(request.getDescription())
                .startNo(request.getStartNo())
                .nextNo(request.getNextNo())
                .endNo(request.getEndNo())
                .defaultAllocationSize(request.getDefaultAllocationSize())
                .warningThreshold(request.getWarningThreshold())
                .active(request.getActive())
                .build();
    }

    public void updateEntity(NumberSequenceEntity entity, NumberSequenceUpdateRequestDto request) {
        if (entity == null || request == null) return;
        entity.setDescription(request.getDescription());
        entity.setStartNo(request.getStartNo());
        entity.setNextNo(request.getNextNo());
        entity.setEndNo(request.getEndNo());
        entity.setDefaultAllocationSize(request.getDefaultAllocationSize());
        entity.setWarningThreshold(request.getWarningThreshold());
        entity.setActive(request.getActive());
    }
}
