package za.co.mawa.bes.mapper;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.PremiumEntity;
import za.co.mawa.bes.dto.PremiumCreateRequestDto;
import za.co.mawa.bes.dto.PremiumResponseDto;
import za.co.mawa.bes.dto.PremiumUpdateRequestDto;

@Component
public class PremiumMapper {

    public PremiumResponseDto toResponse(PremiumEntity entity) {
        if (entity == null) {
            return null;
        }

        return PremiumResponseDto.builder()
                .id(entity.getId())
                .receiptNumber(entity.getReceiptNumber())
                .creationDate(entity.getCreationDate())
                .creationTime(entity.getCreationTime())
                .createdBy(entity.getCreatedBy())
                .membershipId(entity.getMembershipId())
                .extReceiptNumber(entity.getExtReceiptNumber())
                .membershipPeriod(entity.getMembershipPeriod())
                .tenderType(entity.getTenderType())
                .location(entity.getLocation())
                .terminalId(entity.getTerminalId())
                .amount(entity.getAmount())
                .build();
    }

    public PremiumEntity toEntity(PremiumCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return PremiumEntity.builder()
                .receiptNumber(request.getReceiptNumber())
                .creationDate(request.getCreationDate())
                .creationTime(request.getCreationTime())
                .membershipId(request.getMembershipId())
                .extReceiptNumber(request.getExtReceiptNumber())
                .membershipPeriod(request.getMembershipPeriod())
                .tenderType(request.getTenderType())
                .location(request.getLocation())
                .terminalId(request.getTerminalId())
                .amount(request.getAmount())
                .build();
    }

    public void updateEntity(PremiumEntity entity, PremiumUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setReceiptNumber(request.getReceiptNumber());
        entity.setCreationDate(request.getCreationDate());
        entity.setCreationTime(request.getCreationTime());
        entity.setMembershipId(request.getMembershipId());
        entity.setExtReceiptNumber(request.getExtReceiptNumber());
        entity.setMembershipPeriod(request.getMembershipPeriod());
        entity.setTenderType(request.getTenderType());
        entity.setLocation(request.getLocation());
        entity.setTerminalId(request.getTerminalId());
        entity.setAmount(request.getAmount());
    }
}
