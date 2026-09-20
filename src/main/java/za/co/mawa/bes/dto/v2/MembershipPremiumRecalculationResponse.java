package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MembershipPremiumRecalculationResponse {
    private String membershipId;
    private int premiumsChecked;
    private int premiumsGenerated;
    private int premiumsCorrected;
    private int premiumsRemoved;
    private String paidUpToPeriod;
}
