package za.co.mawa.bes.service.v2;

import org.springframework.beans.factory.ObjectProvider;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;

public abstract class AbstractInventoryApprovalHandler implements ApprovalCompletionHandler, ApprovalSubmissionHandler {
    private final ObjectProvider<InventoryManagementService> serviceProvider;

    protected AbstractInventoryApprovalHandler(ObjectProvider<InventoryManagementService> serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Override
    public void onSubmit(ApprovalRequestEntity request, String actor) {
        serviceProvider.getObject().markApprovalSubmitted(request.getReferenceId(), request.getId(), actor);
    }

    @Override
    public void onApproved(ApprovalRequestEntity request, String actor) {
        if (actor != null && request.getRequesterId() != null && actor.equalsIgnoreCase(request.getRequesterId())) {
            throw new IllegalStateException("Inventory approval requires dual control; the requester cannot approve their own inventory transaction");
        }
        serviceProvider.getObject().completeApproval(request.getReferenceId(), true, actor, "Approved");
    }

    @Override
    public void onRejected(ApprovalRequestEntity request, String actor, String reason) {
        serviceProvider.getObject().completeApproval(request.getReferenceId(), false, actor, reason);
    }

    @Override
    public void onRejected(ApprovalRequestEntity request, String actor) {
        serviceProvider.getObject().completeApproval(request.getReferenceId(), false, actor, "Rejected");
    }

    @Override
    public void onCancelled(ApprovalRequestEntity request, String actor) {
        serviceProvider.getObject().completeApproval(request.getReferenceId(), false, actor, "Approval cancelled");
    }
}
