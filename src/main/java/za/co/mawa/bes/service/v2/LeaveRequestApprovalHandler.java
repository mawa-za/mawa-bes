package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;

@Component
@RequiredArgsConstructor
public class LeaveRequestApprovalHandler implements ApprovalSubmissionHandler, ApprovalCompletionHandler {
    private final LeaveRequestV2Service leaveRequestService;

    @Override
    public ApprovalType supports() {
        return ApprovalType.LEAVE;
    }

    @Override
    public void onSubmit(ApprovalRequestEntity approvalRequest, String actionBy) {
        leaveRequestService.submitFromApproval(approvalRequest.getReferenceId(), approvalRequest.getId(), actionBy);
    }

    @Override
    public void onApproved(ApprovalRequestEntity approvalRequest, String actionBy) {
        leaveRequestService.approveFromApproval(
                approvalRequest.getReferenceId(),
                "Approved through approval workflow",
                actionBy);
    }

    @Override
    public void onRejected(ApprovalRequestEntity approvalRequest, String actionBy) {
        leaveRequestService.rejectFromApproval(
                approvalRequest.getReferenceId(),
                "Rejected through approval workflow",
                actionBy);
    }

    @Override
    public void onCancelled(ApprovalRequestEntity approvalRequest, String actionBy) {
        leaveRequestService.cancelFromApproval(
                approvalRequest.getReferenceId(),
                "Approval workflow cancelled",
                actionBy);
    }
}
