package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
@NoArgsConstructor
@Getter
@Setter
public class PartnerBankAccountEditDto implements Serializable
{

    private String accountHolder;
    private String accountType;
    private String bankName;
    private String branchCode;
    private String branchName;

    private String validFrom;
    private String validTo;
    private String status;
}
