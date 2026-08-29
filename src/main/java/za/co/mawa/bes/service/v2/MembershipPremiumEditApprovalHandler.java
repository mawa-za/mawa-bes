package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;

@Component
@RequiredArgsConstructor
public class MembershipPremiumEditApprovalHandler implements ApprovalCompletionHandler {
    private final MembershipPremiumEditService service;

    @Override
    public ApprovalType supports() {
        return ApprovalType.MEMBERSHIP_PREMIUM_EDIT;
    }

    @Override
    public void onApproved(ApprovalRequestEntity request, String actor) {
        service.complete(request.getReferenceId(), true, actor, "APPROVED");
    }

    @Override
    public void onRejected(ApprovalRequestEntity request, String actor) {
        service.complete(request.getReferenceId(), false, actor, "REJECTED");
    }

    @Override
    public void onCancelled(ApprovalRequestEntity request, String actor) {
        service.complete(request.getReferenceId(), false, actor, "CANCELLED");
    }
}
