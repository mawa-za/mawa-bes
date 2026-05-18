package za.co.mawa.bes.dto.v2.payment;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

@NoArgsConstructor
@Getter
@Setter
public class PaymentRequestUpdateRequest {

    private String payeePartnerId;
    private String payeeName;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private String bankName;
    private String accountHolder;
    private String accountNumber;
    private String branchCode;
    private String accountType;
    private String invoiceNo;
    private String externalReference;
    private String paymentReason;
    private String notes;
    private LocalDate requestedPaymentDate;
    private String paidReference;

}
