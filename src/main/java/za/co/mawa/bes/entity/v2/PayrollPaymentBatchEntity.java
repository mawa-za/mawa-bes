package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.enums.payroll.PayrollPaymentBatchStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payroll_payment_batch")
public class PayrollPaymentBatchEntity {

    @Id
    @Column(name = "id", length = 255, nullable = false)
    private String id;

    @Column(name = "batch_no", length = 50, nullable = false, unique = true)
    private String batchNo;

    @Column(name = "description")
    private String description;

    @Column(name = "pay_period", length = 6, nullable = false)
    private String payPeriod;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "source_batch_id")
    private String sourceBatchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private PayrollPaymentBatchStatus status = PayrollPaymentBatchStatus.DRAFT;

    @Column(name = "total_employees", nullable = false)
    private Integer totalEmployees = 0;

    @Column(name = "total_amount_cents", nullable = false)
    private Long totalAmountCents = 0L;

    @Column(name = "eft_file_generated", nullable = false)
    private Boolean eftFileGenerated = false;

    @Column(name = "eft_file_name")
    private String eftFileName;

    @Column(name = "eft_file_generated_at")
    private LocalDateTime eftFileGeneratedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

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

        if (status == null) {
            status = PayrollPaymentBatchStatus.DRAFT;
        }

        if (totalEmployees == null) {
            totalEmployees = 0;
        }

        if (totalAmountCents == null) {
            totalAmountCents = 0L;
        }

        if (eftFileGenerated == null) {
            eftFileGenerated = false;
        }

        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}