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
@Table(name = "funeral_service_claim")
public class FuneralServiceClaimEntity {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "funeral_service_id", nullable = false)
    private String funeralServiceId;

    @Column(name = "membership_claim_id", nullable = false)
    private String membershipClaimId;

    @Column(name = "cover_source", nullable = false)
    private String coverSource = "LOCAL_TENANT";

    @Column(name = "source_tenant_id")
    private String sourceTenantId;

    @Column(name = "source_tenant_name")
    private String sourceTenantName;

    @Column(name = "source_membership_id")
    private String sourceMembershipId;

    @Column(name = "source_reference")
    private String sourceReference;

    @Column(name = "burial_society_partner_id")
    private String burialSocietyPartnerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
