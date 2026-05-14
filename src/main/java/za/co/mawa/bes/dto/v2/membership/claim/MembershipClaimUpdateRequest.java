package za.co.mawa.bes.dto.v2.membership.claim;

import java.time.LocalDate;

public class MembershipClaimUpdateRequest {

    private LocalDate dateOfDeath;

    private LocalDate claimDate;

    private String causeOfDeath;

    private String deathCertificateNo;

    private String claimantPartnerId;

    private Long claimAmountCents;

    private String notes;

    public LocalDate getDateOfDeath() {
        return dateOfDeath;
    }

    public void setDateOfDeath(LocalDate dateOfDeath) {
        this.dateOfDeath = dateOfDeath;
    }

    public LocalDate getClaimDate() {
        return claimDate;
    }

    public void setClaimDate(LocalDate claimDate) {
        this.claimDate = claimDate;
    }

    public String getCauseOfDeath() {
        return causeOfDeath;
    }

    public void setCauseOfDeath(String causeOfDeath) {
        this.causeOfDeath = causeOfDeath;
    }

    public String getDeathCertificateNo() {
        return deathCertificateNo;
    }

    public void setDeathCertificateNo(String deathCertificateNo) {
        this.deathCertificateNo = deathCertificateNo;
    }

    public String getClaimantPartnerId() {
        return claimantPartnerId;
    }

    public void setClaimantPartnerId(String claimantPartnerId) {
        this.claimantPartnerId = claimantPartnerId;
    }

    public Long getClaimAmountCents() {
        return claimAmountCents;
    }

    public void setClaimAmountCents(Long claimAmountCents) {
        this.claimAmountCents = claimAmountCents;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}