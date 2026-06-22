package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import za.co.mawa.bes.enums.payroll.PayrollPaymentItemStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payroll_payment_item")
public class PayrollPaymentItemEntity {

    @Id
    @Column(name = "id", length = 255, nullable = false)
    private String id;

    @Column(name = "batch_id", length = 255, nullable = false)
    private String batchId;

    @Column(name = "employee_id", length = 255)
    private String employeeId;

    @Column(name = "employee_no", length = 50)
    private String employeeNo;

    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @Column(name = "account_no", length = 50, nullable = false)
    private String accountNo;

    @Column(name = "account_type", length = 30)
    private String accountType;

    @Column(name = "account_holder_name")
    private String accountHolderName;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents = 0L;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "salary_reference", length = 100)
    private String salaryReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private PayrollPaymentItemStatus status = PayrollPaymentItemStatus.PENDING;

    @Column(name = "excluded", nullable = false)
    private Boolean excluded = false;

    @Column(name = "exclusion_reason")
    private String exclusionReason;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }

        if (amountCents == null) {
            amountCents = 0L;
        }

        if (status == null) {
            status = PayrollPaymentItemStatus.PENDING;
        }

        if (excluded == null) {
            excluded = false;
        }

        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}