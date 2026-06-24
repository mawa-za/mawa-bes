package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.MembershipPremiumEntity;
import za.co.mawa.bes.dto.v2.MembershipPremiumCreateRequestDto;
import za.co.mawa.bes.dto.v2.MembershipPremiumResponseDto;
import za.co.mawa.bes.dto.v2.MembershipPremiumUpdateRequestDto;

@Component
public class MembershipPremiumMapper {

    public MembershipPremiumResponseDto toResponse(MembershipPremiumEntity entity) {
        if (entity == null) {
            return null;
        }

        return MembershipPremiumResponseDto.builder()
                .id(entity.getId())
                .membershipId(entity.getMembershipId())
                .periodYYYYMM(entity.getPeriodYYYYMM())
                .amountCents(entity.getAmountCents())
                .paidAmountCents(entity.getPaidAmountCents())
                .balanceCents(entity.getBalanceCents())
                .status(entity.getStatus())
                .dueDate(entity.getDueDate())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public MembershipPremiumEntity toEntity(MembershipPremiumCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return MembershipPremiumEntity.builder()
                .membershipId(request.getMembershipId())
                .periodYYYYMM(request.getPeriodYYYYMM())
                .amountCents(request.getAmountCents())
                .paidAmountCents(request.getPaidAmountCents())
                .balanceCents(request.getBalanceCents())
                .status(request.getStatus())
                .dueDate(request.getDueDate())
                .build();
    }

    public void updateEntity(MembershipPremiumEntity entity, MembershipPremiumUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setMembershipId(request.getMembershipId());
        entity.setPeriodYYYYMM(request.getPeriodYYYYMM());
        entity.setAmountCents(request.getAmountCents());
        entity.setPaidAmountCents(request.getPaidAmountCents());
        entity.setBalanceCents(request.getBalanceCents());
        entity.setStatus(request.getStatus());
        entity.setDueDate(request.getDueDate());
    }
}
