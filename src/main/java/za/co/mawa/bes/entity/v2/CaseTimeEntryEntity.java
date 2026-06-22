package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "case_time_entry")
public class CaseTimeEntryEntity {
    @Id @GeneratedValue(generator = "system-uuid") @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name = "case_id", nullable = false) private String caseId;
    @Column(name = "task_id") private String taskId;
    @Column(name = "entry_date", nullable = false) private LocalDate entryDate;
    @Column(name = "user_id", nullable = false) private String userId;
    @Column(columnDefinition = "TEXT", nullable = false) private String description;
    @Column(nullable = false) private Long minutes;
    @Column(name = "hourly_rate_cents", nullable = false) private Long hourlyRateCents;
    @Column(name = "amount_cents", nullable = false) private Long amountCents;
    @Column(nullable = false) private Boolean billable = true;
    @Column(nullable = false) private Boolean billed = false;
    @Column(name = "invoice_id") private String invoiceId;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "created_by") private String createdBy;
}
