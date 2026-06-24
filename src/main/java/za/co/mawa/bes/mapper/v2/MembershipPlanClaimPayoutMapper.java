package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.v2.MembershipPlanClaimPayoutCreateRequestDto;
import za.co.mawa.bes.dto.v2.MembershipPlanClaimPayoutResponseDto;
import za.co.mawa.bes.dto.v2.MembershipPlanClaimPayoutUpdateRequestDto;
import za.co.mawa.bes.entity.v2.MembershipPlanClaimPayoutEntity;
import za.co.mawa.bes.enums.DependentType;
import za.co.mawa.bes.enums.MembershipClaimType;

@Component
public class MembershipPlanClaimPayoutMapper {

    public MembershipPlanClaimPayoutResponseDto toResponse(MembershipPlanClaimPayoutEntity entity) {
        if (entity == null) {
            return null;
        }

        return MembershipPlanClaimPayoutResponseDto.builder()
                .id(entity.getId())
                .planId(entity.getPlan() != null ? entity.getPlan().getId() : null)
                .planCode(entity.getPlan() != null ? entity.getPlan().getPlanCode() : null)
                .planName(entity.getPlan() != null ? entity.getPlan().getName() : null)
                .claimType(entity.getClaimType())
                .dependentType(entity.getDependentType())
                .payoutAmountCents(entity.getPayoutAmountCents())
                .active(entity.getActive())
                .build();
    }

    public MembershipPlanClaimPayoutEntity toEntity(MembershipPlanClaimPayoutCreateRequestDto request) {
        if (request == null) {
            return null;
        }

        MembershipPlanClaimPayoutEntity entity = new MembershipPlanClaimPayoutEntity();
        entity.setClaimType(parseMembershipClaimType(request.getClaimTypeId()));
        entity.setDependentType(parseDependentType(request.getDependentTypeId()));
        entity.setPayoutAmountCents(request.getPayoutAmountCents());
        entity.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
        return entity;
    }

    public void updateEntity(MembershipPlanClaimPayoutEntity entity, MembershipPlanClaimPayoutUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }

        entity.setClaimType(parseMembershipClaimType(request.getClaimTypeId()));
        entity.setDependentType(parseDependentType(request.getDependentTypeId()));
        entity.setPayoutAmountCents(request.getPayoutAmountCents());
        entity.setActive(request.getActive());
    }

    private MembershipClaimType parseMembershipClaimType(String value) {
        return value == null || value.isBlank() ? null : MembershipClaimType.valueOf(value);
    }

    private DependentType parseDependentType(String value) {
        return value == null || value.isBlank() ? null : DependentType.valueOf(value);
    }
}
