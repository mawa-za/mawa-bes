package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.BankAccountEntity;
import za.co.mawa.bes.dto.BankAccountCreateRequestDto;
import za.co.mawa.bes.dto.BankAccountResponseDto;
import za.co.mawa.bes.dto.BankAccountUpdateRequestDto;

@Component
public class BankAccountMapper {

    public BankAccountResponseDto toResponse(BankAccountEntity entity) {
        if (entity == null) {
            return null;
        }

        return BankAccountResponseDto.builder()
                .id(entity.getId())
                .objectId(entity.getObjectId())
                .accountHolder(entity.getAccountHolder())
                .bankName(entity.getBankName())
                .accountNumber(entity.getAccountNumber())
                .accountType(entity.getAccountType())
                .branchCode(entity.getBranchCode())
                .branchName(entity.getBranchName())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .status(entity.getStatus())
                .build();
    }

    public BankAccountEntity toEntity(BankAccountCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return BankAccountEntity.builder()
                .objectId(request.getObjectId())
                .accountHolder(request.getAccountHolder())
                .bankName(request.getBankName())
                .accountNumber(request.getAccountNumber())
                .accountType(request.getAccountType())
                .branchCode(request.getBranchCode())
                .branchName(request.getBranchName())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .status(request.getStatus())
                .build();
    }

    public void updateEntity(BankAccountEntity entity, BankAccountUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setObjectId(request.getObjectId());
        entity.setAccountHolder(request.getAccountHolder());
        entity.setBankName(request.getBankName());
        entity.setAccountNumber(request.getAccountNumber());
        entity.setAccountType(request.getAccountType());
        entity.setBranchCode(request.getBranchCode());
        entity.setBranchName(request.getBranchName());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
        entity.setStatus(request.getStatus());
    }
}
