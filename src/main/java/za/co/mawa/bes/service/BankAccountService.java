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
        try {
            BankAccountEntity bankAccountEntity = new BankAccountEntity();
            bankAccountEntity.setObjectId(bankAccountCreateDto.getObjectId());
            bankAccountEntity.setAccountHolder(bankAccountCreateDto.getAccountHolder());
            bankAccountEntity.setAccountType(bankAccountCreateDto.getAccountType());
            bankAccountEntity.setBankName(bankAccountCreateDto.getBankName());
            bankAccountEntity.setAccountNumber(bankAccountCreateDto.getAccountNumber());
            bankAccountEntity.setBranchCode(getUBC(bankAccountCreateDto.getBankName()));
            bankAccountEntity.setStatus(Status.ACTIVE);
            bankAccountEntity.setValidFrom(new Date());
            bankAccountEntity.setValidTo(new Date());
            bankAccountRepository.save(bankAccountEntity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void edit(BankAccountEditDto bankAccountEditDto) {
        BankAccountEntity bankAccountEntity = bankAccountRepository.getById(bankAccountEditDto.getId());
        bankAccountEntity.setAccountHolder(bankAccountEditDto.getAccountHolder());
        bankAccountEntity.setAccountType(bankAccountEditDto.getAccountType());
        bankAccountEntity.setBankName(bankAccountEditDto.getBankName());
        bankAccountEntity.setBranchCode(getUBC(bankAccountEntity.getBankName()));
        bankAccountEntity.setAccountNumber(bankAccountEditDto.getAccountNumber());
        bankAccountEntity.setBranchCode(bankAccountEditDto.getBankName());
//        bankAccountEntity.setStatus(Status.ACTIVE);
        bankAccountEntity.setValidFrom(new Date());
//        bankAccountEntity.setValidTo(new Date(Constant.END_DATE));
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
        bankAccountDto.setBranchCode(getUBC(bankAccountEntity.getBankName()));
        bankAccountDto.setStatus(fieldOptionService.getFieldOption(Field.STATUS, bankAccountEntity.getStatus()));
        return bankAccountDto;
    }

    public void delete(String id) {
        bankAccountRepository.deleteById(id);
    }

    public String getUBC(String bank) {
        String code = "";
        switch (bank) {
            case "ABSA":
                code = "632005";
                break;
            case "AFRICAN-BANK":
                code = "430000";
                break;
            case "BANK-ZERO":
                code = "888000";
                break;
            case "BIDVEST":
                code = "462005";
                break;
            case "CAPITEC":
                code = "470010";
                break;
            case "DISCOVERY":
                code = "679000";
                break;
            case "FNB":
                code = "250655";
                break;
            case "INVESTEC":
                code = "580105";
                break;
            case "NEDBANK":
                code = "198765";
                break;
            case "POST-BANK":
                code = "460005";
                break;
            case "SBSA":
                code = "051001";
                break;
            case "TYME-BANK":
                code = "678910";
                break;
        }
        return code;
    }
}
