package za.co.mawa.bes.dto.v2.membership.claim;

import za.co.mawa.bes.enums.MembershipClaimDeceasedType;
import za.co.mawa.bes.enums.MembershipClaimType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MembershipClaimCreateRequest {

    private String membershipId;

    private MembershipClaimType claimType;

    private MembershipClaimDeceasedType deceasedType;

    private String deceasedPartnerId;

    private LocalDate dateOfDeath;

    private LocalDate claimDate;

    private String causeOfDeath;

    private String deathCertificateNo;

    private String claimantPartnerId;

    private Long claimAmountCents;

    private String notes;

    private Boolean submit;

    private List<String> linkedClaimIds = new ArrayList<>();

    public String getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(String membershipId) {
        this.membershipId = membershipId;
    }

    public MembershipClaimType getClaimType() {
        return claimType;
    }

    public void setClaimType(MembershipClaimType claimType) {
        this.claimType = claimType;
    }

    public MembershipClaimDeceasedType getDeceasedType() {
        return deceasedType;
    }

    public void setDeceasedType(MembershipClaimDeceasedType deceasedType) {
        this.deceasedType = deceasedType;
    }

    public String getDeceasedPartnerId() {
        return deceasedPartnerId;
    }

    public void setDeceasedPartnerId(String deceasedPartnerId) {
        this.deceasedPartnerId = deceasedPartnerId;
    }

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

    public Boolean getSubmit() {
        return submit;
    }

    public void setSubmit(Boolean submit) {
        this.submit = submit;
    }

    public List<String> getLinkedClaimIds() {
        return linkedClaimIds;
    }

    public void setLinkedClaimIds(List<String> linkedClaimIds) {
        this.linkedClaimIds = linkedClaimIds;
    }
}