package za.co.mawa.bes.mapper.transaction;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.transaction.TransactionAmountEntity;
import za.co.mawa.bes.dto.transaction.TransactionAmountCreateRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionAmountResponseDto;
import za.co.mawa.bes.dto.transaction.TransactionAmountUpdateRequestDto;

@Component
public class TransactionAmountMapper {

    public TransactionAmountResponseDto toResponse(TransactionAmountEntity entity) {
        if (entity == null) {
            return null;
        }

        return TransactionAmountResponseDto.builder()
                .id(entity.getId())
                .transaction(entity.getTransaction())
                .type(entity.getType())
                .amount(entity.getAmount())
                .createdBy(entity.getCreatedBy())
                .changedBy(entity.getChangedBy())
                .build();
    }

    public TransactionAmountEntity toEntity(TransactionAmountCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return TransactionAmountEntity.builder()
                .transaction(request.getTransaction())
                .type(request.getType())
                .amount(request.getAmount())
                .changedBy(request.getChangedBy())
                .build();
    }

    public void updateEntity(TransactionAmountEntity entity, TransactionAmountUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setTransaction(request.getTransaction());
        entity.setType(request.getType());
        entity.setAmount(request.getAmount());
        entity.setChangedBy(request.getChangedBy());
    }
}
