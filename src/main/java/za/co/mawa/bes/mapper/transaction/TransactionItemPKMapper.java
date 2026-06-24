package za.co.mawa.bes.mapper.transaction;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.transaction.TransactionItemPKEntity;
import za.co.mawa.bes.dto.transaction.TransactionItemPKCreateRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionItemPKResponseDto;
import za.co.mawa.bes.dto.transaction.TransactionItemPKUpdateRequestDto;

@Component
public class TransactionItemPKMapper {

    public TransactionItemPKResponseDto toResponse(TransactionItemPKEntity entity) {
        if (entity == null) {
            return null;
        }

        return TransactionItemPKResponseDto.builder()
                .transaction(entity.getTransaction())
                .item(entity.getItem())
                .build();
    }

    public TransactionItemPKEntity toEntity(TransactionItemPKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return TransactionItemPKEntity.builder()
                .transaction(request.getTransaction())
                .item(request.getItem())
                .build();
    }

    public void updateEntity(TransactionItemPKEntity entity, TransactionItemPKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setTransaction(request.getTransaction());
        entity.setItem(request.getItem());
    }
}
