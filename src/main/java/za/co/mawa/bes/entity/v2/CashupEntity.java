package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cashup")
public class CashupEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(name = "id", length = 255)
    private String id;

    @Column(name = "cashup_no", nullable = false, unique = true)
    private Long cashupNo;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "cashup_date", nullable = false)
    private LocalDate cashupDate;

    @Column(name = "total_cents", nullable = false)
    private Long totalCents = 0L;

    @Column(name = "receipt_count", nullable = false)
    private Integer receiptCount = 0;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "AWAITING_DEPOSITS";

    @Column(name = "deposit_total_cents", nullable = false)
    private Long depositTotalCents = 0L;

    @Column(name = "deposit_count", nullable = false)
    private Integer depositCount = 0;

    @Column(name = "approval_request_id", length = 255)
    private String approvalRequestId;

    @Column(name = "legacy_transaction_id", length = 255)
    private String legacyTransactionId;

    @Column(name = "source", nullable = false, length = 30)
    private String source = "MAWAPAY";

    @Column(name = "receipt_book_no", length = 100)
    private String receiptBookNo;

    @Column(name = "receipt_from_no", length = 100)
    private String receiptFromNo;

    @Column(name = "receipt_to_no", length = 100)
    private String receiptToNo;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;
}