package za.co.mawa.bes.service.v2;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;

@Component
public class MembershipStatusChangeApprovalHandler implements ApprovalCompletionHandler {
    private final ObjectProvider<MembershipStatusChangeService> serviceProvider;

    public MembershipStatusChangeApprovalHandler(ObjectProvider<MembershipStatusChangeService> serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Override
    public ApprovalType supports() {
        return ApprovalType.MEMBERSHIP_STATUS_CHANGE;
    }

    @Override
    public void onApproved(ApprovalRequestEntity request, String actor) {
        serviceProvider.getObject().complete(request.getReferenceId(), true, actor, "APPROVED");
    }

    @Override
    public void onRejected(ApprovalRequestEntity request, String actor) {
        serviceProvider.getObject().complete(request.getReferenceId(), false, actor, "REJECTED");
    }

    @Override
    public void onCancelled(ApprovalRequestEntity request, String actor) {
        serviceProvider.getObject().complete(request.getReferenceId(), false, actor, "CANCELLED");
    }
}
