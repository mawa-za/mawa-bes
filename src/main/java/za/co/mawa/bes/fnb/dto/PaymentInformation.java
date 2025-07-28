package za.co.mawa.bes.fnb.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class PaymentInformation implements Serializable {
    private String paymentInformationId;
    private String paymentInformationMethod;
    private boolean batchBooking;
    private int numberOfTransactions;
    private double controlSum;
    private String paymentTypeInformationServiceLevelCode;
    private String requestedExecutionDate;
    private Debtor debtor;
    private DebtorAccount debtorAccount;
    private DebtorAgent debtorAgent;
    private List<CreditTransferTransactionInformation> creditTransferTransactionInformation;
}