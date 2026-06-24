package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.ReceiptSequenceEntity;
import za.co.mawa.bes.dto.ReceiptSequenceCreateRequestDto;
import za.co.mawa.bes.dto.ReceiptSequenceResponseDto;
import za.co.mawa.bes.dto.ReceiptSequenceUpdateRequestDto;

@Component
public class ReceiptSequenceMapper {

    public ReceiptSequenceResponseDto toResponse(ReceiptSequenceEntity entity) {
        if (entity == null) {
            return null;
        }

        return ReceiptSequenceResponseDto.builder()
                .id(entity.getId())
                .nextNo(entity.getNextNo())
                .build();
    }

    public ReceiptSequenceEntity toEntity(ReceiptSequenceCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return ReceiptSequenceEntity.builder()
                .nextNo(request.getNextNo())
                .build();
    }

    public void updateEntity(ReceiptSequenceEntity entity, ReceiptSequenceUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setNextNo(request.getNextNo());
    }
}
