package za.co.mawa.bes.mapper.transaction;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.transaction.TransactionAttributeEntity;
import za.co.mawa.bes.dto.transaction.TransactionAttributeCreateRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionAttributeResponseDto;
import za.co.mawa.bes.dto.transaction.TransactionAttributeUpdateRequestDto;

@Component
public class TransactionAttributeMapper {

    public TransactionAttributeResponseDto toResponse(TransactionAttributeEntity entity) {
        if (entity == null) {
            return null;
        }

        return TransactionAttributeResponseDto.builder()
                .id(entity.getId())
                .transaction(entity.getTransaction())
                .attribute(entity.getAttribute())
                .value(entity.getValue())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .build();
    }

    public TransactionAttributeEntity toEntity(TransactionAttributeCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return TransactionAttributeEntity.builder()
                .transaction(request.getTransaction())
                .attribute(request.getAttribute())
                .value(request.getValue())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .build();
    }

    public void updateEntity(TransactionAttributeEntity entity, TransactionAttributeUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setTransaction(request.getTransaction());
        entity.setAttribute(request.getAttribute());
        entity.setValue(request.getValue());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
    }
}
