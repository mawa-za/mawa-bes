package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePaymentUpdateRequestDto {

    private String id;
    private String invoice;
    private String paymentDateId;
    private String amountCentsId;
    private String paymentMethodId;
    private String referenceNoId;
}
