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
public class BankAccountCreateRequestDto {

    private String objectId;
    private String accountHolder;
    private String bankName;
    private String accountNumber;
    private String accountType;
    private String branchCode;
    private String branchName;
    private Date validFrom;
    private Date validTo;
    private String status;
}
