package za.co.mawa.bes.mapper.transaction;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.transaction.TransactionPartnerPKEntity;
import za.co.mawa.bes.dto.transaction.TransactionPartnerPKCreateRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionPartnerPKResponseDto;
import za.co.mawa.bes.dto.transaction.TransactionPartnerPKUpdateRequestDto;

@Component
public class TransactionPartnerPKMapper {

    public TransactionPartnerPKResponseDto toResponse(TransactionPartnerPKEntity entity) {
        if (entity == null) {
            return null;
        }

        return TransactionPartnerPKResponseDto.builder()
                .transaction(entity.getTransaction())
                .function(entity.getFunction())
                .partner(entity.getPartner())
                .build();
    }

    public TransactionPartnerPKEntity toEntity(TransactionPartnerPKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return TransactionPartnerPKEntity.builder()
                .transaction(request.getTransaction())
                .function(request.getFunction())
                .partner(request.getPartner())
                .build();
    }

    public void updateEntity(TransactionPartnerPKEntity entity, TransactionPartnerPKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setTransaction(request.getTransaction());
        entity.setFunction(request.getFunction());
        entity.setPartner(request.getPartner());
    }
}
