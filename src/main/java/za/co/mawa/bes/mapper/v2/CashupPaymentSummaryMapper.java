package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.v2.CashupPaymentSummaryCreateRequestDto;
import za.co.mawa.bes.dto.v2.CashupPaymentSummaryResponseDto;
import za.co.mawa.bes.dto.v2.CashupPaymentSummaryUpdateRequestDto;
import za.co.mawa.bes.entity.v2.CashupEntity;
import za.co.mawa.bes.entity.v2.CashupPaymentSummaryEntity;

@Component
public class CashupPaymentSummaryMapper {
    public CashupPaymentSummaryResponseDto toResponse(CashupPaymentSummaryEntity entity) {
        if (entity == null) return null;
        return CashupPaymentSummaryResponseDto.builder()
                .id(entity.getId())
                .cashupId(entity.getCashup() != null ? entity.getCashup().getId() : null)
                .paymentMethod(entity.getPaymentMethod())
                .amountCents(entity.getAmountCents())
                .paymentCount(entity.getPaymentCount())
                .createdAt(entity.getCreatedAt())
                .build();
    }
    public CashupPaymentSummaryEntity toEntity(CashupPaymentSummaryCreateRequestDto request) {
        if (request == null) return null;
        return CashupPaymentSummaryEntity.builder()
                .cashup(cashupRef(request.getCashupId()))
                .paymentMethod(request.getPaymentMethod())
                .amountCents(request.getAmountCents())
                .paymentCount(request.getPaymentCount())
                .build();
    }
    public void updateEntity(CashupPaymentSummaryEntity entity, CashupPaymentSummaryUpdateRequestDto request) {
        if (entity == null || request == null) return;
        entity.setId(request.getId());
        entity.setCashup(cashupRef(request.getCashupId()));
        entity.setPaymentMethod(request.getPaymentMethod());
        entity.setAmountCents(request.getAmountCents());
        entity.setPaymentCount(request.getPaymentCount());
    }
    private CashupEntity cashupRef(String id) {
        if (id == null || id.isBlank()) return null;
        CashupEntity cashup = new CashupEntity();
        cashup.setId(id);
        return cashup;
    }
}
