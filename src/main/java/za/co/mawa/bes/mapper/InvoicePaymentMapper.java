package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.InvoicePaymentEntity;
import za.co.mawa.bes.dto.InvoicePaymentCreateRequestDto;
import za.co.mawa.bes.dto.InvoicePaymentResponseDto;
import za.co.mawa.bes.dto.InvoicePaymentUpdateRequestDto;

@Component
public class InvoicePaymentMapper {

    public InvoicePaymentResponseDto toResponse(InvoicePaymentEntity entity) {
        if (entity == null) {
            return null;
        }
        // TODO: map relation field `paymentDate` to `paymentDateId` once the related entity id getter is confirmed.
        // TODO: map relation field `amountCents` to `amountCentsId` once the related entity id getter is confirmed.
        // TODO: map relation field `paymentMethod` to `paymentMethodId` once the related entity id getter is confirmed.
        // TODO: map relation field `referenceNo` to `referenceNoId` once the related entity id getter is confirmed.
        return InvoicePaymentResponseDto.builder()
                .id(entity.getId())
                .invoice(entity.getInvoice())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    public InvoicePaymentEntity toEntity(InvoicePaymentCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return InvoicePaymentEntity.builder()
                .invoice(request.getInvoice())
                .build();
    }

    public void updateEntity(InvoicePaymentEntity entity, InvoicePaymentUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setInvoice(request.getInvoice());
    }
}
