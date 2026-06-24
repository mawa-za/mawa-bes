package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashupPaymentSummaryCreateRequestDto {
    private String cashupId;
    private String paymentMethod;
    private Long amountCents;
    private Integer paymentCount;
}
