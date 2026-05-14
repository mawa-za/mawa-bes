package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "group_society_account_txn")
public class GroupSocietyAccountTxnEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "group_society_id", nullable = false)
    private String groupSocietyId;

    @Column(name = "txn_type", nullable = false)
    private String txnType;

    @Column(nullable = false)
    private String direction;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "balance_before_cents", nullable = false)
    private Long balanceBeforeCents;

    @Column(name = "balance_after_cents", nullable = false)
    private Long balanceAfterCents;

    @Column(name = "txn_date", nullable = false)
    private LocalDate txnDate;

    @Column(name = "txn_datetime", insertable = false, updatable = false)
    private Date txnDatetime;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "reference_no")
    private String referenceNo;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "period")
    private String period;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Date createdAt;

    @Column(name = "created_by")
    private String createdBy;

    public String getId() {
        return id;
    }

    public String getGroupSocietyId() {
        return groupSocietyId;
    }

    public void setGroupSocietyId(String groupSocietyId) {
        this.groupSocietyId = groupSocietyId;
    }

    public String getTxnType() {
        return txnType;
    }

    public void setTxnType(String txnType) {
        this.txnType = txnType;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(Long amountCents) {
        this.amountCents = amountCents;
    }

    public Long getBalanceBeforeCents() {
        return balanceBeforeCents;
    }

    public void setBalanceBeforeCents(Long balanceBeforeCents) {
        this.balanceBeforeCents = balanceBeforeCents;
    }

    public Long getBalanceAfterCents() {
        return balanceAfterCents;
    }

    public void setBalanceAfterCents(Long balanceAfterCents) {
        this.balanceAfterCents = balanceAfterCents;
    }

    public LocalDate getTxnDate() {
        return txnDate;
    }

    public void setTxnDate(LocalDate txnDate) {
        this.txnDate = txnDate;
    }

    public Date getTxnDatetime() {
        return txnDatetime;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}