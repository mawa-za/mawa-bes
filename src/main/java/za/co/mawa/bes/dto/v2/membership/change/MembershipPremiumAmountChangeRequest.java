package za.co.mawa.bes.dto.v2.membership.change;

import lombok.Data;

@Data
public class MembershipPremiumAmountChangeRequest {
    private Long premiumCents;
    private String reason;
}
