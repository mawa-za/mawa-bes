package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.BankAccountCreateDto;
import za.co.mawa.bes.dto.BankAccountDto;
import za.co.mawa.bes.dto.transaction.bank.account.TransactionBankAccountDto;
import za.co.mawa.bes.entity.BankAccountEntity;
import za.co.mawa.bes.entity.transaction.TransactionBankAccount;
import za.co.mawa.bes.repository.BankAccountRepository;
import za.co.mawa.bes.repository.TransactionBankAccountRepository;
import za.co.mawa.bes.utils.Constant;
import za.co.mawa.bes.utils.Field;
import za.co.mawa.bes.utils.Status;

import java.util.Date;

@Service
public class BankAccountService {
    @Autowired
    BankAccountRepository bankAccountRepository;
    @Autowired
    FieldOptionService fieldOptionService;
    public void save(BankAccountCreateDto bankAccountCreateDto){
        BankAccountEntity bankAccountEntity = new BankAccountEntity();
        bankAccountEntity.setObjectId(bankAccountCreateDto.getObjectId());
        bankAccountEntity.setAccountHolder(bankAccountCreateDto.getAccountHolder());
        bankAccountEntity.setAccountType(bankAccountCreateDto.getAccountType());
        bankAccountEntity.setBankName(bankAccountCreateDto.getBankName());
        bankAccountEntity.setAccountNumber(bankAccountCreateDto.getAccountNumber());
        bankAccountEntity.setBranchCode(bankAccountCreateDto.getBranchCode());
        bankAccountEntity.setStatus(Status.ACTIVE);
        bankAccountEntity.setValidFrom(new Date());
        bankAccountEntity.setValidTo(new Date(Constant.END_DATE));
        bankAccountRepository.save(bankAccountEntity);
    }
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

    public void delete(String id) {
        bankAccountRepository.deleteById(id);
    }
}
