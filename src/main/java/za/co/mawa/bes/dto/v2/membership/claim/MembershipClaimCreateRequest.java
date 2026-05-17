package za.co.mawa.bes.dto.v2.membership.claim;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.enums.MembershipClaimDeceasedType;
import za.co.mawa.bes.enums.MembershipClaimType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class MembershipClaimCreateRequest {

    private String membershipId;

    private MembershipClaimType claimType;

    private MembershipClaimDeceasedType deceasedType;

    private String deceasedPartnerId;

    private LocalDate dateOfDeath;

    private LocalDate claimDate;

    private String causeOfDeath;

    private String deathCertificateNo;

    private String claimantPartnerId;

    private Long claimAmountCents;

    private String notes;

    private Boolean submit;
    private String payoutMethod; // CASH, EFT

    private String bankName;
    private String accountHolderName;
    private String accountNumber;
    private String branchCode;
    private String accountType;

    private List<String> linkedClaimIds = new ArrayList<>();
}