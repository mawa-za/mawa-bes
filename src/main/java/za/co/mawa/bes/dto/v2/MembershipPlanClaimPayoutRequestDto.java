package za.co.mawa.bes.dto.v2;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.ClaimType;
import za.co.mawa.bes.enums.DependentType;
import za.co.mawa.bes.enums.MembershipClaimType;

@Getter
@Setter
public class MembershipPlanClaimPayoutRequestDto {

    @NotNull
    private MembershipClaimType claimType;

    /**
     * Use ANY when payout does not depend on the deceased/dependent type.
     */
    @NotNull
    private DependentType dependentType = DependentType.ANY;

    @NotNull
    @Min(0)
    private Long payoutAmountCents;

    private Boolean active = true;
}
