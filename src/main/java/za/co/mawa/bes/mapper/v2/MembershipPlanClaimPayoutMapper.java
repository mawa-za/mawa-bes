package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.MembershipPlanClaimPayoutEntity;
import za.co.mawa.bes.dto.v2.MembershipPlanClaimPayoutCreateRequestDto;
import za.co.mawa.bes.dto.v2.MembershipPlanClaimPayoutResponseDto;
import za.co.mawa.bes.dto.v2.MembershipPlanClaimPayoutUpdateRequestDto;

@Component
public class MembershipPlanClaimPayoutMapper {

    public MembershipPlanClaimPayoutResponseDto toResponse(MembershipPlanClaimPayoutEntity entity) {
        if (entity == null) {
            return null;
        }
        // TODO: map relation field `claimType` to `claimTypeId` once the related entity id getter is confirmed.
        // TODO: map relation field `dependentType` to `dependentTypeId` once the related entity id getter is confirmed.
        return MembershipPlanClaimPayoutResponseDto.builder()
                .id(entity.getId())
                .plan(entity.getPlan())
                .payoutAmountCents(entity.getPayoutAmountCents())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public MembershipPlanClaimPayoutEntity toEntity(MembershipPlanClaimPayoutCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return MembershipPlanClaimPayoutEntity.builder()
                .plan(request.getPlan())
                .payoutAmountCents(request.getPayoutAmountCents())
                .active(request.getActive())
                .build();
    }

    public void updateEntity(MembershipPlanClaimPayoutEntity entity, MembershipPlanClaimPayoutUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setPlan(request.getPlan());
        entity.setPayoutAmountCents(request.getPayoutAmountCents());
        entity.setActive(request.getActive());
    }
}
