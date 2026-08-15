package za.co.mawa.bes.enums;

public enum ApprovalType {
    CLAIM,
    CLAIM_CASH,
    CLAIM_TOMBSTONE,
    CLAIM_FUNERAL,
    CLAIM_COMBINATION,
    CLAIM_GROCERY,
    PAYMENT,
    LEAVE,
    CASHUP,
    INVOICE,
    PURCHASE_ORDER,
    JOURNAL,
    PAYMENT_REQUEST,
    SUPPLIER_INVOICE,
    CUSTOMER_REFUND,
    SUPPLIER_ONBOARDING,
    SUPPLIER_BANKING_DETAILS,
    MEMBERSHIP_TRANSFER,
    MEMBERSHIP_PLAN_CHANGE,
    MEMBERSHIP_DEPENDENT_CHANGE,
    ADDITIONAL_MEMBERSHIP,
    PAYROLL_BATCH,
    EMPLOYEE_BANKING_DETAILS,
    EMPLOYEE_HIRE,
    EMPLOYEE_SUSPENSION,
    EMPLOYEE_TERMINATION,
    EMPLOYEE_REHIRE,
    EMPLOYEE_REINSTATEMENT,
    LEAVE_BALANCE_ADJUSTMENT,
    FUNERAL_UNDERWRITING,
    FUNERAL_COVER_STATUS_CHANGE,
    GROUP_SOCIETY_STATUS_CHANGE,
    GROUP_SOCIETY_BALANCE_ADJUSTMENT,
    GROUP_SOCIETY_FUNERAL_CLAIM,
    PREMIUM_PAYMENT_DELETION;

    public boolean isMembershipClaimApproval() {
        return this == CLAIM
                || this == CLAIM_CASH
                || this == CLAIM_TOMBSTONE
                || this == CLAIM_FUNERAL
                || this == CLAIM_COMBINATION
                || this == CLAIM_GROCERY;
    }

    public static ApprovalType forMembershipClaimType(MembershipClaimType claimType) {
        if (claimType == null) return CLAIM;
        return switch (claimType) {
            case CASH -> CLAIM_CASH;
            case TOMBSTONE -> CLAIM_TOMBSTONE;
            case FUNERAL -> CLAIM_FUNERAL;
            case COMBINATION -> CLAIM_COMBINATION;
            case GROCERY -> CLAIM_GROCERY;
        };
    }
}

