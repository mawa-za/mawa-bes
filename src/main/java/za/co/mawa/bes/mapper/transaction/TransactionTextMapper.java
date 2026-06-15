package za.co.mawa.bes.mapper.transaction;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.transaction.TransactionTextEntity;
import za.co.mawa.bes.dto.transaction.TransactionTextCreateRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionTextResponseDto;
import za.co.mawa.bes.dto.transaction.TransactionTextUpdateRequestDto;

@Component
public class TransactionTextMapper {

    public TransactionTextResponseDto toResponse(TransactionTextEntity entity) {
        if (entity == null) {
            return null;
        }

        return TransactionTextResponseDto.builder()
                .text(entity.getText())
                .build();
    }

    public TransactionTextEntity toEntity(TransactionTextCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return TransactionTextEntity.builder()
                .text(request.getText())
                .build();
    }

    public void updateEntity(TransactionTextEntity entity, TransactionTextUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setText(request.getText());
    }
}
