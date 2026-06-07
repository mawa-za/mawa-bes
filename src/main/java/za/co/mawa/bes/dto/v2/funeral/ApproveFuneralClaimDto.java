package za.co.mawa.bes.dto.v2.funeral;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApproveFuneralClaimDto {
    /** APPROVED, PARTIALLY_APPROVED, REJECTED, SUBMITTED, CANCELLED or PAID. */
    private String status;
    private Long approvedAmountCents;
    private String decisionNotes;
}
