package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_balance_adjustment_request")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LeaveBalanceAdjustmentRequestEntity {
    @Id @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name="request_number", nullable=false, unique=true, length=50) private String requestNumber;
    @Column(name="employment_id", nullable=false) private String employmentId;
    @Column(name="leave_type_id", nullable=false) private String leaveTypeId;
    @Column(name="adjustment_amount", nullable=false, precision=12, scale=2) private BigDecimal adjustmentAmount;
    @Column(name="effective_date", nullable=false) private LocalDate effectiveDate;
    @Column(nullable=false, length=1000) private String reason;
    @Column(name="attachment_object_ids", columnDefinition="JSON") private String attachmentObjectIds;
    @Column(nullable=false, length=30) private String status;
    @Column(name="approval_request_id") private String approvalRequestId;
    @Column(name="requested_at", insertable=false, updatable=false) private LocalDateTime requestedAt;
    @Column(name="requested_by", nullable=false) private String requestedBy;
    @Column(name="actioned_at") private LocalDateTime actionedAt;
    @Column(name="actioned_by") private String actionedBy;
    @Column(name="status_reason", length=1000) private String statusReason;
    @Version @Column(nullable=false) private Long version;
}
