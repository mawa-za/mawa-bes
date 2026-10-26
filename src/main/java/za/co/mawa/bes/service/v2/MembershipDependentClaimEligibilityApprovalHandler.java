package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;

/** Routes only MEMBERSHIP_DEPENDENT_CLAIM_ELIGIBILITY decisions to the membership change application. */
@Component
@RequiredArgsConstructor
public class MembershipDependentClaimEligibilityApprovalHandler implements ApprovalCompletionHandler {
    private final MembershipChangeService service;

    @Override
    public ApprovalType supports() {
        return ApprovalType.MEMBERSHIP_DEPENDENT_CLAIM_ELIGIBILITY;
    }

    @Override
    public void onApproved(ApprovalRequestEntity request, String actor) {
        service.approved(request.getReferenceId(), actor);
    }

    @Override
    public void onRejected(ApprovalRequestEntity request, String actor) {
        service.rejected(request.getReferenceId(), actor, false);
    }

    @Override
    public void onCancelled(ApprovalRequestEntity request, String actor) {
        service.rejected(request.getReferenceId(), actor, true);
    }
}
