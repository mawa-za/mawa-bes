package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.CashupReceiptEntity;
import za.co.mawa.bes.dto.v2.CashupReceiptCreateRequestDto;
import za.co.mawa.bes.dto.v2.CashupReceiptResponseDto;
import za.co.mawa.bes.dto.v2.CashupReceiptUpdateRequestDto;

@Component
public class CashupReceiptMapper {

    public CashupReceiptResponseDto toResponse(CashupReceiptEntity entity) {
        if (entity == null) {
            return null;
        }
        return CashupReceiptResponseDto.builder()
                .id(entity.getId())
                .cashup(entity.getCashup() != null ? entity.getCashup().getId() : null)
                .receiptIdId(entity.getReceiptId())
                .receiptNoId(entity.getReceiptNo() != null ? entity.getReceiptNo().toString() : null)
                .amountCentsId(entity.getAmountCents() != null ? entity.getAmountCents().toString() : null)
                .paymentMethodId(entity.getPaymentMethod())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public CashupReceiptEntity toEntity(CashupReceiptCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return CashupReceiptEntity.builder()
                .receiptId(request.getReceiptIdId())
                .receiptNo(request.getReceiptNoId() != null ? Long.valueOf(request.getReceiptNoId()) : null)
                .amountCents(request.getAmountCentsId() != null ? Long.valueOf(request.getAmountCentsId()) : null)
                .paymentMethod(request.getPaymentMethodId())
                .build();
    }

    public void updateEntity(CashupReceiptEntity entity, CashupReceiptUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setReceiptId(request.getReceiptIdId());
        entity.setReceiptNo(request.getReceiptNoId() != null ? Long.valueOf(request.getReceiptNoId()) : null);
        entity.setAmountCents(request.getAmountCentsId() != null ? Long.valueOf(request.getAmountCentsId()) : null);
        entity.setPaymentMethod(request.getPaymentMethodId());
    }
}
