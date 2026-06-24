package za.co.mawa.bes.mapper.transaction;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.transaction.TransactionDateEntity;
import za.co.mawa.bes.dto.transaction.TransactionDateCreateRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionDateResponseDto;
import za.co.mawa.bes.dto.transaction.TransactionDateUpdateRequestDto;

@Component
public class TransactionDateMapper {

    public TransactionDateResponseDto toResponse(TransactionDateEntity entity) {
        if (entity == null) {
            return null;
        }

        return TransactionDateResponseDto.builder()
                .value(entity.getValue())
                .build();
    }

    public TransactionDateEntity toEntity(TransactionDateCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return TransactionDateEntity.builder()
                .value(request.getValue())
                .build();
    }

    public void updateEntity(TransactionDateEntity entity, TransactionDateUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setValue(request.getValue());
    }
}
