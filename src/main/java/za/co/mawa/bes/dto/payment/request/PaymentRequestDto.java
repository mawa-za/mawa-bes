package za.co.mawa.bes.dto.payment.request;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.BankAccountDto;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
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
    private String number;
    private String batchNumber;
    private FieldOptionDto status;
    private FieldOptionDto statusReason;
    private FieldOptionDto paymentReason;
    private String reference;
    private String description;
    private BigDecimal amount;
    private Date dueDate;
    private Date createdDate;
    private FieldOptionDto type;
    private FieldOptionDto paymentMethod;
    private FieldOptionDto branch;
    private PartnerDto createdBy;
    private PartnerDto changedBy;
    private PartnerDto recipient;
    private PartnerDto employeeResponsible;
    private BankAccountDto bankAccount;

}
