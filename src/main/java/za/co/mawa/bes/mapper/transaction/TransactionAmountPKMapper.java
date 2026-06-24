package za.co.mawa.bes.mapper.transaction;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.transaction.TransactionAmountPKEntity;
import za.co.mawa.bes.dto.transaction.TransactionAmountPKCreateRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionAmountPKResponseDto;
import za.co.mawa.bes.dto.transaction.TransactionAmountPKUpdateRequestDto;

@Component
public class TransactionAmountPKMapper {

    public TransactionAmountPKResponseDto toResponse(TransactionAmountPKEntity entity) {
        if (entity == null) {
            return null;
        }

        return TransactionAmountPKResponseDto.builder()
                .transaction(entity.getTransaction())
                .type(entity.getType())
                .build();
    }

    public TransactionAmountPKEntity toEntity(TransactionAmountPKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return TransactionAmountPKEntity.builder()
                .transaction(request.getTransaction())
                .type(request.getType())
                .build();
    }

    public void updateEntity(TransactionAmountPKEntity entity, TransactionAmountPKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setTransaction(request.getTransaction());
        entity.setType(request.getType());
    }
}
