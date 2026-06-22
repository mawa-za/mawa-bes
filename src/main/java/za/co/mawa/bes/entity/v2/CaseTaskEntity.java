package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.CasePriority;
import za.co.mawa.bes.enums.CaseTaskStatus;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "case_task")
public class CaseTaskEntity {
    @Id @GeneratedValue(generator = "system-uuid") @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name = "case_id", nullable = false) private String caseId;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "assigned_to") private String assignedTo;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CasePriority priority = CasePriority.NORMAL;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CaseTaskStatus status = CaseTaskStatus.TODO;
    @Column(name = "due_date") private LocalDateTime dueDate;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @Column(name = "completed_by") private String completedBy;
    @Column(nullable = false) private Boolean billable = false;
    @Column(name = "estimated_minutes") private Long estimatedMinutes;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "created_by") private String createdBy;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @Column(name = "updated_by") private String updatedBy;
}
