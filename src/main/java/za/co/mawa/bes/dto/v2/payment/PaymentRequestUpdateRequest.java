package za.co.mawa.bes.dto.v2.payment;

import za.co.mawa.bes.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PaymentRequestUpdateRequest {

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
}
