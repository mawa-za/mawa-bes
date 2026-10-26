package za.co.mawa.bes.dto.v2.membership.change;

import lombok.Data;

@Data
public class MembershipDependentEligibilityRequest {
    /** Approved coverage date; defaults to the date of submission. */
    private java.time.LocalDate eligibleFrom;
    private String reason;
}
