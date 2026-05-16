package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.PaymentMethod;
import za.co.mawa.bes.enums.PaymentRequestSourceType;
import za.co.mawa.bes.enums.PaymentRequestStatus;
import za.co.mawa.bes.enums.PaymentRequestType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    public String getId() { return id; }
    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
    public PaymentRequestType getRequestType() { return requestType; }
    public void setRequestType(PaymentRequestType requestType) { this.requestType = requestType; }
    public PaymentRequestSourceType getSourceType() { return sourceType; }
    public void setSourceType(PaymentRequestSourceType sourceType) { this.sourceType = sourceType; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getPayeePartnerId() { return payeePartnerId; }
    public void setPayeePartnerId(String payeePartnerId) { this.payeePartnerId = payeePartnerId; }
    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) { this.externalReference = externalReference; }
    public String getPaymentReason() { return paymentReason; }
    public void setPaymentReason(String paymentReason) { this.paymentReason = paymentReason; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDate getRequestedPaymentDate() { return requestedPaymentDate; }
    public void setRequestedPaymentDate(LocalDate requestedPaymentDate) { this.requestedPaymentDate = requestedPaymentDate; }
    public PaymentRequestStatus getStatus() { return status; }
    public void setStatus(PaymentRequestStatus status) { this.status = status; }
    public String getApprovalRequestId() { return approvalRequestId; }
    public void setApprovalRequestId(String approvalRequestId) { this.approvalRequestId = approvalRequestId; }
    public LocalDate getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDate paidDate) { this.paidDate = paidDate; }
    public String getPaidReference() { return paidReference; }
    public void setPaidReference(String paidReference) { this.paidReference = paidReference; }
    public String getPaidBy() { return paidBy; }
    public void setPaidBy(String paidBy) { this.paidBy = paidBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
