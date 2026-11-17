package za.co.mawa.bes.dto.v2;

import lombok.Data;

@Data
public class MembershipPremiumEditRequest {
    private Long amountCents;
    /** Optional optimistic concurrency value from the bulk review screen. */
    private Long expectedAmountCents;
    private Long expectedPaidAmountCents;
    private String reason;
}
