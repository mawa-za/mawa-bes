package za.co.mawa.bes.dto.v2.membership.claim;

import za.co.mawa.bes.enums.MembershipClaimDeceasedType;
import za.co.mawa.bes.enums.MembershipClaimStatus;
import za.co.mawa.bes.enums.MembershipClaimType;
import za.co.mawa.bes.enums.PaymentMethod;
import za.co.mawa.bes.enums.BankAccountType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MembershipClaimResponse {

    private String id;
    private String claimNo;
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
    private Long combinedClaimAmountCents;

    private MembershipClaimStatus status;

    private String rejectionReason;
    private String notes;

    private Boolean parentCombinationClaim;
    private Boolean linkedToCombinationClaim;

    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private PaymentMethod payoutMethod;

    private String approvalRequestId;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String paymentRequestId;

    private List<LinkedMembershipClaimResponse> linkedClaims = new ArrayList<>();

    // New fields for bank details
    private String bankName;
    private String accountHolderName;
    private String accountNumber;
    private String branchCode;
    private BankAccountType accountType;

    public String getApprovalRequestId() {
        return approvalRequestId;
    }
    public MembershipClaimResponse setApprovalRequestId(String approvalRequestId) {
        this.approvalRequestId = approvalRequestId;
        return this;
    }
    public String getApprovedBy() {
        return approvedBy;
    }
    public MembershipClaimResponse setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
        return this;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }
    public MembershipClaimResponse setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
        return this;
    }
    public String getPaymentRequestId() {
        return paymentRequestId;
    }
    public MembershipClaimResponse setPaymentRequestId(String paymentRequestId) {
        this.paymentRequestId = paymentRequestId;
        return this;
    }
    // Getters and setters for bank details

    public String getBankName() {
        return bankName;
    }

    public MembershipClaimResponse setBankName(String bankName) {
        this.bankName = bankName;
        return this;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public MembershipClaimResponse setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
        return this;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public MembershipClaimResponse setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
        return this;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public MembershipClaimResponse setBranchCode(String branchCode) {
        this.branchCode = branchCode;
        return this;
    }

    public BankAccountType getAccountType() {
        return accountType;
    }

    public MembershipClaimResponse setAccountType(BankAccountType accountType) {
        this.accountType = accountType;
        return this;
    }

    // Existing getters and setters remain unchanged

    public PaymentMethod getPayoutMethod() {
        return payoutMethod;
    }

    public MembershipClaimResponse setPayoutMethod(PaymentMethod payoutMethod) {
        this.payoutMethod = payoutMethod;
        return this;
    }

    public String getId() {
        return id;
    }

    public MembershipClaimResponse setId(String id) {
        this.id = id;
        return this;
    }

    public String getClaimNo() {
        return claimNo;
    }

    public MembershipClaimResponse setClaimNo(String claimNo) {
        this.claimNo = claimNo;
        return this;
    }

    public String getMembershipId() {
        return membershipId;
    }

    public MembershipClaimResponse setMembershipId(String membershipId) {
        this.membershipId = membershipId;
        return this;
    }

    public MembershipClaimType getClaimType() {
        return claimType;
    }

    public MembershipClaimResponse setClaimType(MembershipClaimType claimType) {
        this.claimType = claimType;
        return this;
    }

    public MembershipClaimDeceasedType getDeceasedType() {
        return deceasedType;
    }

    public MembershipClaimResponse setDeceasedType(MembershipClaimDeceasedType deceasedType) {
        this.deceasedType = deceasedType;
        return this;
    }

    public String getDeceasedPartnerId() {
        return deceasedPartnerId;
    }

    public MembershipClaimResponse setDeceasedPartnerId(String deceasedPartnerId) {
        this.deceasedPartnerId = deceasedPartnerId;
        return this;
    }

    public LocalDate getDateOfDeath() {
        return dateOfDeath;
    }

    public MembershipClaimResponse setDateOfDeath(LocalDate dateOfDeath) {
        this.dateOfDeath = dateOfDeath;
        return this;
    }

    public LocalDate getClaimDate() {
        return claimDate;
    }

    public MembershipClaimResponse setClaimDate(LocalDate claimDate) {
        this.claimDate = claimDate;
        return this;
    }

    public String getCauseOfDeath() {
        return causeOfDeath;
    }

    public MembershipClaimResponse setCauseOfDeath(String causeOfDeath) {
        this.causeOfDeath = causeOfDeath;
        return this;
    }

    public String getDeathCertificateNo() {
        return deathCertificateNo;
    }

    public MembershipClaimResponse setDeathCertificateNo(String deathCertificateNo) {
        this.deathCertificateNo = deathCertificateNo;
        return this;
    }

    public String getClaimantPartnerId() {
        return claimantPartnerId;
    }

    public MembershipClaimResponse setClaimantPartnerId(String claimantPartnerId) {
        this.claimantPartnerId = claimantPartnerId;
        return this;
    }

    public Long getClaimAmountCents() {
        return claimAmountCents;
    }

    public MembershipClaimResponse setClaimAmountCents(Long claimAmountCents) {
        this.claimAmountCents = claimAmountCents;
        return this;
    }

    public Long getCombinedClaimAmountCents() {
        return combinedClaimAmountCents;
    }

    public MembershipClaimResponse setCombinedClaimAmountCents(Long combinedClaimAmountCents) {
        this.combinedClaimAmountCents = combinedClaimAmountCents;
        return this;
    }

    public MembershipClaimStatus getStatus() {
        return status;
    }

    public MembershipClaimResponse setStatus(MembershipClaimStatus status) {
        this.status = status;
        return this;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public MembershipClaimResponse setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
        return this;
    }

    public String getNotes() {
        return notes;
    }

    public MembershipClaimResponse setNotes(String notes) {
        this.notes = notes;
        return this;
    }

    public Boolean getParentCombinationClaim() {
        return parentCombinationClaim;
    }

    public MembershipClaimResponse setParentCombinationClaim(Boolean parentCombinationClaim) {
        this.parentCombinationClaim = parentCombinationClaim;
        return this;
    }

    public Boolean getLinkedToCombinationClaim() {
        return linkedToCombinationClaim;
    }

    public MembershipClaimResponse setLinkedToCombinationClaim(Boolean linkedToCombinationClaim) {
        this.linkedToCombinationClaim = linkedToCombinationClaim;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public MembershipClaimResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public MembershipClaimResponse setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public MembershipClaimResponse setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public MembershipClaimResponse setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
        return this;
    }

    public List<LinkedMembershipClaimResponse> getLinkedClaims() {
        return linkedClaims;
    }

    public MembershipClaimResponse setLinkedClaims(List<LinkedMembershipClaimResponse> linkedClaims) {
        this.linkedClaims = linkedClaims;
        return this;
    }
}