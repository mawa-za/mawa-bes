package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_leave_ledger")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EmployeeLeaveLedgerEntity {
    @Id @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name="employee_leave_balance_id", nullable=false) private String employeeLeaveBalanceId;
    @Column(name="employment_id", nullable=false) private String employmentId;
    @Column(name="leave_type_id", nullable=false) private String leaveTypeId;
    @Column(name="transaction_type", nullable=false, length=40) private String transactionType;
    @Column(name="transaction_date", nullable=false) private LocalDate transactionDate;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal amount;
    @Column(name="balance_after", nullable=false, precision=12, scale=2) private BigDecimal balanceAfter;
    @Column(name="reference_type", length=50) private String referenceType;
    @Column(name="reference_id") private String referenceId;
    @Column(length=500) private String description;
    @Column(name="created_at", insertable=false, updatable=false) private LocalDateTime createdAt;
    @Column(name="created_by") private String createdBy;
}
