package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.MembershipPlanPremiumRuleEntity;
import za.co.mawa.bes.dto.v2.MembershipPlanPremiumRuleCreateRequestDto;
import za.co.mawa.bes.dto.v2.MembershipPlanPremiumRuleResponseDto;
import za.co.mawa.bes.dto.v2.MembershipPlanPremiumRuleUpdateRequestDto;

@Component
public class MembershipPlanPremiumRuleMapper {

    public MembershipPlanPremiumRuleResponseDto toResponse(MembershipPlanPremiumRuleEntity entity) {
        if (entity == null) {
            return null;
        }
        // TODO: map relation field `dependentType` to `dependentTypeId` once the related entity id getter is confirmed.
        // TODO: map relation field `minAge` to `minAgeId` once the related entity id getter is confirmed.
        // TODO: map relation field `maxAge` to `maxAgeId` once the related entity id getter is confirmed.
        return MembershipPlanPremiumRuleResponseDto.builder()
                .id(entity.getId())
                .plan(entity.getPlan())
                .additionalPremiumCents(entity.getAdditionalPremiumCents())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public MembershipPlanPremiumRuleEntity toEntity(MembershipPlanPremiumRuleCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return MembershipPlanPremiumRuleEntity.builder()
                .plan(request.getPlan())
                .additionalPremiumCents(request.getAdditionalPremiumCents())
                .active(request.getActive())
                .build();
    }

    public void updateEntity(MembershipPlanPremiumRuleEntity entity, MembershipPlanPremiumRuleUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setPlan(request.getPlan());
        entity.setAdditionalPremiumCents(request.getAdditionalPremiumCents());
        entity.setActive(request.getActive());
    }
}
