package za.co.mawa.bes.entity.v2.tombstone;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name="tombstone_site_assessment")
public class TombstoneSiteAssessmentEntity {
    @Id
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy="uuid")
    @Column(length=255)
    private String id;

    @Column(name="tombstone_order_id", nullable=false) private String tombstoneOrderId;
    @Column(name="version_no", nullable=false) private Integer versionNo = 1;
    @Column(name="status", nullable=false, length=40) private String status = "REQUESTED";
    @Column(name="scheduled_at") private LocalDateTime scheduledAt;
    @Column(name="assessed_at") private LocalDateTime assessedAt;
    @Column(name="assessor_partner_id") private String assessorPartnerId;
    @Column(name="cemetery_name") private String cemeteryName;
    @Column(name="grave_number", length=100) private String graveNumber;
    @Column(name="grave_latitude", precision=10, scale=7) private BigDecimal graveLatitude;
    @Column(name="grave_longitude", precision=10, scale=7) private BigDecimal graveLongitude;
    @Column(name="grave_length_mm") private Integer graveLengthMm;
    @Column(name="grave_width_mm") private Integer graveWidthMm;
    @Column(name="foundation_condition") private String foundationCondition;
    @Column(name="access_restrictions", columnDefinition="TEXT") private String accessRestrictions;
    @Column(name="cemetery_rules", columnDefinition="TEXT") private String cemeteryRules;
    @Column(name="permit_required", nullable=false) private Boolean permitRequired = false;
    @Column(name="permit_reference", length=150) private String permitReference;
    @Column(name="permit_approved", nullable=false) private Boolean permitApproved = false;
    @Column(name="travel_distance_km", precision=10, scale=2) private BigDecimal travelDistanceKm;
    @Column(name="additional_work_required", columnDefinition="TEXT") private String additionalWorkRequired;
    @Column(name="additional_cost_cents", nullable=false) private Long additionalCostCents = 0L;
    @Column(name="photo_attachment_ids_json", columnDefinition="json") private String photoAttachmentIdsJson;
    @Column(name="failure_reason", columnDefinition="TEXT") private String failureReason;

    @Column(name="created_at", nullable=false, updatable=false) private LocalDateTime createdAt;
    @Column(name="created_by") private String createdBy;
    @Column(name="updated_at") private LocalDateTime updatedAt;
    @Column(name="updated_by") private String updatedBy;

    @PrePersist public void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); }
}
