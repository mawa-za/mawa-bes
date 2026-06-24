package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.DependentType;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Builder
@Entity
@Table(
        name = "membership_plan_premium_rule",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_plan_dependent_age_range",
                        columnNames = {"plan_id", "dependent_type", "min_age", "max_age"}
                )
        }
)
public class MembershipPlanPremiumRuleEntity {

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
    @Column(name = "dependent_type", nullable = false, length = 50)
    private DependentType dependentType;

    @NotNull
    @Column(name = "min_age", nullable = false)
    private Integer minAge;

    @NotNull
    @Column(name = "max_age", nullable = false)
    private Integer maxAge;

    @NotNull
    @Column(name = "additional_premium_cents", nullable = false)
    private Long additionalPremiumCents;

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