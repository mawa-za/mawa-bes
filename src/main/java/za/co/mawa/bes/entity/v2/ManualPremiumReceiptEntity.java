package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "manual_premium_receipt", uniqueConstraints = @UniqueConstraint(name = "uq_manual_premium_receipt_book_no", columnNames = {"receipt_book_no", "manual_receipt_no"}))
public class ManualPremiumReceiptEntity {
    @Id @GeneratedValue(generator = "system-uuid") @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(length = 255) private String id;
    @Column(name = "payment_batch_id", nullable = false, length = 255) private String paymentBatchId;
    @Column(name = "cashup_id", length = 255) private String cashupId;
    @Column(name = "membership_id", nullable = false, length = 255) private String membershipId;
    @Column(name = "capture_mode", nullable = false, length = 30) private String captureMode;
    @Column(name = "receipt_book_no", nullable = false, length = 100) private String receiptBookNo;
    @Column(name = "manual_receipt_no", nullable = false, length = 100) private String manualReceiptNo;
    @Column(name = "original_receipt_date", nullable = false) private LocalDate originalReceiptDate;
    @Column(name = "amount_cents", nullable = false) private Long amountCents;
    @Column(name = "payment_method", nullable = false, length = 50) private String paymentMethod;
    @Column(name = "original_collector", length = 255) private String originalCollector;
    @Column(name = "original_collector_employee_id", length = 255) private String originalCollectorEmployeeId;
    @Column(name = "location", length = 255) private String location;
    @Column(name = "location_name", length = 255) private String locationName;
    @Column(name = "workcentre_id", length = 255) private String workcentreId; // legacy, no longer captured
    @Column(name = "late_capture_reason", columnDefinition = "TEXT") private String lateCaptureReason;
    @Column(name = "proof_attachment_id", length = 255) private String proofAttachmentId;
    @Column(name = "captured_at", nullable = false) private LocalDateTime capturedAt;
    @Column(name = "captured_by", nullable = false, length = 255) private String capturedBy;
    @Column(name = "notes", columnDefinition = "TEXT") private String notes;
}
