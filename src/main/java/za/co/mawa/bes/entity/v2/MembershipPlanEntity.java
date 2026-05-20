package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Entity
@Table(name = "membership_plan")
public class MembershipPlanEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @NotBlank
    @Column(name = "plan_code", nullable = false, unique = true, length = 50)
    private String planCode;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Column(name = "premium_cents", nullable = false)
    private Long premiumCents;

    @NotBlank
    @Column(nullable = false, length = 10)
    private String currency = "ZAR";

    @Column(name = "max_dependents")
    private Integer maxDependents;

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

    @Column(name = "old_id")
    private String oldId;

    @ToString.Exclude
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MembershipPlanClaimPayoutEntity> claimPayouts = new ArrayList<>();
    // Getters and Setters
}