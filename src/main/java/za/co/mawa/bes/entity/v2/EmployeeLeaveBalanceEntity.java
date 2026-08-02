package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_leave_balance")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EmployeeLeaveBalanceEntity {
    @Id @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name="employment_id", nullable=false) private String employmentId;
    @Column(name="leave_type_id", nullable=false) private String leaveTypeId;
    @Column(name="leave_profile_rule_id") private String leaveProfileRuleId;
    @Column(name="cycle_start", nullable=false) private LocalDate cycleStart;
    @Column(name="cycle_end", nullable=false) private LocalDate cycleEnd;
    @Column(name="opening_balance", nullable=false, precision=12, scale=2) private BigDecimal openingBalance;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal accrued;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal taken;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal adjusted;
    @Column(name="carried_forward", nullable=false, precision=12, scale=2) private BigDecimal carriedForward;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal expired;
    @Column(name="available_balance", nullable=false, precision=12, scale=2) private BigDecimal availableBalance;
    @Column(name="last_accrual_date") private LocalDate lastAccrualDate;
    @Column(name="created_at", insertable=false, updatable=false) private LocalDateTime createdAt;
    @Column(name="updated_at", insertable=false, updatable=false) private LocalDateTime updatedAt;
    @Version @Column(nullable=false) private Long version;
}
