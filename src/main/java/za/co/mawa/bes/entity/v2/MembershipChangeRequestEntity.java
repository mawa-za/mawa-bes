package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.MembershipChangeStatus;
import za.co.mawa.bes.enums.MembershipChangeType;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "membership_change_request")
public class MembershipChangeRequestEntity {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(name = "id", length = 255)
    private String id;

    @Column(name = "membership_id", nullable = false, length = 255)
    private String membershipId;

    @Column(name = "source_membership_id", length = 255)
    private String sourceMembershipId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 30)
    private MembershipChangeType changeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MembershipChangeStatus status;

    @Column(name = "old_member_id", length = 255)
    private String oldMemberId;

    @Column(name = "new_member_id", length = 255)
    private String newMemberId;

    @Column(name = "old_plan_id", length = 255)
    private String oldPlanId;

    @Column(name = "new_plan_id", length = 255)
    private String newPlanId;

    @Column(name = "old_premium_cents")
    private Long oldPremiumCents;

    @Column(name = "new_premium_cents")
    private Long newPremiumCents;

    @Column(name = "old_dependent_id", length = 255)
    private String oldDependentId;

    @Column(name = "old_dependent_partner_id", length = 255)
    private String oldDependentPartnerId;

    @Column(name = "new_dependent_partner_id", length = 255)
    private String newDependentPartnerId;

    @Column(name = "old_dependent_type", length = 50)
    private String oldDependentType;

    @Column(name = "new_dependent_type", length = 50)
    private String newDependentType;

    @Column(name = "waiting_period_months")
    private Integer waitingPeriodMonths;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "approval_request_id", length = 255)
    private String approvalRequestId;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "requested_by", nullable = false, length = 255)
    private String requestedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by", length = 255)
    private String approvedBy;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "applied_by", length = 255)
    private String appliedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;
}
