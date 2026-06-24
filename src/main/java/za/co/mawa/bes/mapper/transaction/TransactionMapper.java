package za.co.mawa.bes.mapper.transaction;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.transaction.TransactionEntity;
import za.co.mawa.bes.dto.transaction.TransactionCreateRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionResponseDto;
import za.co.mawa.bes.dto.transaction.TransactionUpdateRequestDto;

@Component
public class TransactionMapper {

    public TransactionResponseDto toResponse(TransactionEntity entity) {
        if (entity == null) {
            return null;
        }

        return TransactionResponseDto.builder()
                .id(entity.getId())
                .number(entity.getNumber())
                .type(entity.getType())
                .subType(entity.getSubType())
                .description(entity.getDescription())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .status(entity.getStatus())
                .statusReason(entity.getStatusReason())
                .subStatus(entity.getSubStatus())
                .location(entity.getLocation())
                .category(entity.getCategory())
                .subDescription(entity.getSubDescription())
                .createdBy(entity.getCreatedBy())
                .changedBy(entity.getChangedBy())
                .priority(entity.getPriority())
                .build();
    }

    public TransactionEntity toEntity(TransactionCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return TransactionEntity.builder()
                .number(request.getNumber())
                .type(request.getType())
                .subType(request.getSubType())
                .description(request.getDescription())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .status(request.getStatus())
                .statusReason(request.getStatusReason())
                .subStatus(request.getSubStatus())
                .location(request.getLocation())
                .category(request.getCategory())
                .subDescription(request.getSubDescription())
                .changedBy(request.getChangedBy())
                .priority(request.getPriority())
                .build();
    }

    public void updateEntity(TransactionEntity entity, TransactionUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setNumber(request.getNumber());
        entity.setType(request.getType());
        entity.setSubType(request.getSubType());
        entity.setDescription(request.getDescription());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
        entity.setStatus(request.getStatus());
        entity.setStatusReason(request.getStatusReason());
        entity.setSubStatus(request.getSubStatus());
        entity.setLocation(request.getLocation());
        entity.setCategory(request.getCategory());
        entity.setSubDescription(request.getSubDescription());
        entity.setChangedBy(request.getChangedBy());
        entity.setPriority(request.getPriority());
    }
}
