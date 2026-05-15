package za.co.mawa.bes.enums;

public enum MembershipClaimStatus {
    DRAFT,
    SUBMITTED,
    CANCELLED,

    // Updated by approval module
    APPROVED,
    REJECTED,

    // Updated by payment module
    PAID
}
