package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.CaseEventStatus;
import za.co.mawa.bes.enums.CaseEventType;
import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "case_event")
public class CaseEventEntity {
    @Id @GeneratedValue(generator = "system-uuid") @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name = "case_id", nullable = false) private String caseId;
    @Enumerated(EnumType.STRING) @Column(name = "event_type", nullable = false) private CaseEventType eventType;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "start_at", nullable = false) private LocalDateTime startAt;
    @Column(name = "end_at") private LocalDateTime endAt;
    private String location;
    @Column(name = "reminder_at") private LocalDateTime reminderAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CaseEventStatus status = CaseEventStatus.SCHEDULED;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "created_by") private String createdBy;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @Column(name = "updated_by") private String updatedBy;
}
