package za.co.mawa.bes.service.v2;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.MessageQueueInboundDto;
import za.co.mawa.bes.entity.v2.PaymentRequestEntity;
import za.co.mawa.bes.entity.v2.PaymentRequestStatusHistoryEntity;
import za.co.mawa.bes.enums.PaymentMethod;
import za.co.mawa.bes.enums.PaymentRequestStatus;
import za.co.mawa.bes.fnb.dto.BankPaymentRequest;
import za.co.mawa.bes.fnb.v2.BankPaymentService;
import za.co.mawa.bes.repository.v2.PaymentRequestRepository;
import za.co.mawa.bes.repository.v2.PaymentRequestStatusHistoryRepository;
import za.co.mawa.bes.service.MessageProducerService;
import za.co.mawa.bes.service.SettingService;

@Service
@RequiredArgsConstructor
public class PaymentRequestFnbPaymentQueueService {

    private static final String FNB_MESSAGE_TYPE = "FNB-EFT-PAYMENT";
    private static final String LEGACY_INTEGRATION_GROUP = "INTEGRATION";
    private static final String FNB_API_GROUP = "FNB-API";
    private static final String FNB_API_ATTRIBUTE = "FNB-API";

    private final PaymentRequestRepository paymentRequestRepository;
    private final PaymentRequestStatusHistoryRepository statusHistoryRepository;
    private final MessageProducerService messageProducerService;
    private final SettingService settingService;
    private final Gson gson;
    private final PaymentDisbursementAttemptService attemptService;

    @Autowired
    @Qualifier("bankPaymentServiceV2")
    private BankPaymentService bankPaymentService;

    @Transactional
    public void queueAfterApproval(String paymentRequestId, String referenceNo, String actionBy) {
        PaymentRequestEntity paymentRequest = findById(paymentRequestId);

        if (paymentRequest.getStatus() == PaymentRequestStatus.QUEUED_FOR_PAYMENT
                || paymentRequest.getStatus() == PaymentRequestStatus.PROCESSED
                || paymentRequest.getStatus() == PaymentRequestStatus.PAID) {
            return;
        }

        if (paymentRequest.getStatus() != PaymentRequestStatus.APPROVED) {
            throw new RuntimeException("Payment request must be APPROVED before sending to FNB: " + paymentRequest.getRequestNo());
        }

        if (!isFnbEnabled()) {
            return;
        }

        if (paymentRequest.getPaymentMethod() != PaymentMethod.EFT) {
            return;
        }

        BankPaymentRequest bankPaymentRequest = bankPaymentService.generateRequest(paymentRequest);

        MessageQueueInboundDto message = new MessageQueueInboundDto();
        message.setType(FNB_MESSAGE_TYPE);
        message.setReferenceId(paymentRequest.getId());
        message.setReferenceNo(firstNonBlank(referenceNo, paymentRequest.getRequestNo()));
        message.setPayload(gson.toJson(bankPaymentRequest));

        messageProducerService.sendMessageIfNotExists(message);
        attemptService.ensureQueued(paymentRequest.getId());
        markQueuedForPayment(paymentRequest, actionBy);
    }

    @Transactional
    public void queuePaymentReport(String paymentRequestId, String instructionId) {
        MessageQueueInboundDto message = new MessageQueueInboundDto();
        message.setType("FNB-EFT-PAYMENT-REPORT");
        message.setReferenceId(paymentRequestId);
        message.setReferenceNo(instructionId);
        message.setPayload("{\"instructionId\":\"" + instructionId.replace("\"", "") + "\"}");
        messageProducerService.sendMessageIfNotExists(message);
    }

    private void markQueuedForPayment(PaymentRequestEntity paymentRequest, String updatedBy) {
        PaymentRequestStatus oldStatus = paymentRequest.getStatus();

        if (oldStatus == PaymentRequestStatus.QUEUED_FOR_PAYMENT) {
            return;
        }

        paymentRequest.setStatus(PaymentRequestStatus.QUEUED_FOR_PAYMENT);
        paymentRequest.setUpdatedBy(updatedBy);
        paymentRequestRepository.save(paymentRequest);

        saveHistory(
                paymentRequest.getId(),
                oldStatus,
                PaymentRequestStatus.QUEUED_FOR_PAYMENT,
                "Payment request queued for FNB EFT payment",
                updatedBy
        );
    }

    private PaymentRequestEntity findById(String paymentRequestId) {
        return paymentRequestRepository.findById(paymentRequestId)
                .orElseThrow(() -> new RuntimeException("Payment request not found: " + paymentRequestId));
    }

    private boolean isFnbEnabled() {
        return isTruthy(settingService.getSetting("ENABLED", FNB_API_GROUP))
                || isTruthy(settingService.getSetting("FNB-INTEGRATION-ENABLED", FNB_API_GROUP))
                || isTruthy(settingService.getSetting(FNB_API_ATTRIBUTE, LEGACY_INTEGRATION_GROUP));
    }

    private boolean isTruthy(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim();
        return "1".equals(normalized)
                || "true".equalsIgnoreCase(normalized)
                || "Y".equalsIgnoreCase(normalized)
                || "yes".equalsIgnoreCase(normalized);
    }

    private void saveHistory(
            String paymentRequestId,
            PaymentRequestStatus oldStatus,
            PaymentRequestStatus newStatus,
            String comment,
            String currentUser
    ) {
        PaymentRequestStatusHistoryEntity history = new PaymentRequestStatusHistoryEntity();
        history.setPaymentRequestId(paymentRequestId);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setComment(comment);
        history.setChangedBy(currentUser);
        statusHistoryRepository.save(history);
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }
}
