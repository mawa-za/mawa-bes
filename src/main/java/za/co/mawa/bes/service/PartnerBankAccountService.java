package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.PartnerBankAccountEditDto;
import za.co.mawa.bes.dto.PartnerBankAccountGetDto;
import za.co.mawa.bes.dto.partner.PartnerBankAccountDto;
import za.co.mawa.bes.entity.PartnerBankAccountEntity;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.repository.PartnerBankAccountRepository;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.utils.Constant;
import za.co.mawa.bes.utils.Conversion;
import za.co.mawa.bes.utils.Status;
import za.co.mawa.bes.service.v2.ReferenceDataValidationService;
import za.co.mawa.bes.service.v2.UniversalBranchCodeService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class PartnerBankAccountService {
    @Autowired
    PartnerBankAccountRepository partnerBankAccountRepository;
    @Autowired
    PartnerRepository partnerRepository;
    @Autowired
    ReferenceDataValidationService referenceDataValidationService;
    @Autowired
    UniversalBranchCodeService universalBranchCodeService;


    public String addBankAccount(PartnerBankAccountDto partnerBankAccount) {
        String id = "";
        try {


            PartnerBankAccountEntity partnerBankAccountEntity = new PartnerBankAccountEntity();

            partnerBankAccountEntity.setPartner(partnerBankAccount.getPartner());

            if (partnerBankAccount.getAccountHolder() != null) {
                partnerBankAccountEntity.setAccountHolder(partnerBankAccount.getAccountHolder());
            } else {
                PartnerEntity partner = partnerRepository.getById(partnerBankAccount.getPartner());
                if (partner != null) {
                    partnerBankAccountEntity.setAccountHolder(partner.getName1());
                }
            }


            if (partnerBankAccount.getAccountNumber() != null) {
                partnerBankAccountEntity.setAccountNumber(partnerBankAccount.getAccountNumber());
            }


            if (partnerBankAccount.getAccountType() != null) {
                partnerBankAccountEntity.setAccountType(referenceDataValidationService.requireOption(
                        "BANK-ACCOUNT-TYPE", partnerBankAccount.getAccountType(), "Bank account type"));
            }
            if (partnerBankAccount.getBankName() != null) {
                partnerBankAccountEntity.setBankName(referenceDataValidationService.requireOption(
                        "BANK-NAME", partnerBankAccount.getBankName(), "Bank name"));

            }
            partnerBankAccountEntity.setBranchCode(universalBranchCodeService.resolve(partnerBankAccountEntity.getBankName()));
            partnerBankAccountEntity.setBranchName("Universal Branch");

            partnerBankAccountEntity.setValidFrom(new Date());
            if (partnerBankAccount.getValidTo() != null) {
                partnerBankAccountEntity.setValidTo(Conversion.stringToDate(partnerBankAccount.getValidTo()));

            } else {
                partnerBankAccountEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));

            }

            partnerBankAccountEntity.setStatus(Status.ACTIVE);
            partnerBankAccountRepository.save(partnerBankAccountEntity);
            id = partnerBankAccountEntity.getId();

//            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return id;
    }


    public void deleteBankDetails(String id)  {
        boolean delete = false;
        try {
            partnerBankAccountRepository.deleteById(id);
            delete = true;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public PartnerBankAccountGetDto getBankAccounts(String partner) {
        List<PartnerBankAccountEntity> bankDetails = partnerBankAccountRepository.findByPartner(partner);
        ArrayList<PartnerBankAccountDto> bankingDetails = new ArrayList<>();
        PartnerBankAccountGetDto bankAccountGetDto = new PartnerBankAccountGetDto();
        if (!bankDetails.isEmpty()) {
            bankAccountGetDto.setPartner(partner);
            for (PartnerBankAccountEntity bankDetail : bankDetails) {
                PartnerBankAccountDto partnerBankObj = entityBankToDto(bankDetail);

                bankingDetails.add(partnerBankObj);
            }

            bankAccountGetDto.setPartnerBankAccountDtoList(bankingDetails);
        }
        return bankAccountGetDto;
    }


    public ArrayList<PartnerBankAccountDto> searchBankAccounts(PartnerBankAccountDto partnerBankObj) {

        return null;
    }

    private PartnerBankAccountDto entityBankToDto(PartnerBankAccountEntity bankDetail) {
        PartnerBankAccountDto partnerBankAccountDto = new PartnerBankAccountDto();
        partnerBankAccountDto.setBankName(bankDetail.getBankName());
        partnerBankAccountDto.setAccountType(bankDetail.getAccountType());
        partnerBankAccountDto.setStatus(bankDetail.getStatus());
        partnerBankAccountDto.setAccountNumber(bankDetail.getAccountNumber());
        partnerBankAccountDto.setValidTo(Conversion.dateTimeToString(bankDetail.getValidTo()));
        partnerBankAccountDto.setBranchName(bankDetail.getBranchName());
        partnerBankAccountDto.setValidFrom(Conversion.dateTimeToString(bankDetail.getValidFrom()));
        partnerBankAccountDto.setBranchCode(bankDetail.getBranchCode());
        partnerBankAccountDto.setId(bankDetail.getId());
        partnerBankAccountDto.setAccountHolder(bankDetail.getAccountHolder());
        return partnerBankAccountDto;
    }


    public void editBankAccount(PartnerBankAccountEditDto partnerBankAccount, String id) {
        boolean edited = false;
        try {


            PartnerBankAccountEntity partnerBankAccountEntity = partnerBankAccountRepository.getById(id);
            if (partnerBankAccountEntity != null) {
                if (partnerBankAccount.getAccountHolder() != null) {

                    partnerBankAccountEntity.setAccountHolder(partnerBankAccount.getAccountHolder());
                }

                if (partnerBankAccount.getAccountType() != null) {
                    partnerBankAccountEntity.setAccountType(referenceDataValidationService.requireOption(
                            "BANK-ACCOUNT-TYPE", partnerBankAccount.getAccountType(), "Bank account type"));
                }

                if (partnerBankAccount.getBankName() != null) {

                    partnerBankAccountEntity.setBankName(referenceDataValidationService.requireOption(
                            "BANK-NAME", partnerBankAccount.getBankName(), "Bank name"));
                }
                partnerBankAccountEntity.setBranchCode(universalBranchCodeService.resolve(partnerBankAccountEntity.getBankName()));
                partnerBankAccountEntity.setBranchName("Universal Branch");

                if (partnerBankAccount.getValidTo() != null) {

                    partnerBankAccountEntity.setValidTo(Conversion.stringToDate(partnerBankAccount.getValidTo()));

                }
                if (partnerBankAccount.getValidFrom() != null) {
                    partnerBankAccountEntity.setValidFrom(Conversion.stringToDate(partnerBankAccount.getValidFrom()));
                }

                if (partnerBankAccount.getAccountNumber() != null) {
                    partnerBankAccountEntity.setAccountNumber(partnerBankAccount.getAccountNumber());
                }
                if (partnerBankAccount.getPartner() != null) {
                    partnerBankAccountEntity.setPartner(partnerBankAccount.getPartner());
                }


                partnerBankAccountRepository.save(partnerBankAccountEntity);
                edited = true;
            }


        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }


    public PartnerBankAccountEntity getActiveBankAccountEntity(String partnerId) {
        return partnerBankAccountRepository.findByPartnerAndStatus(partnerId, Status.ACTIVE)
                .stream()
                .filter(account -> account.getValidTo() == null || !account.getValidTo().before(new Date()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Employee does not have active approved banking details"));
    }

    public String activateApprovedBankAccount(PartnerBankAccountDto details) {
        if (details == null || details.getPartner() == null || details.getPartner().isBlank()) {
            throw new IllegalArgumentException("Partner is required for banking details");
        }
        for (PartnerBankAccountEntity existing : partnerBankAccountRepository.findByPartnerAndStatus(
                details.getPartner(), Status.ACTIVE)) {
            if (details.getId() == null || !details.getId().equals(existing.getId())) {
                existing.setStatus("INACTIVE");
                existing.setValidTo(new Date());
                partnerBankAccountRepository.save(existing);
            }
        }
        if (details.getId() == null || details.getId().isBlank()) {
            return addBankAccount(details);
        }
        PartnerBankAccountEditDto edit = new PartnerBankAccountEditDto();
        edit.setPartner(details.getPartner());
        edit.setAccountHolder(details.getAccountHolder());
        edit.setAccountNumber(details.getAccountNumber());
        edit.setAccountType(details.getAccountType());
        edit.setBankName(details.getBankName());
        edit.setBranchCode(universalBranchCodeService.resolve(details.getBankName()));
        edit.setBranchName("Universal Branch");
        edit.setValidFrom(details.getValidFrom());
        edit.setValidTo(details.getValidTo());
        edit.setStatus(Status.ACTIVE);
        editBankAccount(edit, details.getId());
        PartnerBankAccountEntity saved = partnerBankAccountRepository.getById(details.getId());
        saved.setStatus(Status.ACTIVE);
        saved.setBranchCode(universalBranchCodeService.resolve(saved.getBankName()));
        saved.setBranchName("Universal Branch");
        partnerBankAccountRepository.save(saved);
        return saved.getId();
    }


    public PartnerBankAccountDto getBankAccount(PartnerBankAccountDto bankAccount) {
        PartnerBankAccountDto bankAccountObj = new PartnerBankAccountDto();


        return bankAccountObj;
    }

}
