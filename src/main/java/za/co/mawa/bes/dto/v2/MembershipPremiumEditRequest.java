package za.co.mawa.bes.dto.v2;

import lombok.Data;

@Data
public class MembershipPremiumEditRequest {
    private Long amountCents;
    private String reason;
}
