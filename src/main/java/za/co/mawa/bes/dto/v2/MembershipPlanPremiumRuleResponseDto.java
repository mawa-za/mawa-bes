package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.mawa.bes.enums.DependentType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
