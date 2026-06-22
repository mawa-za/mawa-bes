package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.mawa.bes.enums.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipClaimUpdateRequestDto {

    private String id;
    private String claimNo;
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
    private MembershipClaimStatus status;
    private String rejectionReason;
    private String notes;
    private String approvalRequestId;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private String paymentRequestId;
    private PaymentMethod payoutMethod;
    private String bankName;
    private String accountHolderName;
    private String accountNumber;
    private String branchCode;
    private BankAccountType accountType;
}
