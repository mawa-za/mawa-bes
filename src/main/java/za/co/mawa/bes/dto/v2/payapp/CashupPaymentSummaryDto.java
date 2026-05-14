package za.co.mawa.bes.dto.v2.payapp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CashupPaymentSummaryDto {

    private String paymentMethod;
    private Long amountCents;
    private Integer paymentCount;
}
