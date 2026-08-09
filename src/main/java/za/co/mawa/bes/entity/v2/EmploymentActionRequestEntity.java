package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employment_action_request")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EmploymentActionRequestEntity {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name = "request_number", nullable = false, length = 50, unique = true)
    private String requestNumber;
    @Column(name = "action_type", nullable = false, length = 30)
    private String actionType;
    @Column(name = "employment_id")
    private String employmentId;
    @Column(name = "partner_id", nullable = false)
    private String partnerId;
    @Column(name = "proposed_type")
    private String proposedType;
    @Column(name = "proposed_start_date")
    private LocalDate proposedStartDate;
    @Column(name = "proposed_end_date")
    private LocalDate proposedEndDate;
    @Column(name = "proposed_position")
    private String proposedPosition;
    @Column(name = "proposed_branch")
    private String proposedBranch;
    @Column(name = "proposed_department")
    private String proposedDepartment;
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
    @Column(name = "expected_return_date")
    private LocalDate expectedReturnDate;
    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;
    @Column(name = "affects_payroll", nullable = false)
    private Boolean affectsPayroll;
    @Column(name = "suspend_system_access", nullable = false)
    private Boolean suspendSystemAccess;
    @Column(name = "attachment_object_ids", columnDefinition = "JSON")
    private String attachmentObjectIds;
    @Column(name = "status", nullable = false, length = 30)
    private String status;
    @Column(name = "approval_request_id")
    private String approvalRequestId;
    @Column(name = "resulting_employment_id")
    private String resultingEmploymentId;
    @Column(name = "requested_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime requestedAt;
    @Column(name = "requested_by", nullable = false)
    private String requestedBy;
    @Column(name = "actioned_at")
    private LocalDateTime actionedAt;
    @Column(name = "actioned_by")
    private String actionedBy;
    @Column(name = "status_reason", length = 1000)
    private String statusReason;
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
