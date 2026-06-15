package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.InvoiceLineEntity;
import za.co.mawa.bes.dto.InvoiceLineCreateRequestDto;
import za.co.mawa.bes.dto.InvoiceLineResponseDto;
import za.co.mawa.bes.dto.InvoiceLineUpdateRequestDto;

@Component
public class InvoiceLineMapper {

    public InvoiceLineResponseDto toResponse(InvoiceLineEntity entity) {
        if (entity == null) {
            return null;
        }
        // TODO: map relation field `productId` to `productIdId` once the related entity id getter is confirmed.
        // TODO: map relation field `description` to `descriptionId` once the related entity id getter is confirmed.
        // TODO: map relation field `quantity` to `quantityId` once the related entity id getter is confirmed.
        // TODO: map relation field `unitPriceCents` to `unitPriceCentsId` once the related entity id getter is confirmed.
        return InvoiceLineResponseDto.builder()
                .id(entity.getId())
                .invoice(entity.getInvoice())
                .discountCents(entity.getDiscountCents())
                .taxCents(entity.getTaxCents())
                .subtotalCents(entity.getSubtotalCents())
                .totalCents(entity.getTotalCents())
                .build();
    }

    public InvoiceLineEntity toEntity(InvoiceLineCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return InvoiceLineEntity.builder()
                .invoice(request.getInvoice())
                .discountCents(request.getDiscountCents())
                .taxCents(request.getTaxCents())
                .subtotalCents(request.getSubtotalCents())
                .totalCents(request.getTotalCents())
                .build();
    }

    public void updateEntity(InvoiceLineEntity entity, InvoiceLineUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setInvoice(request.getInvoice());
        entity.setDiscountCents(request.getDiscountCents());
        entity.setTaxCents(request.getTaxCents());
        entity.setSubtotalCents(request.getSubtotalCents());
        entity.setTotalCents(request.getTotalCents());
    }
}
