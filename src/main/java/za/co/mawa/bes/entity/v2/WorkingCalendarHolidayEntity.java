package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "working_calendar_holiday")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkingCalendarHolidayEntity {
    @Id @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name="working_calendar_id", nullable=false) private String workingCalendarId;
    @Column(name="holiday_date", nullable=false) private LocalDate holidayDate;
    @Column(nullable=false, length=150) private String name;
    @Column(name="recurring_annual", nullable=false) private Boolean recurringAnnual;
    @Column(nullable=false) private Boolean active;
    @Column(name="created_at", insertable=false, updatable=false) private LocalDateTime createdAt;
    @Column(name="created_by") private String createdBy;
}
