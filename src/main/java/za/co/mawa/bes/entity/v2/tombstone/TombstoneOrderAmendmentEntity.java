package za.co.mawa.bes.entity.v2.tombstone;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name="tombstone_order_amendment")
public class TombstoneOrderAmendmentEntity {
    @Id @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy="uuid")
    @Column(length=255) private String id;
    @Column(name="tombstone_order_id", nullable=false) private String tombstoneOrderId;
    @Column(name="amendment_no", nullable=false) private Integer amendmentNo;
    @Column(name="reason", nullable=false, columnDefinition="TEXT") private String reason;
    @Column(name="amount_delta_cents", nullable=false) private Long amountDeltaCents = 0L;
    @Column(name="status", nullable=false, length=40) private String status = "PENDING_CUSTOMER_APPROVAL";
    @Column(name="supporting_attachment_id") private String supportingAttachmentId;
    @Column(name="requested_at", nullable=false) private LocalDateTime requestedAt;
    @Column(name="requested_by") private String requestedBy;
    @Column(name="approved_at") private LocalDateTime approvedAt;
    @Column(name="approved_by") private String approvedBy;
    @Column(name="rejected_at") private LocalDateTime rejectedAt;
    @Column(name="rejected_by") private String rejectedBy;
    @Column(name="response_notes", columnDefinition="TEXT") private String responseNotes;
    @Column(name="created_at", nullable=false, updatable=false) private LocalDateTime createdAt;
    @Column(name="updated_at") private LocalDateTime updatedAt;
    @PrePersist public void prePersist() { LocalDateTime now=LocalDateTime.now(); if(requestedAt==null) requestedAt=now; if(createdAt==null) createdAt=now; }
    @PreUpdate public void preUpdate() { updatedAt=LocalDateTime.now(); }
}
