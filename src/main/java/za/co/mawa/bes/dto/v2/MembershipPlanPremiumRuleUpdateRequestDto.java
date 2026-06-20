package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipPlanPremiumRuleUpdateRequestDto {

    private String id;
    private String plan;
    private String dependentTypeId;
    private String minAgeId;
    private String maxAgeId;
    private Long additionalPremiumCents;
    private Boolean active;
}
