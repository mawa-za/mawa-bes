package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.CashupPaymentSummaryEntity;
import za.co.mawa.bes.dto.v2.CashupPaymentSummaryCreateRequestDto;
import za.co.mawa.bes.dto.v2.CashupPaymentSummaryResponseDto;
import za.co.mawa.bes.dto.v2.CashupPaymentSummaryUpdateRequestDto;

@Component
public class CashupPaymentSummaryMapper {

    public CashupPaymentSummaryResponseDto toResponse(CashupPaymentSummaryEntity entity) {
        if (entity == null) {
            return null;
        }
        // TODO: map relation field `paymentMethod` to `paymentMethodId` once the related entity id getter is confirmed.
        // TODO: map relation field `amountCents` to `amountCentsId` once the related entity id getter is confirmed.
        // TODO: map relation field `paymentCount` to `paymentCountId` once the related entity id getter is confirmed.
        return CashupPaymentSummaryResponseDto.builder()
                .id(entity.getId())
                .cashup(entity.getCashup())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public CashupPaymentSummaryEntity toEntity(CashupPaymentSummaryCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return CashupPaymentSummaryEntity.builder()
                .cashup(request.getCashup())
                .build();
    }

    public void updateEntity(CashupPaymentSummaryEntity entity, CashupPaymentSummaryUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setCashup(request.getCashup());
    }
}
