package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class PartnerBankAccountDto implements Serializable {
    private String partner;
    private String accountHolder;
    private String accountNumber;
    private String accountType;
    private String bankName;
    private String branchCode;
    private String branchName;
    private String validFrom;
    private String validTo;
    private String type;
    private String status;
    private String id;




}
