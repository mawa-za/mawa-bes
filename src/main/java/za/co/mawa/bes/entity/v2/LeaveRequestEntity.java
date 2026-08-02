package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "leave_request")
public class LeaveRequestEntity {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "request_number", length = 50, unique = true)
    private String requestNumber;

    @Column(name = "employee_partner_id", nullable = false)
    private String employeePartnerId;

    @Column(name = "employment_id")
    private String employmentId;

    /** Retained for legacy rows only. Approvers are resolved by the approval workflow. */
    @Column(name = "approver_partner_id")
    private String approverPartnerId;

    @Column(name = "leave_type", nullable = false, length = 50)
    private String leaveType;

    @Column(name = "leave_type_id")
    private String leaveTypeId;

    @Column(name = "leave_profile_id")
    private String leaveProfileId;

    @Column(name = "working_calendar_id")
    private String workingCalendarId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "days", nullable = false, precision = 10, scale = 2)
    private BigDecimal days;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "projected_balance", precision = 12, scale = 2)
    private BigDecimal projectedBalance;

    @Column(name = "request_reason", length = 1000)
    private String requestReason;

    @Column(name = "attachment_object_ids", columnDefinition = "JSON")
    private String attachmentObjectIds;

    @Column(name = "balance_ledger_id")
    private String balanceLedgerId;

    @Column(name = "approval_request_id")
    private String approvalRequestId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "status_reason", length = 1000)
    private String statusReason;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null || status.isBlank()) status = "PENDING";
        if (unit == null || unit.isBlank()) unit = "DAYS";
        if (version == null) version = 0L;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
