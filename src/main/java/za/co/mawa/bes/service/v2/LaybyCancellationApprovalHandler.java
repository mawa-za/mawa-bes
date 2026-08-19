package za.co.mawa.bes.service.v2;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;

@Component
public class LaybyCancellationApprovalHandler implements ApprovalCompletionHandler {
    private final ObjectProvider<LaybyManagementService> serviceProvider;

    public LaybyCancellationApprovalHandler(ObjectProvider<LaybyManagementService> serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Override
    public ApprovalType supports() {
        return ApprovalType.LAYBY_CANCELLATION;
    }

    @Override
    public void onApproved(ApprovalRequestEntity request, String actor) {
        serviceProvider.getObject().completeCancellationApprovalByRequestId(
                request.getReferenceId(), true, actor, "Cancellation approved");
    }

    @Override
    public void onRejected(ApprovalRequestEntity request, String actor) {
        serviceProvider.getObject().completeCancellationApprovalByRequestId(
                request.getReferenceId(), false, actor, "Cancellation rejected");
    }

    @Override
    public void onCancelled(ApprovalRequestEntity request, String actor) {
        serviceProvider.getObject().completeCancellationApprovalByRequestId(
                request.getReferenceId(), false, actor, "Cancellation approval request cancelled");
    }
}
