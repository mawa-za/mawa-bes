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
        // TODO: map relation field `receiptId` to `receiptIdId` once the related entity id getter is confirmed.
        // TODO: map relation field `receiptNo` to `receiptNoId` once the related entity id getter is confirmed.
        // TODO: map relation field `amountCents` to `amountCentsId` once the related entity id getter is confirmed.
        // TODO: map relation field `paymentMethod` to `paymentMethodId` once the related entity id getter is confirmed.
        return CashupReceiptResponseDto.builder()
                .id(entity.getId())
                .cashup(entity.getCashup())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public CashupReceiptEntity toEntity(CashupReceiptCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return CashupReceiptEntity.builder()
                .cashup(request.getCashup())
                .build();
    }

    public void updateEntity(CashupReceiptEntity entity, CashupReceiptUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setCashup(request.getCashup());
    }
}
