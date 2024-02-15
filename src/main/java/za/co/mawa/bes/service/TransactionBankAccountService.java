package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.transaction.account.TransactionAccountDto;
import za.co.mawa.bes.dto.transaction.bank.account.TransactionBankAccountDto;
import za.co.mawa.bes.entity.transaction.TransactionBankAccount;
import za.co.mawa.bes.repository.TransactionBankAccountRepository;
import za.co.mawa.bes.utils.Field;

import java.util.Optional;

@Service
public class TransactionBankAccountService {
    @Autowired
    TransactionBankAccountRepository transactionBankAccountRepository;
    @Autowired
    FieldOptionService fieldOptionService;
    public TransactionBankAccountDto get(String id) {
        TransactionBankAccount transactionBankAccount = transactionBankAccountRepository.getById(id);
        TransactionBankAccountDto transactionBankAccountDto = new TransactionBankAccountDto();
        transactionBankAccountDto.setAccountHolder(transactionBankAccount.getAccountHolder());
        transactionBankAccountDto.setAccountNumber(transactionBankAccount.getAccountNumber());
        transactionBankAccountDto.setBankName(fieldOptionService.getFieldOption(Field.BANK_NAME, transactionBankAccount.getBankName()));
        transactionBankAccountDto.setAccountType(fieldOptionService.getFieldOption(Field.BANK_ACCOUNT_TYPE, transactionBankAccount.getAccountType()));
        transactionBankAccountDto.setBranchCode(transactionBankAccount.getBranchCode());
        return transactionBankAccountDto;
    }
}
