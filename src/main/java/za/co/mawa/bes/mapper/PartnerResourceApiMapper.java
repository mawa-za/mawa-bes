package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerResourceApiEntity;
import za.co.mawa.bes.dto.PartnerResourceApiCreateRequestDto;
import za.co.mawa.bes.dto.PartnerResourceApiResponseDto;
import za.co.mawa.bes.dto.PartnerResourceApiUpdateRequestDto;

@Component
public class PartnerResourceApiMapper {

    public PartnerResourceApiResponseDto toResponse(PartnerResourceApiEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerResourceApiResponseDto.builder()
                .resource_id(entity.getResource_id())
                .partner_url(entity.getPartner_url())
                .partner_no(entity.getPartner_no())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .status(entity.getStatus())
                .status_reason(entity.getStatus_reason())
                .port_number(entity.getPort_number())
                .resource_name(entity.getResource_name())
                .build();
    }

    public PartnerResourceApiEntity toEntity(PartnerResourceApiCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerResourceApiEntity.builder()
                .resource_id(request.getResource_id())
                .partner_url(request.getPartner_url())
                .partner_no(request.getPartner_no())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .status(request.getStatus())
                .status_reason(request.getStatus_reason())
                .port_number(request.getPort_number())
                .resource_name(request.getResource_name())
                .build();
    }

    public void updateEntity(PartnerResourceApiEntity entity, PartnerResourceApiUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setResource_id(request.getResource_id());
        entity.setPartner_url(request.getPartner_url());
        entity.setPartner_no(request.getPartner_no());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
        entity.setStatus(request.getStatus());
        entity.setStatus_reason(request.getStatus_reason());
        entity.setPort_number(request.getPort_number());
        entity.setResource_name(request.getResource_name());
    }
}
