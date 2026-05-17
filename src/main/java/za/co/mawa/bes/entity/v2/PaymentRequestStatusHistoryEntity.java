package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.PaymentRequestStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_request_status_history")
public class PaymentRequestStatusHistoryEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "payment_request_id", nullable = false)
    private String paymentRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status")
    private PaymentRequestStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private PaymentRequestStatus newStatus;

    @Column(name = "comment")
    private String comment;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    @Column(name = "changed_by")
    private String changedBy;

    @PrePersist
    public void prePersist() {
        this.changedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getPaymentRequestId() { return paymentRequestId; }
    public void setPaymentRequestId(String paymentRequestId) { this.paymentRequestId = paymentRequestId; }
    public PaymentRequestStatus getOldStatus() { return oldStatus; }
    public void setOldStatus(PaymentRequestStatus oldStatus) { this.oldStatus = oldStatus; }
    public PaymentRequestStatus getNewStatus() { return newStatus; }
    public void setNewStatus(PaymentRequestStatus newStatus) { this.newStatus = newStatus; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
}
