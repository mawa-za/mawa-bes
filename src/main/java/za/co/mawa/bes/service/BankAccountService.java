package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.BankAccountDto;
import za.co.mawa.bes.dto.transaction.bank.account.TransactionBankAccountDto;
import za.co.mawa.bes.entity.BankAccountEntity;
import za.co.mawa.bes.entity.transaction.TransactionBankAccount;
import za.co.mawa.bes.repository.BankAccountRepository;
import za.co.mawa.bes.repository.TransactionBankAccountRepository;
import za.co.mawa.bes.utils.Field;

@Service
public class BankAccountService {
    @Autowired
    BankAccountRepository bankAccountRepository;
    @Autowired
    FieldOptionService fieldOptionService;
    public BankAccountDto get(String id) {
        BankAccountEntity bankAccountEntity = bankAccountRepository.getByObjectId(id);
        BankAccountDto bankAccountDto = new BankAccountDto();
        bankAccountDto.setAccountHolder(bankAccountEntity.getAccountHolder());
        bankAccountDto.setAccountNumber(bankAccountEntity.getAccountNumber());
        bankAccountDto.setBankName(fieldOptionService.getFieldOption(Field.BANK_NAME, bankAccountEntity.getBankName()));
        bankAccountDto.setAccountType(fieldOptionService.getFieldOption(Field.BANK_ACCOUNT_TYPE, bankAccountEntity.getAccountType()));
        bankAccountDto.setBranchCode(bankAccountEntity.getBranchCode());
        return bankAccountDto;
    }
}
