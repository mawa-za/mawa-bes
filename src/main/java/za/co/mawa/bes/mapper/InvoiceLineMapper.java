package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.InvoiceLineCreateRequestDto;
import za.co.mawa.bes.dto.InvoiceLineResponseDto;
import za.co.mawa.bes.dto.InvoiceLineUpdateRequestDto;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoiceLineEntity;

@Component
public class InvoiceLineMapper {
    public InvoiceLineResponseDto toResponse(InvoiceLineEntity entity) {
        if (entity == null) return null;
        return InvoiceLineResponseDto.builder()
                .id(entity.getId())
                .invoiceId(entity.getInvoice() != null ? entity.getInvoice().getId() : null)
                .productId(entity.getProductId())
                .description(entity.getDescription())
                .quantity(entity.getQuantity())
                .unitPriceCents(entity.getUnitPriceCents())
                .discountCents(entity.getDiscountCents())
                .taxCents(entity.getTaxCents())
                .subtotalCents(entity.getSubtotalCents())
                .totalCents(entity.getTotalCents())
                .build();
    }
    public InvoiceLineEntity toEntity(InvoiceLineCreateRequestDto request) {
        if (request == null) return null;
        return InvoiceLineEntity.builder()
                .invoice(invoiceRef(request.getInvoiceId()))
                .productId(request.getProductId())
                .description(request.getDescription())
                .quantity(request.getQuantity())
                .unitPriceCents(request.getUnitPriceCents())
                .discountCents(request.getDiscountCents())
                .taxCents(request.getTaxCents())
                .subtotalCents(request.getSubtotalCents())
                .totalCents(request.getTotalCents())
                .build();
    }
    public void updateEntity(InvoiceLineEntity entity, InvoiceLineUpdateRequestDto request) {
        if (entity == null || request == null) return;
        entity.setId(request.getId());
        entity.setInvoice(invoiceRef(request.getInvoiceId()));
        entity.setProductId(request.getProductId());
        entity.setDescription(request.getDescription());
        entity.setQuantity(request.getQuantity());
        entity.setUnitPriceCents(request.getUnitPriceCents());
        entity.setDiscountCents(request.getDiscountCents());
        entity.setTaxCents(request.getTaxCents());
        entity.setSubtotalCents(request.getSubtotalCents());
        entity.setTotalCents(request.getTotalCents());
    }
    private InvoiceEntity invoiceRef(String id) {
        if (id == null || id.isBlank()) return null;
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(id);
        return invoice;
    }
}
