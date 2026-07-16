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
@Table(name="tombstone_production_job")
public class TombstoneProductionJobEntity {
    @Id
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy="uuid")
    @Column(length=255)
    private String id;

    @Column(name="job_no", nullable=false, unique=true, length=50) private String jobNo;
    @Column(name="tombstone_order_id", nullable=false) private String tombstoneOrderId;
    @Column(name="design_id", nullable=false) private String designId;
    @Column(name="internal_production", nullable=false) private Boolean internalProduction = true;
    @Column(name="supplier_partner_id") private String supplierPartnerId;
    @Column(name="purchase_order_id") private String purchaseOrderId;
    @Column(name="status", nullable=false, length=40) private String status = "MATERIAL_ORDERED";
    @Column(name="planned_start_date") private LocalDate plannedStartDate;
    @Column(name="planned_completion_date") private LocalDate plannedCompletionDate;
    @Column(name="actual_start_at") private LocalDateTime actualStartAt;
    @Column(name="actual_completion_at") private LocalDateTime actualCompletionAt;
    @Column(name="quality_checked_at") private LocalDateTime qualityCheckedAt;
    @Column(name="quality_checked_by") private String qualityCheckedBy;
    @Column(name="quality_notes", columnDefinition="TEXT") private String qualityNotes;

    @Column(name="created_at", nullable=false, updatable=false) private LocalDateTime createdAt;
    @Column(name="created_by") private String createdBy;
    @Column(name="updated_at") private LocalDateTime updatedAt;
    @Column(name="updated_by") private String updatedBy;

    @PrePersist public void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); }
}
