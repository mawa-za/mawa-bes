package za.co.mawa.bes.dto.v2;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashupPaymentSummaryResponseDto {
    private String id;
    private String cashupId;
    private String paymentMethod;
    private Long amountCents;
    private Integer paymentCount;
    private LocalDateTime createdAt;
}
