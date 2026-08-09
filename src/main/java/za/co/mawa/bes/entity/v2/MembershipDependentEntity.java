package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.DependentType;
import za.co.mawa.bes.enums.MembershipClaimType;
import za.co.mawa.bes.enums.MembershipDependentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Builder
@Entity
@Table(name = "membership_dependent", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"membership_id", "dependent_partner_id"})
})
public class MembershipDependentEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @NotBlank
    @Column(name = "membership_id", nullable = false, length = 36)
    private String membershipId;

    @NotBlank
    @Column(name = "dependent_partner_id", nullable = false, length = 36)
    private String dependentPartnerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship", nullable = false, length = 50)
    private DependentType dependentType;

    @NotNull
    private Boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MembershipDependentStatus status = MembershipDependentStatus.ACTIVE;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "deceased_date")
    private LocalDate deceasedDate;

    @Column(name = "status_reason", columnDefinition = "TEXT")
    private String statusReason;

    @Column(name = "source_change_request_id", length = 255)
    private String sourceChangeRequestId;

    @Column(name = "replaced_by_dependent_id", length = 255)
    private String replacedByDependentId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    public void prePersist() {
        if (active == null) active = true;
        if (status == null) status = MembershipDependentStatus.ACTIVE;
        if (effectiveFrom == null) effectiveFrom = LocalDate.now();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}