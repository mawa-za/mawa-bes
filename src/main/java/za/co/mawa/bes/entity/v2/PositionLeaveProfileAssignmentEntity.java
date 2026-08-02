package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "position_leave_profile_assignment")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PositionLeaveProfileAssignmentEntity {
    @Id @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name="position_code", nullable=false) private String positionCode;
    @Column(name="leave_profile_id", nullable=false) private String leaveProfileId;
    @Column(name="effective_from", nullable=false) private LocalDate effectiveFrom;
    @Column(name="effective_to", nullable=false) private LocalDate effectiveTo;
    @Column(nullable=false) private Boolean active;
    @Column(name="assigned_at", insertable=false, updatable=false) private LocalDateTime assignedAt;
    @Column(name="assigned_by") private String assignedBy;
    @Version @Column(nullable=false) private Long version;
}
