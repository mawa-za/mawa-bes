package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.utils.DateTime;

import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
public class BankFileXmlDto {
    private String creationDateTime;
    private String messageIdentification;
    private Integer numberOfTransactions;
    private BigDecimal controlSum;
    private String name;
    private String paymentInformationIdentification;
    private String paymentMethod;
    private Boolean batchBooking;
    private String code;
    private String requestedExecutionDate;
    private String id;
    private String number;
    private String proprietary;
    private String memberId;
    private String endToEndIdentification;
    private BigDecimal amount;
    private BigDecimal instructedAmount;
    private String notes;
    private String unStructured;
    private String paymentReason;
    private String paymentReference;
    private BankAccountDto bankAccount;

}
