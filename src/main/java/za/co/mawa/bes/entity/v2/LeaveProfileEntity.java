package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_profile")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LeaveProfileEntity {
    @Id @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(nullable=false, unique=true, length=50) private String code;
    @Column(nullable=false, length=150) private String name;
    @Column(length=500) private String description;
    @Column(name="working_calendar_id", nullable=false) private String workingCalendarId;
    @Column(name="default_profile", nullable=false) private Boolean defaultProfile;
    @Column(name="active_from", nullable=false) private LocalDate activeFrom;
    @Column(name="active_to", nullable=false) private LocalDate activeTo;
    @Column(nullable=false) private Boolean active;
    @Column(name="created_at", insertable=false, updatable=false) private LocalDateTime createdAt;
    @Column(name="created_by") private String createdBy;
    @Column(name="updated_at", insertable=false, updatable=false) private LocalDateTime updatedAt;
    @Column(name="updated_by") private String updatedBy;
    @Version @Column(nullable=false) private Long version;
}
