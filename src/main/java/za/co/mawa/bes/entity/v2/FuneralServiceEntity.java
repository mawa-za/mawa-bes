package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;


import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Builder
@Entity
@Table(name = "funeral_service")
public class FuneralServiceEntity {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "service_request_no", unique = true)
    private String serviceRequestNo;

    @Column(name = "mortuary_inventory_id")
    private String mortuaryInventoryId;

    @Column(name = "deceased_partner_id")
    private String deceasedPartnerId;

    @Column(name = "deceased_name", nullable = false)
    private String deceasedName;

    @Column(name = "deceased_identity_number")
    private String deceasedIdentityNumber;

    @Column(name = "package_id", nullable = false)
    private String packageId;

    @Column(name = "family_rep_id", nullable = false)
    private String familyRepId;

    @Column(name = "funeral_date")
    private LocalDate funeralDate;

    @Column(name = "funeral_area")
    private String funeralArea;

    @Column(name = "death_certificate_no", length = 100)
    private String deathCertificateNo;

    @Column(name = "cause_of_death", length = 255)
    private String causeOfDeath;

    @Column(name = "total_amount_cents", nullable = false)
    private Long totalAmountCents = 0L;

    @Column(name = "extras_json", columnDefinition = "json")
    private String extrasJson;

    @Column(name = "status", nullable = false)
    private String status = "ARRANGEMENT_CREATED";

    /** Last successfully completed wizard step (0-based). */
    @Column(name = "wizard_step", nullable = false)
    private Integer wizardStep = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = "ARRANGEMENT_CREATED";
        if (wizardStep == null) wizardStep = 0;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
