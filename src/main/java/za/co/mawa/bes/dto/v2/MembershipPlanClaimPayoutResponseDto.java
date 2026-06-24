package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.mawa.bes.enums.DependentType;
import za.co.mawa.bes.enums.MembershipClaimType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipPlanClaimPayoutResponseDto {
    private String id;
    private String planId;
    private String planCode;
    private String planName;
    private MembershipClaimType claimType;
    private DependentType dependentType;
    private Long payoutAmountCents;
    private Boolean active;
}
