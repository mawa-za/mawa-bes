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
        return MembershipPlanPremiumRuleResponseDto.builder()
                .id(entity.getId())
                .plan(entity.getPlan() != null ? entity.getPlan().getId() : null)
                .dependentTypeId(entity.getDependentType() != null ? entity.getDependentType().name() : null)
                .minAgeId(entity.getMinAge() != null ? entity.getMinAge().toString() : null)
                .maxAgeId(entity.getMaxAge() != null ? entity.getMaxAge().toString() : null)
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
                .dependentType(request.getDependentTypeId() != null ? za.co.mawa.bes.enums.DependentType.valueOf(request.getDependentTypeId()) : null)
                .minAge(request.getMinAgeId() != null ? Integer.valueOf(request.getMinAgeId()) : null)
                .maxAge(request.getMaxAgeId() != null ? Integer.valueOf(request.getMaxAgeId()) : null)
                .additionalPremiumCents(request.getAdditionalPremiumCents())
                .active(request.getActive())
                .build();
    }

    public void updateEntity(MembershipPlanPremiumRuleEntity entity, MembershipPlanPremiumRuleUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setDependentType(request.getDependentTypeId() != null ? za.co.mawa.bes.enums.DependentType.valueOf(request.getDependentTypeId()) : null);
        entity.setMinAge(request.getMinAgeId() != null ? Integer.valueOf(request.getMinAgeId()) : null);
        entity.setMaxAge(request.getMaxAgeId() != null ? Integer.valueOf(request.getMaxAgeId()) : null);
        entity.setAdditionalPremiumCents(request.getAdditionalPremiumCents());
        entity.setActive(request.getActive());
    }
}
