package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;

@Component
@RequiredArgsConstructor
public class PremiumPaymentDeletionApprovalHandler implements ApprovalCompletionHandler {
    private final MembershipPremiumPaymentService membershipPremiumPaymentService;

    @Override
    public ApprovalType supports() {
        return ApprovalType.PREMIUM_PAYMENT_DELETION;
    }

    @Override
    public void onApproved(ApprovalRequestEntity approvalRequest, String actionBy) {
        membershipPremiumPaymentService.reverseApprovedPayment(
                approvalRequest.getReferenceId(),
                actionBy,
                approvalRequest.getDescription());
    }
}
