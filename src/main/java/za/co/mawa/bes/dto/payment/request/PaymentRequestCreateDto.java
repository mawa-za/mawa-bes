package za.co.mawa.bes.dto.payment.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.BankAccountCreateDto;
import za.co.mawa.bes.dto.BankAccountDto;
import za.co.mawa.bes.dto.transaction.account.TransactionAccountCreateDto;
import za.co.mawa.bes.dto.transaction.account.TransactionAccountDto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PaymentRequestCreateDto implements Serializable{
    private String recipientId;
    private String paymentReason;
    private String reference;
    private String amount;
    private Date dueDate;
    private String type;
    private String paymentMethod;
    private String employeeResponsibleId;
    private String branch;
    private BankAccountCreateDto bankAccount;
}
