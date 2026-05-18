package za.co.mawa.bes.service.v2;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.payment.request.PaymentRequestDto;
import za.co.mawa.bes.dto.v2.MessageQueueInboundDto;
import za.co.mawa.bes.dto.v2.payment.PaymentRequestResponse;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.entity.v2.PaymentRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.fnb.dto.BankPaymentRequest;
import za.co.mawa.bes.fnb.v2.BankPaymentService;
import za.co.mawa.bes.service.MessageProducerService;
import za.co.mawa.bes.service.SettingService;

@Component
@RequiredArgsConstructor
public class PaymentRequestApprovalHandler implements ApprovalCompletionHandler, ApprovalSubmissionHandler {

    private final PaymentRequestService paymentRequestService;
    private final MessageProducerService messageProducerService;
    private final SettingService settingService;
    private final Gson gson;
;
    @Autowired
    @Qualifier("bankPaymentServiceV2")
    BankPaymentService bankPaymentService;

    @Override
    public ApprovalType supports() {
        return ApprovalType.PAYMENT_REQUEST;
    }

    @Override
    public void onSubmit(ApprovalRequestEntity approvalRequest, String actionBy) {
        String paymentRequestId = approvalRequest.getReferenceId();
        PaymentRequestEntity paymentRequest =  paymentRequestService.findById(paymentRequestId);
        paymentRequest.setApprovalRequestId(approvalRequest.getId());
        paymentRequest.setUpdatedBy(actionBy);
        paymentRequestService.linkApproval(paymentRequest);
    }

    @Override
    public void onApproved(ApprovalRequestEntity approvalRequest, String actionBy) {
        String paymentRequestId = approvalRequest.getReferenceId();

        paymentRequestService.markApproved(paymentRequestId, actionBy);

        String fnbEnabled = settingService.getSetting("INTEGRATION", "FNB-API");

        if (!"1".equals(fnbEnabled)) {
            return;
        }
        PaymentRequestEntity paymentRequest =  paymentRequestService.findById(paymentRequestId);
        BankPaymentRequest bankPaymentRequest =  bankPaymentService.generateRequest(paymentRequest);
        MessageQueueInboundDto messageQueueInboundDto = new MessageQueueInboundDto();
        messageQueueInboundDto.setType("FNB-EFT-PAYMENT");
        messageQueueInboundDto.setReferenceId(approvalRequest.getReferenceId());
        messageQueueInboundDto.setReferenceNo(approvalRequest.getReferenceNo());
        messageQueueInboundDto.setPayload(gson.toJson(bankPaymentRequest));
        messageProducerService.sendMessageIfNotExists(messageQueueInboundDto);
        paymentRequestService.markQueuedForPayment(paymentRequestId, actionBy);
    }
}
