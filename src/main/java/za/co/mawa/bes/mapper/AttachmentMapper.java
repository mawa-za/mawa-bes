package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.AttachmentEntity;
import za.co.mawa.bes.dto.AttachmentCreateRequestDto;
import za.co.mawa.bes.dto.AttachmentResponseDto;
import za.co.mawa.bes.dto.AttachmentUpdateRequestDto;

@Component
public class AttachmentMapper {

    public AttachmentResponseDto toResponse(AttachmentEntity entity) {
        if (entity == null) {
            return null;
        }

        return AttachmentResponseDto.builder()
                .id(entity.getId())
                .objectId(entity.getObjectId())
                .documentType(entity.getDocumentType())
                .uploadBy(entity.getUploadBy())
                .uploadTime(entity.getUploadTime())
                .uploadDate(entity.getUploadDate())
                .downloadBy(entity.getDownloadBy())
                .downloadDate(entity.getDownloadDate())
                .extension(entity.getExtension())
                .build();
    }

    public AttachmentEntity toEntity(AttachmentCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return AttachmentEntity.builder()
                .objectId(request.getObjectId())
                .documentType(request.getDocumentType())
                .uploadBy(request.getUploadBy())
                .uploadTime(request.getUploadTime())
                .uploadDate(request.getUploadDate())
                .downloadBy(request.getDownloadBy())
                .downloadDate(request.getDownloadDate())
                .extension(request.getExtension())
                .build();
    }

    public void updateEntity(AttachmentEntity entity, AttachmentUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setObjectId(request.getObjectId());
        entity.setDocumentType(request.getDocumentType());
        entity.setUploadBy(request.getUploadBy());
        entity.setUploadTime(request.getUploadTime());
        entity.setUploadDate(request.getUploadDate());
        entity.setDownloadBy(request.getDownloadBy());
        entity.setDownloadDate(request.getDownloadDate());
        entity.setExtension(request.getExtension());
    }
}
