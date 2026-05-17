package za.co.mawa.bes.dto.v2.payment;

import za.co.mawa.bes.enums.PaymentMethod;
import za.co.mawa.bes.enums.PaymentRequestSourceType;
import za.co.mawa.bes.enums.PaymentRequestStatus;
import za.co.mawa.bes.enums.PaymentRequestType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PaymentRequestResponse {

    private String id;
    private String requestNo;
    private PaymentRequestType requestType;
    private PaymentRequestSourceType sourceType;
    private String sourceId;
    private String payeePartnerId;
    private String payeeName;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private String bankName;
    private String accountHolder;
    private String accountNumber;
    private String branchCode;
    private String accountType;
    private String invoiceNo;
    private String externalReference;
    private String paymentReason;
    private String notes;
    private LocalDate requestedPaymentDate;
    private PaymentRequestStatus status;
    private String approvalRequestId;
    private LocalDate paidDate;
    private String paidReference;
    private String paidBy;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public String getId() { return id; }
    public PaymentRequestResponse setId(String id) { this.id = id; return this; }
    public String getRequestNo() { return requestNo; }
    public PaymentRequestResponse setRequestNo(String requestNo) { this.requestNo = requestNo; return this; }
    public PaymentRequestType getRequestType() { return requestType; }
    public PaymentRequestResponse setRequestType(PaymentRequestType requestType) { this.requestType = requestType; return this; }
    public PaymentRequestSourceType getSourceType() { return sourceType; }
    public PaymentRequestResponse setSourceType(PaymentRequestSourceType sourceType) { this.sourceType = sourceType; return this; }
    public String getSourceId() { return sourceId; }
    public PaymentRequestResponse setSourceId(String sourceId) { this.sourceId = sourceId; return this; }
    public String getPayeePartnerId() { return payeePartnerId; }
    public PaymentRequestResponse setPayeePartnerId(String payeePartnerId) { this.payeePartnerId = payeePartnerId; return this; }
    public String getPayeeName() { return payeeName; }
    public PaymentRequestResponse setPayeeName(String payeeName) { this.payeeName = payeeName; return this; }
    public BigDecimal getAmount() { return amount; }
    public PaymentRequestResponse setAmount(BigDecimal amount) { this.amount = amount; return this; }
    public String getCurrency() { return currency; }
    public PaymentRequestResponse setCurrency(String currency) { this.currency = currency; return this; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentRequestResponse setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; return this; }
    public String getBankName() { return bankName; }
    public PaymentRequestResponse setBankName(String bankName) { this.bankName = bankName; return this; }
    public String getAccountHolder() { return accountHolder; }
    public PaymentRequestResponse setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; return this; }
    public String getAccountNumber() { return accountNumber; }
    public PaymentRequestResponse setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; return this; }
    public String getBranchCode() { return branchCode; }
    public PaymentRequestResponse setBranchCode(String branchCode) { this.branchCode = branchCode; return this; }
    public String getAccountType() { return accountType; }
    public PaymentRequestResponse setAccountType(String accountType) { this.accountType = accountType; return this; }
    public String getInvoiceNo() { return invoiceNo; }
    public PaymentRequestResponse setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; return this; }
    public String getExternalReference() { return externalReference; }
    public PaymentRequestResponse setExternalReference(String externalReference) { this.externalReference = externalReference; return this; }
    public String getPaymentReason() { return paymentReason; }
    public PaymentRequestResponse setPaymentReason(String paymentReason) { this.paymentReason = paymentReason; return this; }
    public String getNotes() { return notes; }
    public PaymentRequestResponse setNotes(String notes) { this.notes = notes; return this; }
    public LocalDate getRequestedPaymentDate() { return requestedPaymentDate; }
    public PaymentRequestResponse setRequestedPaymentDate(LocalDate requestedPaymentDate) { this.requestedPaymentDate = requestedPaymentDate; return this; }
    public PaymentRequestStatus getStatus() { return status; }
    public PaymentRequestResponse setStatus(PaymentRequestStatus status) { this.status = status; return this; }
    public String getApprovalRequestId() { return approvalRequestId; }
    public PaymentRequestResponse setApprovalRequestId(String approvalRequestId) { this.approvalRequestId = approvalRequestId; return this; }
    public LocalDate getPaidDate() { return paidDate; }
    public PaymentRequestResponse setPaidDate(LocalDate paidDate) { this.paidDate = paidDate; return this; }
    public String getPaidReference() { return paidReference; }
    public PaymentRequestResponse setPaidReference(String paidReference) { this.paidReference = paidReference; return this; }
    public String getPaidBy() { return paidBy; }
    public PaymentRequestResponse setPaidBy(String paidBy) { this.paidBy = paidBy; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public PaymentRequestResponse setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
    public String getCreatedBy() { return createdBy; }
    public PaymentRequestResponse setCreatedBy(String createdBy) { this.createdBy = createdBy; return this; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public PaymentRequestResponse setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
    public String getUpdatedBy() { return updatedBy; }
    public PaymentRequestResponse setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; return this; }
}
