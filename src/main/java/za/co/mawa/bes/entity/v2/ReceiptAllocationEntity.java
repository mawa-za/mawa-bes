package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.ReceiptAllocationType;
import za.co.mawa.bes.enums.ReceiptStatus;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "receipt_allocation")
public class ReceiptAllocationEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(length = 255)
    private String id;

    @Column(name = "receipt_id", nullable = false, length = 255)
    private String receiptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_type", nullable = false, length = 50)
    private ReceiptAllocationType allocationType;

    @Column(name = "reference_id", length = 255)
    private String referenceId;

    @Column(name = "reference_no", length = 150)
    private String referenceNo;

    @Column(name = "period_yyyymm", length = 6)
    private String periodYYYYMM;

    @Column(name = "membership_id", length = 255)
    private String membershipId;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReceiptStatus status = ReceiptStatus.POSTED;

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