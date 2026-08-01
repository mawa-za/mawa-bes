package za.co.mawa.bes.dto.v2.group;

import java.time.LocalDate;

public class GroupSocietyClaimDebitRequest {

    private Long amountCents;
    private LocalDate claimDate;
    private String claimId;
    private String claimNo;
    private String notes;

    public Long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(Long amountCents) {
        this.amountCents = amountCents;
    }

    public LocalDate getClaimDate() {
        return claimDate;
    }

    public void setClaimDate(LocalDate claimDate) {
        this.claimDate = claimDate;
    }

    public String getClaimId() {
        return claimId;
    }

    public void setClaimId(String claimId) {
        this.claimId = claimId;
    }

    public String getClaimNo() {
        return claimNo;
    }

    public void setClaimNo(String claimNo) {
        this.claimNo = claimNo;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
