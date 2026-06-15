package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
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
    private String cashup;
    private String paymentMethodId;
    private String amountCentsId;
    private String paymentCountId;
    private LocalDateTime createdAt;
}
