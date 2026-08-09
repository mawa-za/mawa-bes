package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;

@Component
@RequiredArgsConstructor
public class PayrollBatchApprovalHandler implements ApprovalSubmissionHandler, ApprovalCompletionHandler {
    private final PayrollPaymentBatchService payrollService;

    @Override
    public ApprovalType supports() {
        return ApprovalType.PAYROLL_BATCH;
    }

    @Override
    public void onSubmit(ApprovalRequestEntity approvalRequest, String actionBy) {
        payrollService.markPendingApproval(approvalRequest.getReferenceId(), approvalRequest.getId(), actionBy);
    }

    @Override
    public void onApproved(ApprovalRequestEntity approvalRequest, String actionBy) {
        payrollService.approveBatch(approvalRequest.getReferenceId(), actionBy);
    }

    @Override
    public void onRejected(ApprovalRequestEntity approvalRequest, String actionBy) {
        payrollService.rejectFromApproval(approvalRequest.getReferenceId(), actionBy);
    }

    @Override
    public void onCancelled(ApprovalRequestEntity approvalRequest, String actionBy) {
        payrollService.cancelBatch(approvalRequest.getReferenceId(), actionBy);
    }
}
