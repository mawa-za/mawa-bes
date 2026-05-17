package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
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

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_method", length = 20)
    private PaymentMethod payoutMethod;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_holder_name", length = 150)
    private String accountHolderName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 30)
    private BankAccountType accountType;

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
}