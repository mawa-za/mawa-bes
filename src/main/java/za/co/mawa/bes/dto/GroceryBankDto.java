package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class GroceryBankDto implements Serializable {
    private String accountHolder;
    private String accountNumber;
    private String accountType;
    private String accountBranch;
    private String accountBranchCode;
}
