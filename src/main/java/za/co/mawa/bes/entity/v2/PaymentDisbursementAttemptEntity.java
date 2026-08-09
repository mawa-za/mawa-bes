package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.PaymentDisbursementAttemptStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_disbursement_attempt", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_attempt_request_no", columnNames = {"payment_request_id", "attempt_no"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDisbursementAttemptEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(name = "id", length = 255)
    private String id;

    @Column(name = "payment_request_id", nullable = false, length = 255)
    private String paymentRequestId;

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentDisbursementAttemptStatus status;

    @Column(name = "instruction_id", length = 255)
    private String instructionId;

    @Column(name = "provider_status", length = 100)
    private String providerStatus;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;

    @Column(name = "bank_report_json", columnDefinition = "LONGTEXT")
    private String bankReportJson;

    @Column(name = "bank_report_retrieved_at")
    private LocalDateTime bankReportRetrievedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (provider == null || provider.isBlank()) provider = "FNB";
        if (status == null) status = PaymentDisbursementAttemptStatus.QUEUED;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
