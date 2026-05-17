package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.v2.membership.claim.MembershipClaimResponse;
import za.co.mawa.bes.dto.v2.payment.PaymentRequestCreateRequest;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.enums.MembershipClaimType;
import za.co.mawa.bes.enums.PaymentRequestSourceType;
import za.co.mawa.bes.enums.PaymentRequestType;

@Component
@RequiredArgsConstructor
public class MembershipClaimApprovalHandler implements ApprovalCompletionHandler {
    @Autowired
    PaymentRequestService paymentRequestService;

     MembershipClaimService membershipClaimService;

    @Override
    public ApprovalType supports() {
        return ApprovalType.CLAIM;
    }

    @Override
    public void onApproved(ApprovalRequestEntity approvalRequest, String actionBy) {
        MembershipClaimResponse membershipClaimResponse = membershipClaimService.getById(approvalRequest.getReferenceId());
        if (membershipClaimResponse == null) {
            if (membershipClaimResponse.getClaimType().equals(MembershipClaimType.CASH)) {
                PaymentRequestCreateRequest paymentRequestCreateRequest = new PaymentRequestCreateRequest();
                paymentRequestCreateRequest.setRequestType(PaymentRequestType.CLAIM_PAYOUT);
                paymentRequestCreateRequest.setSourceType(PaymentRequestSourceType.MEMBERSHIP_CLAIM);
                paymentRequestCreateRequest.setSourceId(membershipClaimResponse.getId());
                paymentRequestCreateRequest.setPaymentMethod(membershipClaimResponse.getPayoutMethod());
                paymentRequestCreateRequest.setPayeePartnerId(membershipClaimResponse.getClaimantPartnerId());
//                paymentRequestCreateRequest.setPayeeName(membershipClaimResponse.getClaimantName());
//                paymentRequestCreateRequest.setAmount(membershipClaimResponse.getAmount());
                paymentRequestCreateRequest.setPaymentReason("CASH-CLAIM-PAYOUT");
                paymentRequestCreateRequest.setExternalReference("CLAIM-" + membershipClaimResponse.getClaimNo());
                paymentRequestCreateRequest.setAccountHolder(membershipClaimResponse.getAccountHolderName());
                paymentRequestCreateRequest.setAccountNumber(membershipClaimResponse.getAccountNumber());
                paymentRequestCreateRequest.setAccountType(membershipClaimResponse.getAccountType().toString());
                paymentRequestCreateRequest.setBankName(membershipClaimResponse.getBankName());
                paymentRequestCreateRequest.setBranchCode(membershipClaimResponse.getBranchCode());
                paymentRequestService.create(paymentRequestCreateRequest, actionBy);
            }
        }
    }
}
