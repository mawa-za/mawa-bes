package za.co.mawa.bes.mapper.transaction;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.transaction.TransactionAttributePKEntity;
import za.co.mawa.bes.dto.transaction.TransactionAttributePKCreateRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionAttributePKResponseDto;
import za.co.mawa.bes.dto.transaction.TransactionAttributePKUpdateRequestDto;

@Component
public class TransactionAttributePKMapper {

    public TransactionAttributePKResponseDto toResponse(TransactionAttributePKEntity entity) {
        if (entity == null) {
            return null;
        }

        return TransactionAttributePKResponseDto.builder()
                .transaction(entity.getTransaction())
                .attribute(entity.getAttribute())
                .build();
    }

    public TransactionAttributePKEntity toEntity(TransactionAttributePKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return TransactionAttributePKEntity.builder()
                .transaction(request.getTransaction())
                .attribute(request.getAttribute())
                .build();
    }

    public void updateEntity(TransactionAttributePKEntity entity, TransactionAttributePKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setTransaction(request.getTransaction());
        entity.setAttribute(request.getAttribute());
    }
}
