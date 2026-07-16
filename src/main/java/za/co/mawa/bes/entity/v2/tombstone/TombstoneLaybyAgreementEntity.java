package za.co.mawa.bes.entity.v2.tombstone;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name="tombstone_layby_agreement")
public class TombstoneLaybyAgreementEntity {
    @Id
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy="uuid")
    @Column(length=255)
    private String id;

    @Column(name="agreement_no", nullable=false, unique=true, length=50) private String agreementNo;
    @Column(name="tombstone_order_id", nullable=false, unique=true) private String tombstoneOrderId;
    @Column(name="deposit_required_cents", nullable=false) private Long depositRequiredCents = 0L;
    @Column(name="installment_amount_cents", nullable=false) private Long installmentAmountCents = 0L;
    @Column(name="payment_frequency", nullable=false, length=20) private String paymentFrequency = "MONTHLY";
    @Column(name="start_date", nullable=false) private LocalDate startDate;
    @Column(name="expected_settlement_date") private LocalDate expectedSettlementDate;
    @Column(name="grace_period_days", nullable=false) private Integer gracePeriodDays = 0;
    @Column(name="administration_fee_cents", nullable=false) private Long administrationFeeCents = 0L;
    @Column(name="total_amount_cents", nullable=false) private Long totalAmountCents = 0L;
    @Column(name="paid_amount_cents", nullable=false) private Long paidAmountCents = 0L;
    @Column(name="balance_cents", nullable=false) private Long balanceCents = 0L;
    @Column(name="status", nullable=false, length=30) private String status = "ACTIVE";
    @Column(name="cancellation_reason", columnDefinition="TEXT") private String cancellationReason;
    @Column(name="terms_accepted_at") private LocalDateTime termsAcceptedAt;
    @Column(name="terms_accepted_by") private String termsAcceptedBy;

    @Column(name="created_at", nullable=false, updatable=false) private LocalDateTime createdAt;
    @Column(name="created_by") private String createdBy;
    @Column(name="updated_at") private LocalDateTime updatedAt;
    @Column(name="updated_by") private String updatedBy;

    @PrePersist public void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); }
}
