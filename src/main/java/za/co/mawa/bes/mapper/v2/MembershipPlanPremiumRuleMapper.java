package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.v2.MembershipPlanPremiumRuleCreateRequestDto;
import za.co.mawa.bes.dto.v2.MembershipPlanPremiumRuleResponseDto;
import za.co.mawa.bes.dto.v2.MembershipPlanPremiumRuleUpdateRequestDto;
import za.co.mawa.bes.entity.v2.MembershipPlanPremiumRuleEntity;
import za.co.mawa.bes.enums.DependentType;

@Component
public class MembershipPlanPremiumRuleMapper {

    public MembershipPlanPremiumRuleResponseDto toResponse(MembershipPlanPremiumRuleEntity entity) {
        if (entity == null) {
            return null;
        }

        return MembershipPlanPremiumRuleResponseDto.builder()
                .id(entity.getId())
                .planId(entity.getPlan() != null ? entity.getPlan().getId() : null)
                .planName(entity.getPlan() != null ? entity.getPlan().getName() : null)
                .dependentType(entity.getDependentType())
                .minAge(entity.getMinAge())
                .maxAge(entity.getMaxAge())
                .additionalPremiumCents(entity.getAdditionalPremiumCents())
                .active(entity.getActive())
                .build();
    }

    public MembershipPlanPremiumRuleEntity toEntity(MembershipPlanPremiumRuleCreateRequestDto request) {
        if (request == null) {
            return null;
        }

        MembershipPlanPremiumRuleEntity entity = new MembershipPlanPremiumRuleEntity();
        entity.setDependentType(parseDependentType(request.getDependentTypeId()));
        entity.setMinAge(parseInteger(request.getMinAgeId()));
        entity.setMaxAge(parseInteger(request.getMaxAgeId()));
        entity.setAdditionalPremiumCents(request.getAdditionalPremiumCents());
        entity.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
        return entity;
    }

    public void updateEntity(MembershipPlanPremiumRuleEntity entity, MembershipPlanPremiumRuleUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }

        entity.setDependentType(parseDependentType(request.getDependentTypeId()));
        entity.setMinAge(parseInteger(request.getMinAgeId()));
        entity.setMaxAge(parseInteger(request.getMaxAgeId()));
        entity.setAdditionalPremiumCents(request.getAdditionalPremiumCents());
        entity.setActive(request.getActive());
    }

    private DependentType parseDependentType(String value) {
        return value == null || value.isBlank() ? null : DependentType.valueOf(value);
    }

    private Integer parseInteger(String value) {
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }
}
