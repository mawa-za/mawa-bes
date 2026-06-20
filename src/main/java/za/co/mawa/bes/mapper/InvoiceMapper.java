package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.dto.InvoiceCreateRequestDto;
import za.co.mawa.bes.dto.InvoiceResponseDto;
import za.co.mawa.bes.dto.InvoiceUpdateRequestDto;

@Component
public class InvoiceMapper {

    public InvoiceResponseDto toResponse(InvoiceEntity entity) {
        if (entity == null) {
            return null;
        }
        // TODO: map relation field `payments` to `paymentsIds` once the related entity id getter is confirmed.
        return InvoiceResponseDto.builder()
                .id(entity.getId())
                .invoiceNo(entity.getInvoiceNo())
                .externalRef(entity.getExternalRef())
                .partnerId(entity.getPartnerId())
                .invoiceDate(entity.getInvoiceDate())
                .dueDate(entity.getDueDate())
                .status(entity.getStatus())
                .subtotalCents(entity.getSubtotalCents())
                .taxCents(entity.getTaxCents())
                .discountCents(entity.getDiscountCents())
                .totalCents(entity.getTotalCents())
                .paidCents(entity.getPaidCents())
                .balanceCents(entity.getBalanceCents())
                .currency(entity.getCurrency())
                .notes(entity.getNotes())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .lines(entity.getLines())
                .build();
    }

    public InvoiceEntity toEntity(InvoiceCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return InvoiceEntity.builder()
                .invoiceNo(request.getInvoiceNo())
                .externalRef(request.getExternalRef())
                .partnerId(request.getPartnerId())
                .invoiceDate(request.getInvoiceDate())
                .dueDate(request.getDueDate())
                .status(request.getStatus())
                .subtotalCents(request.getSubtotalCents())
                .taxCents(request.getTaxCents())
                .discountCents(request.getDiscountCents())
                .totalCents(request.getTotalCents())
                .paidCents(request.getPaidCents())
                .balanceCents(request.getBalanceCents())
                .currency(request.getCurrency())
                .notes(request.getNotes())
                .lines(request.getLines())
                .build();
    }

    public void updateEntity(InvoiceEntity entity, InvoiceUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setInvoiceNo(request.getInvoiceNo());
        entity.setExternalRef(request.getExternalRef());
        entity.setPartnerId(request.getPartnerId());
        entity.setInvoiceDate(request.getInvoiceDate());
        entity.setDueDate(request.getDueDate());
        entity.setStatus(request.getStatus());
        entity.setSubtotalCents(request.getSubtotalCents());
        entity.setTaxCents(request.getTaxCents());
        entity.setDiscountCents(request.getDiscountCents());
        entity.setTotalCents(request.getTotalCents());
        entity.setPaidCents(request.getPaidCents());
        entity.setBalanceCents(request.getBalanceCents());
        entity.setCurrency(request.getCurrency());
        entity.setNotes(request.getNotes());
        entity.setLines(request.getLines());
    }
}
