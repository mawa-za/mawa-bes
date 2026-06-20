package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.CashupSequenceEntity;
import za.co.mawa.bes.dto.CashupSequenceCreateRequestDto;
import za.co.mawa.bes.dto.CashupSequenceResponseDto;
import za.co.mawa.bes.dto.CashupSequenceUpdateRequestDto;

@Component
public class CashupSequenceMapper {

    public CashupSequenceResponseDto toResponse(CashupSequenceEntity entity) {
        if (entity == null) {
            return null;
        }

        return CashupSequenceResponseDto.builder()
                .id(entity.getId())
                .nextNo(entity.getNextNo())
                .build();
    }

    public CashupSequenceEntity toEntity(CashupSequenceCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return CashupSequenceEntity.builder()
                .nextNo(request.getNextNo())
                .build();
    }

    public void updateEntity(CashupSequenceEntity entity, CashupSequenceUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setNextNo(request.getNextNo());
    }
}
