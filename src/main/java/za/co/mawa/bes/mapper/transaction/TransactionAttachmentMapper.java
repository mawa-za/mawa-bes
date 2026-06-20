package za.co.mawa.bes.mapper.transaction;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.transaction.TransactionAttachmentEntity;
import za.co.mawa.bes.dto.transaction.TransactionAttachmentCreateRequestDto;
import za.co.mawa.bes.dto.transaction.TransactionAttachmentResponseDto;
import za.co.mawa.bes.dto.transaction.TransactionAttachmentUpdateRequestDto;

@Component
public class TransactionAttachmentMapper {

    public TransactionAttachmentResponseDto toResponse(TransactionAttachmentEntity entity) {
        if (entity == null) {
            return null;
        }

        return TransactionAttachmentResponseDto.builder()
                .fileId(entity.getFileId())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .status(entity.getStatus())
                .build();
    }

    public TransactionAttachmentEntity toEntity(TransactionAttachmentCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return TransactionAttachmentEntity.builder()
                .fileId(request.getFileId())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .status(request.getStatus())
                .build();
    }

    public void updateEntity(TransactionAttachmentEntity entity, TransactionAttachmentUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setFileId(request.getFileId());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
        entity.setStatus(request.getStatus());
    }
}
