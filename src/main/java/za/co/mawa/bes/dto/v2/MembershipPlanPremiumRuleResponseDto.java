package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;
import za.co.mawa.bes.enums.DependentType;

@Getter
@Builder
public class MembershipPlanPremiumRuleResponseDto {

    private String id;

    private String planId;

    private String planName;

    private DependentType dependentType;

    private Integer minAge;

    private Integer maxAge;

    private Long additionalPremiumCents;

    private Boolean active;
}