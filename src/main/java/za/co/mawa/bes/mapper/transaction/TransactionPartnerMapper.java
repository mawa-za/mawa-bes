package za.co.mawa.bes.mapper.transaction;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.transaction.TransactionPartnerEntity;
import za.co.mawa.bes.dto.transaction.TransactionPartnerCreateRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionPartnerResponseDto;
import za.co.mawa.bes.dto.transaction.TransactionPartnerUpdateRequestDto;

@Component
public class TransactionPartnerMapper {

    public TransactionPartnerResponseDto toResponse(TransactionPartnerEntity entity) {
        if (entity == null) {
            return null;
        }

        return TransactionPartnerResponseDto.builder()
                .dateAdded(entity.getDateAdded())
                .dateEffective(entity.getDateEffective())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .status(entity.getStatus())
                .statusReason(entity.getStatusReason())
                .createdBy(entity.getCreatedBy())
                .changedBy(entity.getChangedBy())
                .build();
    }

    public TransactionPartnerEntity toEntity(TransactionPartnerCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return TransactionPartnerEntity.builder()
                .dateAdded(request.getDateAdded())
                .dateEffective(request.getDateEffective())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .status(request.getStatus())
                .statusReason(request.getStatusReason())
                .changedBy(request.getChangedBy())
                .build();
    }

    public void updateEntity(TransactionPartnerEntity entity, TransactionPartnerUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setDateAdded(request.getDateAdded());
        entity.setDateEffective(request.getDateEffective());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
        entity.setStatus(request.getStatus());
        entity.setStatusReason(request.getStatusReason());
        entity.setChangedBy(request.getChangedBy());
    }
}
