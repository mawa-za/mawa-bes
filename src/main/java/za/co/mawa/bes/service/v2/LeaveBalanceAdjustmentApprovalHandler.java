package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;

@Component
@RequiredArgsConstructor
public class LeaveBalanceAdjustmentApprovalHandler implements ApprovalCompletionHandler {
    private final LeaveBalanceAdjustmentService service;

    @Override
    public ApprovalType supports() { return ApprovalType.LEAVE_BALANCE_ADJUSTMENT; }

    @Override
    public void onApproved(ApprovalRequestEntity request, String actionBy) {
        service.approve(request.getReferenceId(), request.getId(), actionBy);
    }

    @Override
    public void onRejected(ApprovalRequestEntity request, String actionBy) {
        service.reject(request.getReferenceId(), request.getId(), actionBy, "Leave balance adjustment rejected");
    }

    @Override
    public void onCancelled(ApprovalRequestEntity request, String actionBy) {
        service.cancel(request.getReferenceId(), request.getId(), actionBy, "Leave balance adjustment approval cancelled");
    }
}
