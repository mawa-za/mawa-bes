package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.dto.v2.payment.PaymentRequestStatusUpdateRequest;
import za.co.mawa.bes.entity.v2.PaymentRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.enums.PaymentRequestStatus;

@Component
@RequiredArgsConstructor
public class PaymentRequestApprovalHandler implements ApprovalCompletionHandler, ApprovalSubmissionHandler {

    private final PaymentRequestService paymentRequestService;
    private final PaymentRequestFnbPaymentQueueService fnbPaymentQueueService;

    @Override
    public ApprovalType supports() {
        return ApprovalType.PAYMENT_REQUEST;
    }

    @Override
    public void onSubmit(ApprovalRequestEntity approvalRequest, String actionBy) {
        paymentRequestService.submit(approvalRequest.getReferenceId(), actionBy);

        PaymentRequestEntity paymentRequest = paymentRequestService.findById(approvalRequest.getReferenceId());
        paymentRequest.setApprovalRequestId(approvalRequest.getId());
        paymentRequest.setUpdatedBy(actionBy);
        paymentRequestService.linkApproval(paymentRequest);
    }

    @Override
    public void onApproved(ApprovalRequestEntity approvalRequest, String actionBy) {
        String paymentRequestId = approvalRequest.getReferenceId();

        paymentRequestService.markApproved(paymentRequestId, actionBy);
        fnbPaymentQueueService.queueAfterApproval(paymentRequestId, approvalRequest.getReferenceNo(), actionBy);
    }

    @Override
    public void onRejected(ApprovalRequestEntity approvalRequest, String actionBy) {
        PaymentRequestStatusUpdateRequest request = new PaymentRequestStatusUpdateRequest();
        request.setStatus(PaymentRequestStatus.REJECTED);
        request.setApprovalRequestId(approvalRequest.getId());
        request.setComment("Payment request rejected through approval workflow");
        paymentRequestService.updateStatus(approvalRequest.getReferenceId(), request, actionBy);
    }

    @Override
    public void onCancelled(ApprovalRequestEntity approvalRequest, String actionBy) {
        paymentRequestService.cancel(approvalRequest.getReferenceId(),
                "Payment request approval cancelled", actionBy);
    }
}
