package za.co.mawa.bes.dto.v2.membership.change;

import lombok.*;
import za.co.mawa.bes.enums.MembershipChangeStatus;
import za.co.mawa.bes.enums.MembershipChangeType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipChangeResponse {
    private String id;
    private String membershipId;
    private String sourceMembershipId;
    private MembershipChangeType changeType;
    private MembershipChangeStatus status;

    private String oldMemberId;
    private String oldMemberName;
    private String newMemberId;
    private String newMemberName;

    private String oldPlanId;
    private String oldPlanName;
    private String newPlanId;
    private String newPlanName;
    private Long oldPremiumCents;
    private Long newPremiumCents;

    private String oldDependentId;
    private String oldDependentPartnerId;
    private String oldDependentName;
    private String newDependentPartnerId;
    private String newDependentName;
    private String oldDependentType;
    private String newDependentType;

    private Integer waitingPeriodMonths;
    private LocalDate effectiveDate;
    private String reason;
    private String approvalRequestId;
    private LocalDateTime requestedAt;
    private String requestedBy;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private LocalDateTime appliedAt;
    private String appliedBy;
}
