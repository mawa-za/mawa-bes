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

    public LocalDate getClaimDate() {
        return claimDate;
    }

    public String getClaimId() {
        return claimId;
    }

    public String getClaimNo() {
        return claimNo;
    }

    public String getNotes() {
        return notes;
    }
}