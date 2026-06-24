package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.NumberSequenceEntity;
import za.co.mawa.bes.dto.v2.NumberSequenceCreateRequestDto;
import za.co.mawa.bes.dto.v2.NumberSequenceResponseDto;
import za.co.mawa.bes.dto.v2.NumberSequenceUpdateRequestDto;

@Component
public class NumberSequenceMapper {

    public NumberSequenceResponseDto toResponse(NumberSequenceEntity entity) {
        if (entity == null) {
            return null;
        }

        return NumberSequenceResponseDto.builder()
                .id(entity.getId())
                .seqType(entity.getSeqType())
                .nextNo(entity.getNextNo())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public NumberSequenceEntity toEntity(NumberSequenceCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return NumberSequenceEntity.builder()
                .seqType(request.getSeqType())
                .nextNo(request.getNextNo())
                .build();
    }

    public void updateEntity(NumberSequenceEntity entity, NumberSequenceUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setSeqType(request.getSeqType());
        entity.setNextNo(request.getNextNo());
    }
}
