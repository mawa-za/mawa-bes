package za.co.mawa.bes.mapper.transaction;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.transaction.TransactionLinkPKEntity;
import za.co.mawa.bes.dto.transaction.TransactionLinkPKCreateRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionLinkPKResponseDto;
import za.co.mawa.bes.dto.transaction.TransactionLinkPKUpdateRequestDto;

@Component
public class TransactionLinkPKMapper {

    public TransactionLinkPKResponseDto toResponse(TransactionLinkPKEntity entity) {
        if (entity == null) {
            return null;
        }

        return TransactionLinkPKResponseDto.builder()
                .transaction1(entity.getTransaction1())
                .transaction2(entity.getTransaction2())
                .type(entity.getType())
                .build();
    }

    public TransactionLinkPKEntity toEntity(TransactionLinkPKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return TransactionLinkPKEntity.builder()
                .transaction1(request.getTransaction1())
                .transaction2(request.getTransaction2())
                .type(request.getType())
                .build();
    }

    public void updateEntity(TransactionLinkPKEntity entity, TransactionLinkPKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setTransaction1(request.getTransaction1());
        entity.setTransaction2(request.getTransaction2());
        entity.setType(request.getType());
    }
}
