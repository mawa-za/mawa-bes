package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.PaymentBatchStatus;
import za.co.mawa.bes.enums.ReceiptSourceType;
import za.co.mawa.bes.enums.SyncStatus;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "payment_batch")
public class PaymentBatchEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(length = 255)
    private String id;

    @Column(name = "payment_batch_no", nullable = false, unique = true, length = 100)
    private String paymentBatchNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private ReceiptSourceType sourceType;

    @Column(name = "received_from_partner_id", length = 255)
    private String receivedFromPartnerId;

    @Column(name = "membership_id", length = 255)
    private String membershipId;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "total_amount_cents", nullable = false)
    private Long totalAmountCents = 0L;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "employee_responsible", length = 255)
    private String employeeResponsible;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "terminal_id", length = 100)
    private String terminalId;

    @Column(name = "local_payment_batch_id", length = 255)
    private String localPaymentBatchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentBatchStatus status = PaymentBatchStatus.POSTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 30)
    private SyncStatus syncStatus = SyncStatus.SYNCED;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "legacy_premium_payment_id", length = 50)
    private String legacyPremiumPaymentId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;
}
