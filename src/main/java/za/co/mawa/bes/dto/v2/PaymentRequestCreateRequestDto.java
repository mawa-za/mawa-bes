package za.co.mawa.bes.dto.v2;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import za.co.mawa.bes.enums.PaymentMethod;
import za.co.mawa.bes.enums.PaymentRequestSourceType;
import za.co.mawa.bes.enums.PaymentRequestStatus;
import za.co.mawa.bes.enums.PaymentRequestType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestCreateRequestDto {

    private String requestNo;
    private PaymentRequestType requestType;
    private PaymentRequestSourceType sourceType;
    private String sourceId;
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
    private PaymentRequestStatus status;
    private String approvalRequestId;
    private LocalDate paidDate;
    private String paidReference;
    private String paidBy;
    private String approvedBy;
    private Date approvedAt;
}
