package za.co.mawa.bes.dto.v2.group;

import java.time.LocalDate;

public class GroupSocietyAdjustmentRequest {

    private Long amountCents;
    private LocalDate adjustmentDate;
    private String direction;
    private String referenceNo;
    private String notes;

    public Long getAmountCents() {
        return amountCents;
    }

    public LocalDate getAdjustmentDate() {
        return adjustmentDate;
    }

    public String getDirection() {
        return direction;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public String getNotes() {
        return notes;
    }
}