package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;


import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Builder
@Entity
@Table(name = "funeral_external_membership_cover")
public class FuneralExternalMembershipCoverEntity {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "identity_number", nullable = false)
    private String identityNumber;

    @Column(name = "source_tenant_id", nullable = false)
    private String sourceTenantId;

    @Column(name = "source_tenant_name")
    private String sourceTenantName;

    @Column(name = "source_membership_id", nullable = false)
    private String sourceMembershipId;

    @Column(name = "source_membership_no")
    private String sourceMembershipNo;

    @Column(name = "source_reference")
    private String sourceReference;

    @Column(name = "burial_society_partner_id")
    private String burialSocietyPartnerId;

    @Column(name = "burial_society_name", nullable = false)
    private String burialSocietyName;

    @Column(name = "cover_amount_cents", nullable = false)
    private Long coverAmountCents = 0L;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = "ACTIVE";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
