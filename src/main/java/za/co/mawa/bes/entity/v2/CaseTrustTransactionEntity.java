package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.TrustPaymentMethod;
import za.co.mawa.bes.enums.TrustTransactionDirection;
import za.co.mawa.bes.enums.TrustTransactionType;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "case_trust_transaction")
public class CaseTrustTransactionEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "case_id", nullable = false)
    private String caseId;

    @Column(name = "client_partner_id", nullable = false)
    private String clientPartnerId;

    @Column(name = "transaction_no", nullable = false, unique = true)
    private String transactionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TrustTransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrustTransactionDirection direction;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "balance_after_cents", nullable = false)
    private Long balanceAfterCents;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private TrustPaymentMethod paymentMethod;

    @Column(name = "reference_no")
    private String referenceNo;

    @Column(name = "bank_reference")
    private String bankReference;

    @Column(name = "payee_name")
    private String payeeName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "related_invoice_id")
    private String relatedInvoiceId;

    @Column(name = "related_receipt_id")
    private String relatedReceiptId;

    @Column(name = "related_transaction_id")
    private String relatedTransactionId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(nullable = false)
    private Boolean reversed = false;

    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;

    @Column(name = "reversed_by")
    private String reversedBy;

    @Column(name = "reversal_reason", columnDefinition = "TEXT")
    private String reversalReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;
}
