package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;
import za.co.mawa.bes.enums.ClaimType;
import za.co.mawa.bes.enums.DependentType;
import za.co.mawa.bes.enums.MembershipClaimType;

@Getter
@Builder
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
