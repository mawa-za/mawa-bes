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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
    private final PaymentAccountConfigurationService paymentAccountConfigurationService;

    @Autowired
    @Qualifier("bankPaymentServiceV2")
    private BankPaymentService bankPaymentService;

    /**
     * Best-effort queueing used by automatic approval completion handlers. A missing
     * or disabled payment configuration must not reverse an otherwise valid claim
     * approval; the reason is written to payment-request history for diagnosis.
     */
    @Transactional
    public void queueAfterApproval(String paymentRequestId, String referenceNo, String actionBy) {
        queue(paymentRequestId, referenceNo, actionBy, false);
    }

    /**
     * Explicit user-triggered queueing. Configuration problems are returned to the
     * caller so an approved request can be corrected and reprocessed safely.
     */
    @Transactional
    public void queueForBank(String paymentRequestId, String referenceNo, String actionBy) {
        queue(paymentRequestId, referenceNo, actionBy, true);
    }

    private void queue(
            String paymentRequestId,
            String referenceNo,
            String actionBy,
            boolean failWhenNotQueueable
    ) {
        PaymentRequestEntity paymentRequest = findById(paymentRequestId);

        if (paymentRequest.getStatus() == PaymentRequestStatus.QUEUED_FOR_PAYMENT
                || paymentRequest.getStatus() == PaymentRequestStatus.PROCESSED
                || paymentRequest.getStatus() == PaymentRequestStatus.PAID) {
            return;
        }

        if (paymentRequest.getStatus() != PaymentRequestStatus.APPROVED) {
            throw new IllegalStateException(
                    "Payment request must be APPROVED before sending to FNB: " + paymentRequest.getRequestNo()
            );
        }

        String actor = defaultActor(actionBy);
        if (!prepareFnbRouting(paymentRequest, actor, failWhenNotQueueable)) {
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
        markQueuedForPayment(paymentRequest, actor);
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

    private boolean prepareFnbRouting(
            PaymentRequestEntity paymentRequest,
            String actionBy,
            boolean failWhenNotQueueable
    ) {
        if (!isFnbEnabled()) {
            return notQueueable(
                    paymentRequest,
                    "Bank message not queued because FNB integration is disabled.",
                    failWhenNotQueueable,
                    actionBy
            );
        }

        if (paymentRequest.getRequestType() == null) {
            return notQueueable(
                    paymentRequest,
                    "Bank message not queued because the payment request type is missing.",
                    failWhenNotQueueable,
                    actionBy
            );
        }

        Optional<Map<String, Object>> configuredDebtor =
                paymentAccountConfigurationService.activeDebtor(paymentRequest.getRequestType().name());
        if (configuredDebtor.isEmpty()) {
            return notQueueable(
                    paymentRequest,
                    "Bank message not queued because no active debtor account is configured for payment request type "
                            + paymentRequest.getRequestType().name() + ".",
                    failWhenNotQueueable,
                    actionBy
            );
        }

        Map<String, Object> debtor = configuredDebtor.get();
        String integration = Objects.toString(debtor.get("bank_integration"), "").trim();
        if (!"FNB".equalsIgnoreCase(integration)) {
            return notQueueable(
                    paymentRequest,
                    "Bank message not queued because the active debtor account for payment request type "
                            + paymentRequest.getRequestType().name() + " is not configured for FNB.",
                    failWhenNotQueueable,
                    actionBy
            );
        }

        String debtorAccountId = Objects.toString(debtor.get("id"), null);
        boolean routingChanged = !Objects.equals(paymentRequest.getDebtorAccountId(), debtorAccountId)
                || !"FNB".equalsIgnoreCase(paymentRequest.getBankIntegration())
                || paymentRequest.getPaymentMethod() != PaymentMethod.EFT;

        paymentRequest.setDebtorAccountId(debtorAccountId);
        paymentRequest.setBankIntegration("FNB");
        paymentRequest.setPaymentMethod(PaymentMethod.EFT);
        paymentRequest.setUpdatedBy(actionBy);

        if (routingChanged) {
            paymentRequestRepository.saveAndFlush(paymentRequest);
            saveHistory(
                    paymentRequest.getId(),
                    PaymentRequestStatus.APPROVED,
                    PaymentRequestStatus.APPROVED,
                    "Payment routing refreshed from active FNB debtor account before queueing",
                    actionBy
            );
        }
        return true;
    }

    private boolean notQueueable(
            PaymentRequestEntity paymentRequest,
            String reason,
            boolean failWhenNotQueueable,
            String actionBy
    ) {
        saveHistory(
                paymentRequest.getId(),
                PaymentRequestStatus.APPROVED,
                PaymentRequestStatus.APPROVED,
                reason,
                actionBy
        );
        if (failWhenNotQueueable) {
            throw new IllegalStateException(reason);
        }
        return false;
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

    private String defaultActor(String actionBy) {
        return actionBy == null || actionBy.isBlank() ? "SYSTEM" : actionBy;
    }
}
