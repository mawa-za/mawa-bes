package za.co.mawa.bes.dto.payment.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.BankAccountDto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PaymentRequestQueryDto implements Serializable {
    private String recipientId;
    private String recipient;
    private String id;
    private String paymentReason;
    private String reference;
    private BigDecimal amount;
    private Date dueDate;
    private String type;
    private String paymentMethod;
    private String employeeResponsibleId;
    private String branch;
    private String batchNumber;
    private String status;
    private String transactionNumber;
    private Date dateCreated;
}
