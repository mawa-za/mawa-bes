package za.co.mawa.bes.mapper.transaction;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;
import za.co.mawa.bes.dto.transaction.TransactionLinkCreateRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionLinkResponseDto;
import za.co.mawa.bes.dto.transaction.TransactionLinkUpdateRequestDto;

@Component
public class TransactionLinkMapper {

    public TransactionLinkResponseDto toResponse(TransactionLinkEntity entity) {
        if (entity == null) {
            return null;
        }

        return TransactionLinkResponseDto.builder()
                .created_by(entity.getCreated_by())
                .creation_date(entity.getCreation_date())
                .build();
    }

    public TransactionLinkEntity toEntity(TransactionLinkCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return TransactionLinkEntity.builder()
                .created_by(request.getCreated_by())
                .creation_date(request.getCreation_date())
                .build();
    }

    public void updateEntity(TransactionLinkEntity entity, TransactionLinkUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setCreated_by(request.getCreated_by());
        entity.setCreation_date(request.getCreation_date());
    }
}
