package za.co.mawa.bes.mapper.transaction;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.transaction.TransactionItemEntity;
import za.co.mawa.bes.dto.transaction.TransactionItemCreateRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionItemResponseDto;
import za.co.mawa.bes.dto.transaction.TransactionItemUpdateRequestDto;

@Component
public class TransactionItemMapper {

    public TransactionItemResponseDto toResponse(TransactionItemEntity entity) {
        if (entity == null) {
            return null;
        }

        return TransactionItemResponseDto.builder()
                .product(entity.getProduct())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .unitOfMeasure(entity.getUnitOfMeasure())
                .status(entity.getStatus())
                .build();
    }

    public TransactionItemEntity toEntity(TransactionItemCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return TransactionItemEntity.builder()
                .product(request.getProduct())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .unitOfMeasure(request.getUnitOfMeasure())
                .status(request.getStatus())
                .build();
    }

    public void updateEntity(TransactionItemEntity entity, TransactionItemUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setProduct(request.getProduct());
        entity.setQuantity(request.getQuantity());
        entity.setUnitPrice(request.getUnitPrice());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
        entity.setUnitOfMeasure(request.getUnitOfMeasure());
        entity.setStatus(request.getStatus());
    }
}
