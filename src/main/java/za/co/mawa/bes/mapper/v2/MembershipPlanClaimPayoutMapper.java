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
        return MembershipPlanClaimPayoutResponseDto.builder()
                .id(entity.getId())
                .plan(entity.getPlan() != null ? entity.getPlan().getId() : null)
                .claimTypeId(entity.getClaimType() != null ? entity.getClaimType().name() : null)
                .dependentTypeId(entity.getDependentType() != null ? entity.getDependentType().name() : null)
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
                .claimType(request.getClaimTypeId() != null ? za.co.mawa.bes.enums.MembershipClaimType.valueOf(request.getClaimTypeId()) : null)
                .dependentType(request.getDependentTypeId() != null ? za.co.mawa.bes.enums.DependentType.valueOf(request.getDependentTypeId()) : null)
                .payoutAmountCents(request.getPayoutAmountCents())
                .active(request.getActive())
                .build();
    }

    public void updateEntity(MembershipPlanClaimPayoutEntity entity, MembershipPlanClaimPayoutUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setClaimType(request.getClaimTypeId() != null ? za.co.mawa.bes.enums.MembershipClaimType.valueOf(request.getClaimTypeId()) : null);
        entity.setDependentType(request.getDependentTypeId() != null ? za.co.mawa.bes.enums.DependentType.valueOf(request.getDependentTypeId()) : null);
        entity.setPayoutAmountCents(request.getPayoutAmountCents());
        entity.setActive(request.getActive());
    }
}
