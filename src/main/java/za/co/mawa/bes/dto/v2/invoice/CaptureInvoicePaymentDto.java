package za.co.mawa.bes.dto.v2.invoice;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CaptureInvoicePaymentDto {
    private Long amountCents;
    private LocalDate paymentDate;
    private String paymentMethod; // CASH, EFT, CARD, OTHER
    private String reference;
    private String notes;
    private String createdBy;
    private String deviceId;
    private String terminalId;
    private String location;
    private String employeeResponsible;
}
