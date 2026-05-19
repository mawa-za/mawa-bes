package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.PaymentMethod;
import za.co.mawa.bes.enums.PaymentRequestSourceType;
import za.co.mawa.bes.enums.PaymentRequestStatus;
import za.co.mawa.bes.enums.PaymentRequestType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "payment_request")
public class PaymentRequestEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "request_no", nullable = false, unique = true)
    private String requestNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    private PaymentRequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type")
    private PaymentRequestSourceType sourceType;

    @Column(name = "source_id")
    private String sourceId;

    @Column(name = "payee_partner_id")
    private String payeePartnerId;

    @Column(name = "payee_name", nullable = false)
    private String payeeName;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency = "ZAR";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "account_holder")
    private String accountHolder;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "branch_code")
    private String branchCode;

    @Column(name = "account_type")
    private String accountType;

    @Column(name = "invoice_no")
    private String invoiceNo;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "payment_reason")
    private String paymentReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "requested_payment_date")
    private LocalDate requestedPaymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentRequestStatus status = PaymentRequestStatus.DRAFT;

    @Column(name = "approval_request_id")
    private String approvalRequestId;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "paid_reference")
    private String paidReference;

    @Column(name = "paid_by")
    private String paidBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "approved_by", length = 36)
    private String approvedBy;

    @Column(name = "approved_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date approvedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = PaymentRequestStatus.DRAFT;
        }

        if (this.currency == null || this.currency.isBlank()) {
            this.currency = "ZAR";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
