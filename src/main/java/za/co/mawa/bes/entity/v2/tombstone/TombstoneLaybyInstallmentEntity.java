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
@Table(name="tombstone_layby_installment")
public class TombstoneLaybyInstallmentEntity {
    @Id
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy="uuid")
    @Column(length=255)
    private String id;

    @Column(name="layby_agreement_id", nullable=false) private String laybyAgreementId;
    @Column(name="installment_no", nullable=false) private Integer installmentNo;
    @Column(name="due_date", nullable=false) private LocalDate dueDate;
    @Column(name="amount_cents", nullable=false) private Long amountCents = 0L;
    @Column(name="paid_amount_cents", nullable=false) private Long paidAmountCents = 0L;
    @Column(name="receipt_id") private String receiptId;
    @Column(name="paid_at") private LocalDateTime paidAt;
    @Column(name="status", nullable=false, length=30) private String status = "SCHEDULED";

    @Column(name="created_at", nullable=false, updatable=false) private LocalDateTime createdAt;

    @PrePersist public void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
