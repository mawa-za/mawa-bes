package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerAttachmentPKEntity;
import za.co.mawa.bes.dto.PartnerAttachmentPKCreateRequestDto;
import za.co.mawa.bes.dto.PartnerAttachmentPKResponseDto;
import za.co.mawa.bes.dto.PartnerAttachmentPKUpdateRequestDto;

@Component
public class PartnerAttachmentPKMapper {

    public PartnerAttachmentPKResponseDto toResponse(PartnerAttachmentPKEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerAttachmentPKResponseDto.builder()
                .partner(entity.getPartner())
                .documentType(entity.getDocumentType())
                .build();
    }

    public PartnerAttachmentPKEntity toEntity(PartnerAttachmentPKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerAttachmentPKEntity.builder()
                .partner(request.getPartner())
                .documentType(request.getDocumentType())
                .build();
    }

    public void updateEntity(PartnerAttachmentPKEntity entity, PartnerAttachmentPKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setPartner(request.getPartner());
        entity.setDocumentType(request.getDocumentType());
    }
}
