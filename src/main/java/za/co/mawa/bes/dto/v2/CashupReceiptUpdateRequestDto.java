package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashupReceiptUpdateRequestDto {

    private String id;
    private String cashup;
    private String receiptIdId;
    private String receiptNoId;
    private String amountCentsId;
    private String paymentMethodId;
}
