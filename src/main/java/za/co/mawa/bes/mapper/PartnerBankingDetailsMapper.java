package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerBankingDetailsEntity;
import za.co.mawa.bes.dto.PartnerBankingDetailsCreateRequestDto;
import za.co.mawa.bes.dto.PartnerBankingDetailsResponseDto;
import za.co.mawa.bes.dto.PartnerBankingDetailsUpdateRequestDto;

@Component
public class PartnerBankingDetailsMapper {

    public PartnerBankingDetailsResponseDto toResponse(PartnerBankingDetailsEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerBankingDetailsResponseDto.builder()
                .id(entity.getId())
                .partner(entity.getPartner())
                .accountHolder(entity.getAccountHolder())
                .accountNumber(entity.getAccountNumber())
                .accountType(entity.getAccountType())
                .bankName(entity.getBankName())
                .branchCode(entity.getBranchCode())
                .branchName(entity.getBranchName())
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .status(entity.getStatus())
                .build();
    }

    public PartnerBankingDetailsEntity toEntity(PartnerBankingDetailsCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerBankingDetailsEntity.builder()
                .partner(request.getPartner())
                .accountHolder(request.getAccountHolder())
                .accountNumber(request.getAccountNumber())
                .accountType(request.getAccountType())
                .bankName(request.getBankName())
                .branchCode(request.getBranchCode())
                .branchName(request.getBranchName())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .status(request.getStatus())
                .build();
    }

    public void updateEntity(PartnerBankingDetailsEntity entity, PartnerBankingDetailsUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setPartner(request.getPartner());
        entity.setAccountHolder(request.getAccountHolder());
        entity.setAccountNumber(request.getAccountNumber());
        entity.setAccountType(request.getAccountType());
        entity.setBankName(request.getBankName());
        entity.setBranchCode(request.getBranchCode());
        entity.setBranchName(request.getBranchName());
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
        entity.setStatus(request.getStatus());
    }
}
