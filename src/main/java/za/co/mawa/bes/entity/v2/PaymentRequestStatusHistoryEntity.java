package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.PaymentRequestStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_request_status_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

}
