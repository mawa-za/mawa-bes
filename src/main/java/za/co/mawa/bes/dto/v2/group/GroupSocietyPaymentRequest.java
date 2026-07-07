package za.co.mawa.bes.dto.v2.group;

import java.time.LocalDate;

public class GroupSocietyPaymentRequest {

    private Long amountCents;
    private LocalDate paymentDate;
    private String paymentMethod;
    private String period;
    private String referenceId;
    private String referenceNo;
    private String notes;

    public Long getAmountCents() {
        return amountCents;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getPeriod() {
        return period;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public String getNotes() {
        return notes;
    }

    public void setAmountCents(Long amountCents) {
        this.amountCents = amountCents;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

}
