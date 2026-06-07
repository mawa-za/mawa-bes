package za.co.mawa.bes.dto.v2.funeral;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CaptureInvoicePaymentDto {
    private Long amountCents;
    private String paymentMethod; // CASH, EFT, CARD
    private String reference;
}
