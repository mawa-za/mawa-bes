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
@Table(name="tombstone_installation_checklist")
public class TombstoneInstallationChecklistEntity {
    @Id
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy="uuid")
    @Column(length=255)
    private String id;

    @Column(name="installation_id", nullable=false) private String installationId;
    @Column(name="checklist_code", nullable=false, length=100) private String checklistCode;
    @Column(name="checklist_label", nullable=false) private String checklistLabel;
    @Column(name="required", nullable=false) private Boolean required = true;
    @Column(name="completed", nullable=false) private Boolean completed = false;
    @Column(name="completed_at") private LocalDateTime completedAt;
    @Column(name="completed_by") private String completedBy;
    @Column(name="notes", columnDefinition="TEXT") private String notes;
    @Column(name="evidence_attachment_id") private String evidenceAttachmentId;

    @Column(name="created_at", nullable=false, updatable=false) private LocalDateTime createdAt;

    @PrePersist public void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
