package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PartnerBankingDetailsPKEntity;
import za.co.mawa.bes.dto.PartnerBankingDetailsPKCreateRequestDto;
import za.co.mawa.bes.dto.PartnerBankingDetailsPKResponseDto;
import za.co.mawa.bes.dto.PartnerBankingDetailsPKUpdateRequestDto;

@Component
public class PartnerBankingDetailsPKMapper {

    public PartnerBankingDetailsPKResponseDto toResponse(PartnerBankingDetailsPKEntity entity) {
        if (entity == null) {
            return null;
        }

        return PartnerBankingDetailsPKResponseDto.builder()
                .partner(entity.getPartner())
                .type(entity.getType())
                .accountNumber(entity.getAccountNumber())
                .build();
    }

    public PartnerBankingDetailsPKEntity toEntity(PartnerBankingDetailsPKCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PartnerBankingDetailsPKEntity.builder()
                .partner(request.getPartner())
                .type(request.getType())
                .accountNumber(request.getAccountNumber())
                .build();
    }

    public void updateEntity(PartnerBankingDetailsPKEntity entity, PartnerBankingDetailsPKUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setPartner(request.getPartner());
        entity.setType(request.getType());
        entity.setAccountNumber(request.getAccountNumber());
    }
}
