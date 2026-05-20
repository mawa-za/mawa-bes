package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.DependentType;

@Getter
@Setter
public class MembershipPlanPremiumRuleRequestDto {

    private DependentType dependentType;

    private Integer minAge;

    private Integer maxAge;

    private Long additionalPremiumCents;

    private Boolean active = true;
}