package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.BankAccountCreateDto;
import za.co.mawa.bes.dto.BankAccountDto;
import za.co.mawa.bes.dto.BankAccountEditDto;
import za.co.mawa.bes.dto.transaction.bank.account.TransactionBankAccountDto;
import za.co.mawa.bes.entity.BankAccountEntity;
import za.co.mawa.bes.entity.transaction.TransactionBankAccount;
import za.co.mawa.bes.repository.BankAccountRepository;
import za.co.mawa.bes.repository.TransactionBankAccountRepository;
import za.co.mawa.bes.utils.Constant;
import za.co.mawa.bes.utils.Field;
import za.co.mawa.bes.utils.Status;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class BankAccountService {
    @Autowired
    BankAccountRepository bankAccountRepository;
    @Autowired
    FieldOptionService fieldOptionService;

    public void add(BankAccountCreateDto bankAccountCreateDto) {
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
    public void edit(BankAccountEditDto bankAccountEditDto) {
        BankAccountEntity bankAccountEntity = bankAccountRepository.getById(bankAccountEditDto.getId());
        bankAccountEntity.setAccountHolder(bankAccountEditDto.getAccountHolder());
        bankAccountEntity.setAccountType(bankAccountEditDto.getAccountType());
        bankAccountEntity.setBankName(bankAccountEditDto.getBankName());
        bankAccountEntity.setAccountNumber(bankAccountEditDto.getAccountNumber());
        bankAccountEntity.setBranchCode(bankAccountEditDto.getBranchCode());
//        bankAccountEntity.setStatus(Status.ACTIVE);
        bankAccountEntity.setValidFrom(new Date());
        bankAccountEntity.setValidTo(new Date(Constant.END_DATE));
        bankAccountRepository.save(bankAccountEntity);
    }
    public List<BankAccountDto> getList(String objectId) {
        List<BankAccountDto> bankAccountDtoList = new ArrayList<>();
        List<BankAccountEntity> bankAccountEntityList = bankAccountRepository.getByObjectId(objectId);
        for (BankAccountEntity bankAccountEntity : bankAccountEntityList) {
            bankAccountDtoList.add(get(bankAccountEntity.getId()));
        }
        return bankAccountDtoList;
    }

    public BankAccountDto get(String id) {
        BankAccountEntity bankAccountEntity = bankAccountRepository.getById(id);
        BankAccountDto bankAccountDto = new BankAccountDto();
        bankAccountDto.setId(bankAccountEntity.getId());
        bankAccountDto.setAccountHolder(bankAccountEntity.getAccountHolder());
        bankAccountDto.setAccountNumber(bankAccountEntity.getAccountNumber());
        bankAccountDto.setBankName(fieldOptionService.getFieldOption(Field.BANK_NAME, bankAccountEntity.getBankName()));
        bankAccountDto.setAccountType(fieldOptionService.getFieldOption(Field.BANK_ACCOUNT_TYPE, bankAccountEntity.getAccountType()));
        bankAccountDto.setBranchCode(bankAccountEntity.getBranchCode());
        bankAccountDto.setStatus(fieldOptionService.getFieldOption(Field.STATUS, bankAccountEntity.getStatus()));
        return bankAccountDto;
    }

    public void delete(String id) {
        bankAccountRepository.deleteById(id);
    }
}
