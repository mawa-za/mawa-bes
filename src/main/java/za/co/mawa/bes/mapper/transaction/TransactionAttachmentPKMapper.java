package za.co.mawa.bes.mapper.transaction;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.transaction.TransactionAttachmentPKEntity;
import za.co.mawa.bes.dto.transaction.TransactionAttachmentPKCreateRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionAttachmentPKResponseDto;
import za.co.mawa.bes.dto.transaction.TransactionAttachmentPKUpdateRequestDto;

@Component
public class TransactionAttachmentPKMapper {

    public TransactionAttachmentPKResponseDto toResponse(TransactionAttachmentPKEntity entity) {
        if (entity == null) {
            return null;
        }

        return TransactionAttachmentPKResponseDto.builder()
                .transaction(entity.getTransaction())
                .documentType(entity.getDocumentType())
                .build();
    }

    public TransactionAttachmentPKEntity toEntity(TransactionAttachmentPKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return TransactionAttachmentPKEntity.builder()
                .transaction(request.getTransaction())
                .documentType(request.getDocumentType())
                .build();
    }

    public void updateEntity(TransactionAttachmentPKEntity entity, TransactionAttachmentPKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setTransaction(request.getTransaction());
        entity.setDocumentType(request.getDocumentType());
    }
}
