package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employment_status_history")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EmploymentStatusHistoryEntity {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name = "employment_id", nullable = false)
    private String employmentId;
    @Column(name = "action_request_id")
    private String actionRequestId;
    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;
    @Column(name = "old_status", length = 30)
    private String oldStatus;
    @Column(name = "new_status", length = 30)
    private String newStatus;
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
    @Column(name = "reason", length = 1000)
    private String reason;
    @Column(name = "previous_values", columnDefinition = "JSON")
    private String previousValues;
    @Column(name = "new_values", columnDefinition = "JSON")
    private String newValues;
    @Column(name = "approval_request_id")
    private String approvalRequestId;
    @Column(name = "changed_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime changedAt;
    @Column(name = "changed_by")
    private String changedBy;
}
