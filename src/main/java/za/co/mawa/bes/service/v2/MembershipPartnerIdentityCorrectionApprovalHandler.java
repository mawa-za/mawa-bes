package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;

@Component
@RequiredArgsConstructor
public class MembershipPartnerIdentityCorrectionApprovalHandler implements ApprovalCompletionHandler {
    private final MembershipPartnerIdentityCorrectionService service;
    public ApprovalType supports() { return ApprovalType.MEMBERSHIP_PARTNER_IDENTITY_CORRECTION; }
    public void onApproved(ApprovalRequestEntity request, String actor) { service.complete(request.getReferenceId(), true, actor, "APPROVED"); }
    public void onRejected(ApprovalRequestEntity request, String actor) { service.complete(request.getReferenceId(), false, actor, "REJECTED"); }
    public void onCancelled(ApprovalRequestEntity request, String actor) { service.complete(request.getReferenceId(), false, actor, "CANCELLED"); }
}
