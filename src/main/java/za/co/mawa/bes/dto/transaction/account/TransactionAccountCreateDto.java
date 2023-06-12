package za.co.mawa.bes.dto.transaction.account;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TransactionAccountCreateDto implements Serializable{
    private String accountHolder;
    private String bankName;
    private String accountNumber;
    private String branchCode;
    private String accountType;
}
