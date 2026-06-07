package za.co.mawa.bes.dto.v2.funeral;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class FuneralClaimDto {
    private String funeralServiceClaimId;
    private String membershipClaimId;
    private String claimNo;
    private String membershipId;
    private String membershipNumber;
    private String claimType;
    private String deceasedType;
    private String deceasedPartnerId;
    private String claimantPartnerId;
    private LocalDate dateOfDeath;
    private Long claimedAmountCents;
    private Long approvedAmountCents;
    private String status;
    private String coverSource;
    private String sourceTenantId;
    private String sourceTenantName;
    private String sourceMembershipId;
    private String sourceReference;
    private LocalDateTime approvedAt;
}
