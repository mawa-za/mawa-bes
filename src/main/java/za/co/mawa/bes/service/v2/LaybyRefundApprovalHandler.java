package za.co.mawa.bes.service.v2;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;

@Component
public class LaybyRefundApprovalHandler implements ApprovalCompletionHandler {
    private final ObjectProvider<LaybyManagementService> serviceProvider;

    public LaybyRefundApprovalHandler(ObjectProvider<LaybyManagementService> serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Override
    public ApprovalType supports() {
        return ApprovalType.LAYBY_REFUND;
    }

    @Override
    public void onApproved(ApprovalRequestEntity request, String actor) {
        serviceProvider.getObject().completeRefundApprovalByReferenceId(
                request.getReferenceId(), true, actor, "Refund approved");
    }

    @Override
    public void onRejected(ApprovalRequestEntity request, String actor) {
        serviceProvider.getObject().completeRefundApprovalByReferenceId(
                request.getReferenceId(), false, actor, "Refund rejected");
    }

    @Override
    public void onCancelled(ApprovalRequestEntity request, String actor) {
        serviceProvider.getObject().completeRefundApprovalByReferenceId(
                request.getReferenceId(), false, actor, "Refund approval request cancelled");
    }
}
