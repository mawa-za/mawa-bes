package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "working_calendar")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkingCalendarEntity {
    @Id @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(nullable = false, unique = true, length = 50) private String code;
    @Column(nullable = false, length = 150) private String name;
    @Column(length = 500) private String description;
    @Column(name="monday_working", nullable=false) private Boolean mondayWorking;
    @Column(name="tuesday_working", nullable=false) private Boolean tuesdayWorking;
    @Column(name="wednesday_working", nullable=false) private Boolean wednesdayWorking;
    @Column(name="thursday_working", nullable=false) private Boolean thursdayWorking;
    @Column(name="friday_working", nullable=false) private Boolean fridayWorking;
    @Column(name="saturday_working", nullable=false) private Boolean saturdayWorking;
    @Column(name="sunday_working", nullable=false) private Boolean sundayWorking;
    @Column(name="hours_per_day", nullable=false, precision=6, scale=2) private BigDecimal hoursPerDay;
    @Column(nullable=false) private Boolean active;
    @Column(name="created_at", insertable=false, updatable=false) private LocalDateTime createdAt;
    @Column(name="created_by") private String createdBy;
    @Column(name="updated_at", insertable=false, updatable=false) private LocalDateTime updatedAt;
    @Column(name="updated_by") private String updatedBy;
    @Version @Column(nullable=false) private Long version;
}
