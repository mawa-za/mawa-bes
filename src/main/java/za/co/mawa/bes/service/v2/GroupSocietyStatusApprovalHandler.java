package za.co.mawa.bes.service.v2;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;

@Component
public class GroupSocietyStatusApprovalHandler implements ApprovalCompletionHandler {

    private final ObjectProvider<GroupSocietyApprovalService> serviceProvider;

    public GroupSocietyStatusApprovalHandler(ObjectProvider<GroupSocietyApprovalService> serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Override
    public ApprovalType supports() {
        return ApprovalType.GROUP_SOCIETY_STATUS_CHANGE;
    }

    @Override
    public void onApproved(ApprovalRequestEntity request, String actor) {
        serviceProvider.getObject().completeStatus(request.getReferenceId(), true, actor);
    }

    @Override
    public void onRejected(ApprovalRequestEntity request, String actor) {
        serviceProvider.getObject().completeStatus(request.getReferenceId(), false, actor);
    }

    @Override
    public void onCancelled(ApprovalRequestEntity request, String actor) {
        serviceProvider.getObject().completeStatus(request.getReferenceId(), false, actor);
    }
}
