package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.MembershipClaimDeceasedType;
import za.co.mawa.bes.enums.MembershipClaimStatus;
import za.co.mawa.bes.enums.MembershipClaimType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "membership_claim")
public class MembershipClaimEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(name = "id", length = 255)
    private String id;

    @Column(name = "claim_no", nullable = false, unique = true, length = 50)
    private String claimNo;

    @Column(name = "membership_id", nullable = false, length = 255)
    private String membershipId;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_type", nullable = false, length = 40)
    private MembershipClaimType claimType;

    @Enumerated(EnumType.STRING)
    @Column(name = "deceased_type", nullable = false, length = 30)
    private MembershipClaimDeceasedType deceasedType;

    @Column(name = "deceased_partner_id", nullable = false, length = 255)
    private String deceasedPartnerId;

    @Column(name = "date_of_death", nullable = false)
    private LocalDate dateOfDeath;

    @Column(name = "claim_date", nullable = false)
    private LocalDate claimDate;

    @Column(name = "cause_of_death", length = 255)
    private String causeOfDeath;

    @Column(name = "death_certificate_no", length = 100)
    private String deathCertificateNo;

    @Column(name = "claimant_partner_id", length = 255)
    private String claimantPartnerId;

    @Column(name = "claim_amount_cents", nullable = false)
    private Long claimAmountCents = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MembershipClaimStatus status = MembershipClaimStatus.DRAFT;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();

        if (this.claimDate == null) {
            this.claimDate = LocalDate.now();
        }

        if (this.status == null) {
            this.status = MembershipClaimStatus.DRAFT;
        }

        if (this.claimAmountCents == null) {
            this.claimAmountCents = 0L;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getClaimNo() {
        return claimNo;
    }

    public void setClaimNo(String claimNo) {
        this.claimNo = claimNo;
    }

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

    public MembershipClaimStatus getStatus() {
        return status;
    }

    public void setStatus(MembershipClaimStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}