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
}