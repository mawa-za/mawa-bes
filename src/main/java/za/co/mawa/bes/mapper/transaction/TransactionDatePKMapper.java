package za.co.mawa.bes.mapper.transaction;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.transaction.TransactionDatePKEntity;
import za.co.mawa.bes.dto.transaction.TransactionDatePKCreateRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionDatePKResponseDto;
import za.co.mawa.bes.dto.transaction.TransactionDatePKUpdateRequestDto;

@Component
public class TransactionDatePKMapper {

    public TransactionDatePKResponseDto toResponse(TransactionDatePKEntity entity) {
        if (entity == null) {
            return null;
        }

        return TransactionDatePKResponseDto.builder()
                .transaction(entity.getTransaction())
                .type(entity.getType())
                .build();
    }

    public TransactionDatePKEntity toEntity(TransactionDatePKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return TransactionDatePKEntity.builder()
                .transaction(request.getTransaction())
                .type(request.getType())
                .build();
    }

    public void updateEntity(TransactionDatePKEntity entity, TransactionDatePKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setTransaction(request.getTransaction());
        entity.setType(request.getType());
    }
}
