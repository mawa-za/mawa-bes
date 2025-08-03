package za.co.mawa.bes.fnb.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class CreditTransferTransactionInformation implements Serializable {
    private String endToEndId;
    private Amount amount;
    private Creditor creditor;
    private CreditorAccount creditorAccount;
    private CreditorAgent creditorAgent;
    private String remittanceInformationUnstructured;
    private String remittanceLocationMethod;
    private String remittanceLocationElectronicAddress;
}