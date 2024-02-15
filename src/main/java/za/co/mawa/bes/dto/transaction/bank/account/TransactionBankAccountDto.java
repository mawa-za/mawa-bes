package za.co.mawa.bes.dto.transaction.bank.account;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TransactionBankAccountDto {
    private String accountHolder;
    private FieldOptionDto bankName;
    private String accountNumber;
    private String branchCode;
    private FieldOptionDto accountType;
}
