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
@Table(name="tombstone_design")
public class TombstoneDesignEntity {
    @Id
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy="uuid")
    @Column(length=255)
    private String id;

    @Column(name="tombstone_order_id", nullable=false) private String tombstoneOrderId;
    @Column(name="version_no", nullable=false) private Integer versionNo = 1;
    @Column(name="status", nullable=false, length=30) private String status = "DRAFT";
    @Column(name="inscription_text", columnDefinition="TEXT") private String inscriptionText;
    @Column(name="font_name", length=100) private String fontName;
    @Column(name="layout_notes", columnDefinition="TEXT") private String layoutNotes;
    @Column(name="symbols_json", columnDefinition="json") private String symbolsJson;
    @Column(name="material", length=100) private String material;
    @Column(name="colour", length=100) private String colour;
    @Column(name="dimensions") private String dimensions;
    @Column(name="design_attachment_id") private String designAttachmentId;
    @Column(name="sent_for_approval_at") private LocalDateTime sentForApprovalAt;
    @Column(name="customer_approval_method", length=30) private String customerApprovalMethod;
    @Column(name="customer_approval_reference") private String customerApprovalReference;
    @Column(name="approved_at") private LocalDateTime approvedAt;
    @Column(name="approved_by") private String approvedBy;
    @Column(name="change_request", columnDefinition="TEXT") private String changeRequest;

    @Column(name="created_at", nullable=false, updatable=false) private LocalDateTime createdAt;
    @Column(name="created_by") private String createdBy;
    @Column(name="updated_at") private LocalDateTime updatedAt;
    @Column(name="updated_by") private String updatedBy;

    @PrePersist public void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); }
}
