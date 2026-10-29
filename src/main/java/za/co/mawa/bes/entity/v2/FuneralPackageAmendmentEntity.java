package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/** A versioned, auditable change to an already invoiced funeral arrangement. */
@Entity
@Table(name = "funeral_package_amendment")
@Getter @Setter
public class FuneralPackageAmendmentEntity {
    @Id
    private String id;
    @Column(name = "funeral_service_id", nullable = false)
    private String funeralServiceId;
    @Column(name = "amendment_no", nullable = false, unique = true, length = 120)
    private String amendmentNo;
    @Column(name = "old_package_id", nullable = false)
    private String oldPackageId;
    @Column(name = "new_package_id", nullable = false)
    private String newPackageId;
    @Column(name = "old_package_snapshot", columnDefinition = "json")
    private String oldPackageSnapshot;
    @Column(name = "new_package_snapshot", columnDefinition = "json")
    private String newPackageSnapshot;
    @Column(name = "old_total_cents", nullable = false)
    private Long oldTotalCents;
    @Column(name = "new_total_cents", nullable = false)
    private Long newTotalCents;
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;
    @Column(name = "customer_acknowledged", nullable = false)
    private Boolean customerAcknowledged;
    @Column(name = "status", nullable = false, length = 40)
    private String status;
    @Column(name = "reconciliation_note", columnDefinition = "TEXT")
    private String reconciliationNote;
    @Column(name = "adjustment_documents_json", columnDefinition = "json")
    private String adjustmentDocumentsJson;
    @Column(name = "approval_request_id")
    private String approvalRequestId;
    @Column(name = "requested_by")
    private String requestedBy;
    @Column(name = "approved_by")
    private String approvedBy;
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    @Column(name = "applied_at")
    private LocalDateTime appliedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Version
    private Long version;
    @PrePersist
    public void created() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate
    public void updated() { updatedAt = LocalDateTime.now(); }
}
