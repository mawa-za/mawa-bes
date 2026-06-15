package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerBankAccountEntity;
import za.co.mawa.bes.dto.PartnerBankAccountCreateRequestDto;
import za.co.mawa.bes.dto.PartnerBankAccountResponseDto;
import za.co.mawa.bes.dto.PartnerBankAccountUpdateRequestDto;

@Component
public class PartnerBankAccountMapper {

    public PartnerBankAccountResponseDto toResponse(PartnerBankAccountEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerBankAccountResponseDto.builder()
                .id(entity.getId())
                .partner(entity.getPartner())
                .accountHolder(entity.getAccountHolder())
                .accountType(entity.getAccountType())
                .bankName(entity.getBankName())
                .branchCode(entity.getBranchCode())
                .branchName(entity.getBranchName())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .status(entity.getStatus())
                .accountNumber(entity.getAccountNumber())
                .build();
    }

    public PartnerBankAccountEntity toEntity(PartnerBankAccountCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerBankAccountEntity.builder()
                .partner(request.getPartner())
                .accountHolder(request.getAccountHolder())
                .accountType(request.getAccountType())
                .bankName(request.getBankName())
                .branchCode(request.getBranchCode())
                .branchName(request.getBranchName())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .status(request.getStatus())
                .accountNumber(request.getAccountNumber())
                .build();
    }

    public void updateEntity(PartnerBankAccountEntity entity, PartnerBankAccountUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setPartner(request.getPartner());
        entity.setAccountHolder(request.getAccountHolder());
        entity.setAccountType(request.getAccountType());
        entity.setBankName(request.getBankName());
        entity.setBranchCode(request.getBranchCode());
        entity.setBranchName(request.getBranchName());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
        entity.setStatus(request.getStatus());
        entity.setAccountNumber(request.getAccountNumber());
    }
}
