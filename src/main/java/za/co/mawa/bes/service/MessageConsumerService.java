package za.co.mawa.bes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.entity.MessageQueueEntity;
import za.co.mawa.bes.fnb.BankPaymentService;
import za.co.mawa.bes.fnb.FnbInitiationRecoveryService;
import za.co.mawa.bes.fnb.dto.BankPaymentRequest;
import za.co.mawa.bes.fnb.dto.PaymentInformation;
import za.co.mawa.bes.fnb.dto.BankPaymentResponse;
import za.co.mawa.bes.fnb.dto.OriginalPaymentInformation;
import za.co.mawa.bes.fnb.dto.StatusReasonInformation;
import za.co.mawa.bes.fnb.dto.TransactionInfoAndStatus;
import za.co.mawa.bes.service.v2.PaymentDisbursementAttemptService;
import za.co.mawa.bes.service.v2.PaymentRequestFnbPaymentQueueService;
import za.co.mawa.bes.repository.MessageQueueRepository;
import za.co.mawa.bes.xero.XeroInvoicePushService;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class MessageConsumerService {

    @Autowired
    UserService userService;
    @Autowired
    MessageQueueRepository messageQueueRepository;
    @Autowired
    TenantAdminService tenantAdminService;
    @Autowired
    BankPaymentService bankPaymentService;
    @Autowired
    FnbInitiationRecoveryService fnbInitiationRecoveryService;
    @Autowired
    @Qualifier("paymentRequestServiceV2")
    za.co.mawa.bes.service.v2.PaymentRequestService paymentRequestService;
    @Autowired
    XeroInvoicePushService xeroInvoicePushService;
    @Autowired
    PaymentDisbursementAttemptService paymentAttemptService;
    @Autowired
    PaymentRequestFnbPaymentQueueService paymentQueueService;
    @Autowired
    SettingService settingService;
    Gson gson = new Gson();

    private static final String QUEUE_GROUP = "MESSAGE-QUEUE";
    private static final String ENABLED = "ENABLED";
    private static final String INTERVAL_SECONDS = "INTERVAL-SECONDS";
    private static final String BATCH_SIZE = "BATCH-SIZE";
    private static final String RETRY_DELAY_SECONDS = "RETRY-DELAY-SECONDS";
    private final Map<String, LocalDateTime> lastRunByTenant = new ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "${mawa.scheduler.dispatcher-delay-ms:30000}")
    public void processAllTenants() {
        final List<TenantDto> tenants;
        try {
            tenants = tenantAdminService.getAll();
        } catch (RuntimeException ex) {
            // A temporary admin-service outage must not escape the scheduled
            // method and generate an unbounded TaskUtils stack trace every run.
            log.error("Message queue dispatch skipped because tenant discovery is unavailable: {}", ex.getMessage());
            return;
        }
        for (TenantDto tenant : tenants) {
            try {
                TenantContext.setCurrentTenant(tenant.getId());
                if (!isSchedulerEnabled() || !isDueToRun()) {
                    continue;
                }
                lastRunByTenant.put(TenantContext.getCurrentTenant(), LocalDateTime.now());
                processCurrentTenant();
            } catch (Exception e) {
                System.err.println("Error processing tenant " + tenant + ": " + e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    public boolean isSchedulerEnabled() {
        String enabled = settingService.getSetting(ENABLED, QUEUE_GROUP);
        return enabled == null || enabled.isBlank() || "true".equalsIgnoreCase(enabled) || "1".equals(enabled) || "Y".equalsIgnoreCase(enabled);
    }

    public int getSchedulerIntervalSeconds() {
        String value = settingService.getSetting(INTERVAL_SECONDS, QUEUE_GROUP);
        try {
            int parsed = Integer.parseInt(value);
            return Math.max(30, parsed);
        } catch (Exception ignored) {
            return 60;
        }
    }

    public int getBatchSize() {
        String value = settingService.getSetting(BATCH_SIZE, QUEUE_GROUP);
        try {
            int parsed = Integer.parseInt(value);
            return Math.max(1, Math.min(parsed, 100));
        } catch (Exception ignored) {
            return 10;
        }
    }

    public int getRetryDelaySeconds() {
        String value = settingService.getSetting(RETRY_DELAY_SECONDS, QUEUE_GROUP);
        try {
            int parsed = Integer.parseInt(value);
            return Math.max(5, Math.min(parsed, 3600));
        } catch (Exception ignored) {
            return 10;
        }
    }

    public LocalDateTime getLastRunAt() { return lastRunByTenant.get(TenantContext.getCurrentTenant()); }

    public LocalDateTime getNextRunAt() {
        if (!isSchedulerEnabled()) return null;
        LocalDateTime lastRunAt = getLastRunAt();
        if (lastRunAt == null) return LocalDateTime.now();
        return lastRunAt.plusSeconds(getSchedulerIntervalSeconds());
    }

    private boolean isDueToRun() {
        LocalDateTime lastRunAt = getLastRunAt();
        return lastRunAt == null || !lastRunAt.plusSeconds(getSchedulerIntervalSeconds()).isAfter(LocalDateTime.now());
    }

    public int processCurrentTenant() {
        ObjectMapper mapper = new ObjectMapper();
        int processedCount = 0;
        List<MessageQueueEntity> messageQueueEntities = messageQueueRepository
                .findTop10ByProcessedFalseAndNextAttemptAtBeforeOrderByNextAttemptAtAsc(LocalDateTime.now());
        int batchSize = getBatchSize();
        if (messageQueueEntities.size() > batchSize) {
            messageQueueEntities = messageQueueEntities.subList(0, batchSize);
        }

        for (MessageQueueEntity msg : messageQueueEntities) {
            try {
                System.out.println("Tenant: " + TenantContext.getCurrentTenant() + " Payload: " + msg.getPayload());
                switch (msg.getType()) {
                    case "FNB-EFT-PAYMENT":
                        BankPaymentRequest bankPaymentRequest = mapper.readValue(msg.getPayload(), BankPaymentRequest.class);
                        List<String> paymentRequestReferences = resolvePaymentRequestReferences(msg, bankPaymentRequest);
                        List<String> fnbLogReferences = resolveFnbLogReferences(msg, bankPaymentRequest);
                        String instructionId = resolveStoredInstructionId(paymentRequestReferences);
                        String systemUserId = resolveSystemUserId();

                        if (instructionId == null) {
                            instructionId = fnbInitiationRecoveryService
                                    .recoverInstructionId(fnbLogReferences);
                            if (instructionId != null) {
                                log.info(
                                        "Recovered FNB instruction ID {} from API activity logs for queue message {}",
                                        instructionId,
                                        msg.getId()
                                );
                            }
                        }

                        if (instructionId == null) {
                            instructionId = bankPaymentService.sendPaymentRequest(msg.getPayload());
                            if (instructionId == null || instructionId.isBlank()) {
                                throw new IllegalStateException("FNB initiate response did not contain an instructionId");
                            }
                        } else {
                            log.info(
                                    "Skipping duplicate FNB initiate call for queue message {}. Reusing instruction ID {}",
                                    msg.getId(),
                                    instructionId
                            );
                        }

                        // Persist first, in an independent transaction, before any other
                        // local processing that could fail and cause a queue retry.
                        for (String paymentRequestReference : paymentRequestReferences) {
                            paymentRequestService.recordFnbInstruction(
                                    paymentRequestReference,
                                    instructionId,
                                    systemUserId
                            );
                        }

                        if (resolveStoredInstructionId(paymentRequestReferences) == null) {
                            throw new IllegalStateException("FNB instruction ID could not be persisted against the payment request");
                        }

                        for (String paymentRequestReference : paymentRequestReferences) {
                            paymentRequestService.markSentToBank(
                                    paymentRequestReference,
                                    instructionId,
                                    systemUserId
                            );
                            paymentAttemptService.markSubmitted(paymentRequestReference, instructionId);
                            paymentQueueService.queuePaymentReport(paymentRequestReference, instructionId);
                        }

                        msg.setProcessed(true);
                        break;
                    case "FNB-EFT-PAYMENT-REPORT":
                        processFnbPaymentReport(msg, systemUserIdForReport());
                        break;
                    case "INVOICE-EMAIL":
                        msg.setProcessed(true);
                        break;
                    case "XERO-INVOICE":
                        xeroInvoicePushService.pushInvoice(resolveInvoiceId(msg));
                        msg.setProcessed(true);
                        break;
                    default:
                        System.out.println("No processor registered for message type: " + msg.getType());
                        break;
                }

            } catch (Exception e) {
                log.error(
                        "Message queue processing failed for tenant {}, message {}, type {}",
                        TenantContext.getCurrentTenant(),
                        msg.getId(),
                        msg.getType(),
                        e
                );
                if ("XERO-INVOICE".equals(msg.getType())) {
                    xeroInvoicePushService.markFailed(resolveInvoiceId(msg), e.getMessage());
                }
                msg.setRetryCount(msg.getRetryCount() + 1);
                if ("FNB-EFT-PAYMENT-REPORT".equals(msg.getType())) {
                    // Bank report availability is eventually consistent. Keep polling instead
                    // of abandoning a valid disbursement after three temporary failures.
                    msg.setProcessed(false);
                    msg.setNextAttemptAt(LocalDateTime.now().plusSeconds(getPaymentReportIntervalSeconds()));
                } else if (msg.getRetryCount() > 3) {
                    msg.setProcessed(true);
                } else {
                    msg.setNextAttemptAt(LocalDateTime.now().plusSeconds(getRetryDelaySeconds()));
                }
            }
            messageQueueRepository.save(msg);
            processedCount++;
        }
        return processedCount;
    }

    private void processFnbPaymentReport(MessageQueueEntity msg, String systemUserId) throws Exception {
        String paymentRequestId = msg.getReferenceId();
        String instructionId = paymentRequestService.getFnbInstructionId(paymentRequestId);
        if (instructionId == null || instructionId.isBlank()) {
            instructionId = msg.getReferenceNo();
        }
        if (instructionId == null || instructionId.isBlank()) {
            throw new IllegalStateException("FNB payment report message has no instruction ID");
        }

        BankPaymentResponse report = bankPaymentService.getPaymentReport(instructionId);
        String providerStatus = resolveProviderStatus(report);
        String reason = resolveProviderReason(report);

        if (isSuccessfulBankStatus(providerStatus)) {
            paymentRequestService.markBankPaymentPaid(paymentRequestId, providerStatus, systemUserId);
            paymentAttemptService.markSucceeded(paymentRequestId, providerStatus);
            msg.setProcessed(true);
            return;
        }

        if (isFailedBankStatus(providerStatus)) {
            paymentRequestService.markBankPaymentFailed(paymentRequestId, providerStatus, reason, systemUserId);
            paymentAttemptService.markFailed(paymentRequestId, providerStatus, providerStatus, reason);
            msg.setProcessed(true);
            return;
        }

        paymentRequestService.markBankPaymentPending(paymentRequestId, providerStatus, systemUserId);
        paymentAttemptService.markPending(paymentRequestId, providerStatus);
        msg.setRetryCount(msg.getRetryCount() + 1);
        msg.setNextAttemptAt(LocalDateTime.now().plusSeconds(getPaymentReportIntervalSeconds()));
        msg.setProcessed(false);
    }

    private int getPaymentReportIntervalSeconds() {
        String value = settingService.getSetting("PAYMENT-REPORT-INTERVAL-SECONDS", "FNB-API");
        try {
            return Math.max(30, Math.min(Integer.parseInt(value), 3600));
        } catch (Exception ignored) {
            return 60;
        }
    }

    private String systemUserIdForReport() {
        return resolveSystemUserId();
    }

    private String resolveProviderStatus(BankPaymentResponse report) {
        if (report == null) return "PENDING";
        if (report.getOriginalPaymentInformation() != null) {
            for (OriginalPaymentInformation payment : report.getOriginalPaymentInformation()) {
                if (payment.getTransactionInfoAndStatus() != null) {
                    for (TransactionInfoAndStatus transaction : payment.getTransactionInfoAndStatus()) {
                        if (transaction.getTransactionStatus() != null && !transaction.getTransactionStatus().isBlank()) {
                            return transaction.getTransactionStatus();
                        }
                    }
                }
                if (payment.getPaymentInformationStatus() != null && !payment.getPaymentInformationStatus().isBlank()) {
                    return payment.getPaymentInformationStatus();
                }
            }
        }
        return report.getGroupStatus() == null || report.getGroupStatus().isBlank() ? "PENDING" : report.getGroupStatus();
    }

    private String resolveProviderReason(BankPaymentResponse report) {
        if (report == null) return "No payment report returned by FNB";
        String reason = reasonFrom(report.getStatusReasonInformation());
        if (reason != null) return reason;
        if (report.getOriginalPaymentInformation() != null) {
            for (OriginalPaymentInformation payment : report.getOriginalPaymentInformation()) {
                reason = reasonFrom(payment.getStatusReasonInformation());
                if (reason != null) return reason;
                if (payment.getTransactionInfoAndStatus() != null) {
                    for (TransactionInfoAndStatus transaction : payment.getTransactionInfoAndStatus()) {
                        reason = reasonFrom(transaction.getStatusReasonInformation());
                        if (reason != null) return reason;
                    }
                }
            }
        }
        return "FNB returned status " + resolveProviderStatus(report);
    }

    private String reasonFrom(List<StatusReasonInformation> reasons) {
        if (reasons == null) return null;
        for (StatusReasonInformation reason : reasons) {
            if (reason == null) continue;
            String value = firstNonBlank(reason.getAdditionalInformation(), reason.getReason());
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) return primary;
        return fallback;
    }

    private boolean isSuccessfulBankStatus(String status) {
        if (status == null) return false;
        return Set.of("ACSC", "ACCC", "COMPLETED", "COMPLETE", "SUCCESS", "SUCCEEDED", "PAID")
                .contains(status.trim().toUpperCase());
    }

    private boolean isFailedBankStatus(String status) {
        if (status == null) return false;
        return Set.of("RJCT", "REJECTED", "FAILED", "FAILURE", "CANCELLED", "CANCELED", "CANC")
                .contains(status.trim().toUpperCase());
    }

    private List<String> resolvePaymentRequestReferences(
            MessageQueueEntity message,
            BankPaymentRequest bankPaymentRequest
    ) {
        if (message.getReferenceId() != null && !message.getReferenceId().isBlank()) {
            // Resolve now, before calling FNB, so an invalid queue reference cannot
            // result in a successful external payment with no local payment request.
            paymentRequestService.getFnbInstructionId(message.getReferenceId());
            return List.of(message.getReferenceId());
        }

        if (bankPaymentRequest.getPaymentInformation() == null || bankPaymentRequest.getPaymentInformation().isEmpty()) {
            throw new IllegalStateException("FNB queue message contains no payment request reference");
        }

        List<String> references = bankPaymentRequest.getPaymentInformation().stream()
                .map(PaymentInformation::getPaymentInformationId)
                .filter(reference -> reference != null && !reference.isBlank())
                .distinct()
                .toList();

        if (references.isEmpty()) {
            throw new IllegalStateException("FNB queue message contains no payment request reference");
        }

        for (String reference : references) {
            paymentRequestService.getFnbInstructionId(reference);
        }
        return references;
    }

    private List<String> resolveFnbLogReferences(
            MessageQueueEntity message,
            BankPaymentRequest bankPaymentRequest
    ) {
        Set<String> references = new LinkedHashSet<>();
        if (message.getReferenceNo() != null && !message.getReferenceNo().isBlank()) {
            references.add(message.getReferenceNo());
        }
        if (bankPaymentRequest.getPaymentInformation() != null) {
            bankPaymentRequest.getPaymentInformation().stream()
                    .map(PaymentInformation::getPaymentInformationId)
                    .filter(reference -> reference != null && !reference.isBlank())
                    .forEach(references::add);
        }
        return List.copyOf(references);
    }

    private String resolveStoredInstructionId(List<String> paymentRequestReferences) {
        String resolvedInstructionId = null;
        for (String reference : paymentRequestReferences) {
            String storedInstructionId = paymentRequestService.getFnbInstructionId(reference);
            if (storedInstructionId == null || storedInstructionId.isBlank()) {
                continue;
            }
            if (resolvedInstructionId != null && !resolvedInstructionId.equals(storedInstructionId)) {
                throw new IllegalStateException("Payment requests in the same FNB message have different instruction IDs");
            }
            resolvedInstructionId = storedInstructionId;
        }
        return resolvedInstructionId;
    }

    private String resolveSystemUserId() {
        try {
            String userId = userService.getUserByName("BGUSER").getId();
            if (userId != null && !userId.isBlank()) {
                return userId;
            }
        } catch (Exception exception) {
            log.warn("BGUSER is unavailable; using SYSTEM for FNB queue audit updates", exception);
        }
        return "SYSTEM";
    }

    private String resolveInvoiceId(MessageQueueEntity msg) {
        if (msg.getReferenceId() != null && !msg.getReferenceId().isBlank()) {
            return msg.getReferenceId();
        }
        return msg.getPayload();
    }

    private void sendInvoice(MessageQueueEntity msg) {
        try {
//            paymentRequestService.sendInvoiceFile(msg.getPayload());
            msg.setProcessed(true);
        } catch (Exception e) {
            msg.setRetryCount(msg.getRetryCount() + 1);
            if (msg.getRetryCount() > 3) {
                msg.setProcessed(true); // Optionally move to DeadLetterQueue
            } else {
                msg.setNextAttemptAt(LocalDateTime.now().plusSeconds(getRetryDelaySeconds()));
            }
        }
        messageQueueRepository.save(msg);
    }
}


