package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;

@RequiredArgsConstructor
public abstract class AbstractEmploymentActionApprovalHandler implements ApprovalCompletionHandler {
    protected final EmploymentLifecycleService lifecycleService;

    @Override
    public void onApproved(ApprovalRequestEntity request, String actionBy) {
        lifecycleService.approveAction(request.getReferenceId(), request.getId(), actionBy);
    }

    @Override
    public void onRejected(ApprovalRequestEntity request, String actionBy) {
        lifecycleService.rejectAction(request.getReferenceId(), request.getId(), actionBy, "Employment action rejected");
    }

    @Override
    public void onCancelled(ApprovalRequestEntity request, String actionBy) {
        lifecycleService.cancelAction(request.getReferenceId(), request.getId(), actionBy, "Employment action approval cancelled");
    }
}
