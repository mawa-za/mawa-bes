package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerAttachmentEntity;
import za.co.mawa.bes.dto.PartnerAttachmentCreateRequestDto;
import za.co.mawa.bes.dto.PartnerAttachmentResponseDto;
import za.co.mawa.bes.dto.PartnerAttachmentUpdateRequestDto;

@Component
public class PartnerAttachmentMapper {

    public PartnerAttachmentResponseDto toResponse(PartnerAttachmentEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerAttachmentResponseDto.builder()
                .fileId(entity.getFileId())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .status(entity.getStatus())
                .build();
    }

    public PartnerAttachmentEntity toEntity(PartnerAttachmentCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerAttachmentEntity.builder()
                .fileId(request.getFileId())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .status(request.getStatus())
                .build();
    }

    public void updateEntity(PartnerAttachmentEntity entity, PartnerAttachmentUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setFileId(request.getFileId());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
        entity.setStatus(request.getStatus());
    }
}
