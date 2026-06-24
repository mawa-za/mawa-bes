package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.FuneralServiceInvoiceEntity;
import za.co.mawa.bes.dto.v2.FuneralServiceInvoiceCreateRequestDto;
import za.co.mawa.bes.dto.v2.FuneralServiceInvoiceResponseDto;
import za.co.mawa.bes.dto.v2.FuneralServiceInvoiceUpdateRequestDto;

@Component
public class FuneralServiceInvoiceMapper {

    public FuneralServiceInvoiceResponseDto toResponse(FuneralServiceInvoiceEntity entity) {
        if (entity == null) {
            return null;
        }

        return FuneralServiceInvoiceResponseDto.builder()
                .id(entity.getId())
                .funeralServiceId(entity.getFuneralServiceId())
                .invoiceId(entity.getInvoiceId())
                .entityType(entity.getEntityType())
                .partnerId(entity.getPartnerId())
                .membershipClaimId(entity.getMembershipClaimId())
                .amountCents(entity.getAmountCents())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public FuneralServiceInvoiceEntity toEntity(FuneralServiceInvoiceCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return FuneralServiceInvoiceEntity.builder()
                .funeralServiceId(request.getFuneralServiceId())
                .invoiceId(request.getInvoiceId())
                .entityType(request.getEntityType())
                .partnerId(request.getPartnerId())
                .membershipClaimId(request.getMembershipClaimId())
                .amountCents(request.getAmountCents())
                .build();
    }

    public void updateEntity(FuneralServiceInvoiceEntity entity, FuneralServiceInvoiceUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setFuneralServiceId(request.getFuneralServiceId());
        entity.setInvoiceId(request.getInvoiceId());
        entity.setEntityType(request.getEntityType());
        entity.setPartnerId(request.getPartnerId());
        entity.setMembershipClaimId(request.getMembershipClaimId());
        entity.setAmountCents(request.getAmountCents());
    }
}
