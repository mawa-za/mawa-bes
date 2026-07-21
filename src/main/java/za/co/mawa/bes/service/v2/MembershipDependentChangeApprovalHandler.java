package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;

@Component
@RequiredArgsConstructor
public class MembershipDependentChangeApprovalHandler implements ApprovalCompletionHandler {
    private final MembershipChangeService service;

    @Override
    public ApprovalType supports() {
        return ApprovalType.MEMBERSHIP_DEPENDENT_CHANGE;
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
