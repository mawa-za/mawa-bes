package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.v2.membership.claim.MembershipClaimResponse;
import za.co.mawa.bes.dto.v2.payment.PaymentRequestResponse;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.enums.MembershipClaimType;


@Component
@RequiredArgsConstructor
public class MembershipClaimApprovalHandler implements ApprovalCompletionHandler, ApprovalSubmissionHandler {
    @Autowired
    PaymentRequestService paymentRequestService;
    @Autowired
    MembershipClaimService membershipClaimService;
    @Autowired
    FuneralClaimSettlementService funeralClaimSettlementService;

    @Override
    public ApprovalType supports() {
        return ApprovalType.CLAIM;
    }

    @Override
    public void onSubmit(ApprovalRequestEntity approvalRequest, String actionBy) {
        membershipClaimService.submit(approvalRequest.getReferenceId(), actionBy);
        membershipClaimService.linkApproval(approvalRequest, actionBy);
    }

    @Override
    public void onApproved(ApprovalRequestEntity approvalRequest, String actionBy) {
        MembershipClaimResponse claim = membershipClaimService
                .markApprovedFromWorkflow(approvalRequest.getReferenceId(), actionBy);

        if (claim == null) {
            return;
        }

        if (claim.getClaimType() == MembershipClaimType.FUNERAL
                || claim.getClaimType() == MembershipClaimType.COMBINATION) {
            PaymentRequestResponse paymentRequest = funeralClaimSettlementService
                    .settleApprovedClaim(claim.getId(), actionBy);
            if (paymentRequest == null || paymentRequest.getId() == null || paymentRequest.getId().isBlank()) {
                throw new IllegalStateException(
                        "Approved funeral claim did not create or reuse a payment request: " + claim.getId());
            }
            membershipClaimService.linkPaymentRequest(paymentRequest, actionBy);
            return;
        }

        if (claim.getClaimType() == MembershipClaimType.CASH
                || claim.getClaimType() == MembershipClaimType.GROCERY) {
            PaymentRequestResponse paymentRequest = paymentRequestService
                    .createOrReuseApprovedClaimPayout(claim, approvalRequest, actionBy);
            membershipClaimService.linkPaymentRequest(paymentRequest, actionBy);
        }
    }
}
