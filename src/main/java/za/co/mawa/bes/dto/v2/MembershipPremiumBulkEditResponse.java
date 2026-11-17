package za.co.mawa.bes.dto.v2;

import java.util.List;

/** Approvals are submitted as a single transaction, but each row is independently approvable. */
public record MembershipPremiumBulkEditResponse(
        String batchId, int submittedCount, List<ApprovalLine> approvals) {
    public record ApprovalLine(String membershipId, String premiumId, String approvalRequestId) { }
}
