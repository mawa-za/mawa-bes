package za.co.mawa.bes.mapper.transaction;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.transaction.TransactionTextPKEntity;
import za.co.mawa.bes.dto.transaction.TransactionTextPKCreateRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionTextPKResponseDto;
import za.co.mawa.bes.dto.transaction.TransactionTextPKUpdateRequestDto;

@Component
public class TransactionTextPKMapper {

    public TransactionTextPKResponseDto toResponse(TransactionTextPKEntity entity) {
        if (entity == null) {
            return null;
        }

        return TransactionTextPKResponseDto.builder()
                .transaction(entity.getTransaction())
                .type(entity.getType())
                .build();
    }

    public TransactionTextPKEntity toEntity(TransactionTextPKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return TransactionTextPKEntity.builder()
                .transaction(request.getTransaction())
                .type(request.getType())
                .build();
    }

    public void updateEntity(TransactionTextPKEntity entity, TransactionTextPKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setTransaction(request.getTransaction());
        entity.setType(request.getType());
    }
}
