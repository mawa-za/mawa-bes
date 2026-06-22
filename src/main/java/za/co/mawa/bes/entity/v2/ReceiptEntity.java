package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.ReceiptSourceType;
import za.co.mawa.bes.enums.ReceiptStatus;
import za.co.mawa.bes.enums.SyncStatus;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "receipt")
public class ReceiptEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(length = 255)
    private String id;

    @Column(name = "receipt_no", nullable = false, unique = true, length = 100)
    private String receiptNo;

    @Column(name = "payment_batch_id", length = 255)
    private String paymentBatchId;

    @Column(name = "payment_batch_no", length = 100)
    private String paymentBatchNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private ReceiptSourceType sourceType;

    @Column(name = "received_from_partner_id", length = 255)
    private String receivedFromPartnerId;

    @Column(name = "membership_id", length = 255)
    private String membershipId;

    @Column(name = "receipt_date", nullable = false)
    private LocalDateTime receiptDate;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "total_amount_cents", nullable = false)
    private Long totalAmountCents = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReceiptStatus status = ReceiptStatus.POSTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 30)
    private SyncStatus syncStatus = SyncStatus.SYNCED;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "employee_responsible", length = 255)
    private String employeeResponsible;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "terminal_id", length = 100)
    private String terminalId;

    @Column(name = "external_receipt_no", length = 100)
    private String externalReceiptNo;

    @Column(name = "printed", nullable = false)
    private Boolean printed = true;

    @Column(name = "print_count", nullable = false)
    private Integer printCount = 1;

    @Column(name = "legacy_premium_payment_id", length = 50)
    private String legacyPremiumPaymentId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;
}