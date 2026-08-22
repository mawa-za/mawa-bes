package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;

@Component
@RequiredArgsConstructor
public class MembershipMergeApprovalHandler implements ApprovalCompletionHandler {
    private final MembershipChangeService service;
    public ApprovalType supports() { return ApprovalType.MEMBERSHIP_MERGE; }
    public void onApproved(ApprovalRequestEntity request, String actor) { service.approved(request.getReferenceId(), actor); }
    public void onRejected(ApprovalRequestEntity request, String actor) { service.rejected(request.getReferenceId(), actor, false); }
    public void onCancelled(ApprovalRequestEntity request, String actor) { service.rejected(request.getReferenceId(), actor, true); }
}
