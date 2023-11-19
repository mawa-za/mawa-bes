package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PartnerBankAccountDto {
    private String id;
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

}
