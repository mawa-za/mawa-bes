package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.ClaimType;
import za.co.mawa.bes.enums.DependentType;
import za.co.mawa.bes.enums.MembershipClaimType;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Builder
@Entity
@Table(
        name = "membership_plan_claim_payout",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_plan_claim_dependent_type",
                        columnNames = {"plan_id", "claim_type", "dependent_type"}
                )
        },
        indexes = {
                @Index(name = "idx_mpcp_plan_id", columnList = "plan_id"),
                @Index(name = "idx_mpcp_claim_type", columnList = "claim_type"),
                @Index(name = "idx_mpcp_dependent_type", columnList = "dependent_type"),
                @Index(name = "idx_mpcp_active", columnList = "active")
        }
)
public class MembershipPlanClaimPayoutEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @NotNull
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private MembershipPlanEntity plan;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "claim_type", nullable = false, length = 50)
    private MembershipClaimType claimType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "dependent_type", nullable = false, length = 50)
    private DependentType dependentType = DependentType.ANY;

    @NotNull
    @Column(name = "payout_amount_cents", nullable = false)
    private Long payoutAmountCents;

    @NotNull
    private Boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;
}
