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
@Table(name="tombstone_installation")
public class TombstoneInstallationEntity {
    @Id
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy="uuid")
    @Column(length=255)
    private String id;

    @Column(name="installation_no", nullable=false, unique=true, length=50) private String installationNo;
    @Column(name="tombstone_order_id", nullable=false) private String tombstoneOrderId;
    @Column(name="production_job_id") private String productionJobId;
    @Column(name="rework_of_installation_id") private String reworkOfInstallationId;
    @Column(name="status", nullable=false, length=40) private String status = "READY_TO_SCHEDULE";
    @Column(name="scheduled_start_at") private LocalDateTime scheduledStartAt;
    @Column(name="scheduled_end_at") private LocalDateTime scheduledEndAt;
    @Column(name="dispatched_at") private LocalDateTime dispatchedAt;
    @Column(name="arrived_at") private LocalDateTime arrivedAt;
    @Column(name="installed_at") private LocalDateTime installedAt;
    @Column(name="accepted_at") private LocalDateTime acceptedAt;
    @Column(name="completed_at") private LocalDateTime completedAt;
    @Column(name="cemetery_name") private String cemeteryName;
    @Column(name="grave_number", length=100) private String graveNumber;
    @Column(name="assigned_vehicle_id") private String assignedVehicleId;
    @Column(name="contact_person") private String contactPerson;
    @Column(name="contact_number", length=100) private String contactNumber;
    @Column(name="permit_reference", length=150) private String permitReference;
    @Column(name="instructions", columnDefinition="TEXT") private String instructions;
    @Column(name="before_photo_attachment_ids_json", columnDefinition="json") private String beforePhotoAttachmentIdsJson;
    @Column(name="after_photo_attachment_ids_json", columnDefinition="json") private String afterPhotoAttachmentIdsJson;
    @Column(name="customer_representative_name") private String customerRepresentativeName;
    @Column(name="customer_signature_attachment_id") private String customerSignatureAttachmentId;
    @Column(name="installer_signature_attachment_id") private String installerSignatureAttachmentId;
    @Column(name="completion_notes", columnDefinition="TEXT") private String completionNotes;
    @Column(name="rework_reason", columnDefinition="TEXT") private String reworkReason;

    @Column(name="created_at", nullable=false, updatable=false) private LocalDateTime createdAt;
    @Column(name="created_by") private String createdBy;
    @Column(name="updated_at") private LocalDateTime updatedAt;
    @Column(name="updated_by") private String updatedBy;

    @PrePersist public void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); }
}
