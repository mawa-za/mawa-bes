package za.co.mawa.bes.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerBankAccountUpdateRequestDto {

    private String id;
    private String partner;
    private String accountHolder;
    private String accountType;
    private String bankName;
    private String branchCode;
    private String branchName;
    private Date validFrom;
    private Date validTo;
    private String status;
    private String accountNumber;
}
