package za.co.mawa.bes.dto.v2.membership.claim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.mawa.bes.enums.MembershipClaimDeceasedType;
import za.co.mawa.bes.enums.MembershipClaimStatus;
import za.co.mawa.bes.enums.MembershipClaimType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipClaimListItemResponse {
    private String id;
    private String claimNo;
    private String membershipId;
    private String membershipNo;
    private String memberName;
    private String memberNumber;
    private String memberIdentityNumber;
    private String deceasedName;
    private String deceasedNumber;
    private String deceasedIdentityNumber;
    private String claimantName;
    private MembershipClaimType claimType;
    private String coveragePlanId;
    private LocalDate coverageEventDate;
    private MembershipClaimDeceasedType deceasedType;
    private String deceasedPartnerId;
    private LocalDate dateOfDeath;
    private LocalDate claimDate;
    private String claimantPartnerId;
    private Long claimAmountCents;
    private MembershipClaimStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
