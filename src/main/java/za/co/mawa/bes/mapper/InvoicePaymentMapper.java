package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.InvoicePaymentCreateRequestDto;
import za.co.mawa.bes.dto.InvoicePaymentResponseDto;
import za.co.mawa.bes.dto.InvoicePaymentUpdateRequestDto;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoicePaymentEntity;

@Component
public class InvoicePaymentMapper {
    public InvoicePaymentResponseDto toResponse(InvoicePaymentEntity entity) {
        if (entity == null) return null;
        return InvoicePaymentResponseDto.builder()
                .id(entity.getId())
                .invoiceId(entity.getInvoice() != null ? entity.getInvoice().getId() : null)
                .paymentDate(entity.getPaymentDate())
                .amountCents(entity.getAmountCents())
                .paymentMethod(entity.getPaymentMethod())
                .referenceNo(entity.getReferenceNo())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }
    public InvoicePaymentEntity toEntity(InvoicePaymentCreateRequestDto request) {
        if (request == null) return null;
        return InvoicePaymentEntity.builder()
                .invoice(invoiceRef(request.getInvoiceId()))
                .paymentDate(request.getPaymentDate())
                .amountCents(request.getAmountCents())
                .paymentMethod(request.getPaymentMethod())
                .referenceNo(request.getReferenceNo())
                .build();
    }
    public void updateEntity(InvoicePaymentEntity entity, InvoicePaymentUpdateRequestDto request) {
        if (entity == null || request == null) return;
        entity.setId(request.getId());
        entity.setInvoice(invoiceRef(request.getInvoiceId()));
        entity.setPaymentDate(request.getPaymentDate());
        entity.setAmountCents(request.getAmountCents());
        entity.setPaymentMethod(request.getPaymentMethod());
        entity.setReferenceNo(request.getReferenceNo());
    }
    private InvoiceEntity invoiceRef(String id) {
        if (id == null || id.isBlank()) return null;
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(id);
        return invoice;
    }
}
