package za.co.mawa.bes.dto.payment.request;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.transaction.account.TransactionAccountDto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PaymentRequestDto implements Serializable{
    private String id;
    private String status;
    private String statusReason;
    private String paymentReason;
    private String reference;
    private BigDecimal amount;
    private Date dueDate;
    private String type;
    private String createdBy;
    private String changedBy;
    private String employeeResponsibleId;
    private TransactionAccountDto bankDetails;

}
