package za.co.mawa.bes.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.service.v2.BackgroundExecutionContextService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * A bounded, opt-in Microsoft 365-only diagnostic poller. Exchange trace Read.All
 * must be granted to a SEPARATE app from the mailbox-scoped Mail.Send app.
 * Never exposes global trace results to tenants. No changes to non-matching mail.
 */
@Service
public class Microsoft365MessageTraceReconciler {
    private static final Logger log = LoggerFactory.getLogger(Microsoft365MessageTraceReconciler.class);
    private final TenantAdminService tenantAdmin;
    private final BackgroundExecutionContextService background;
    private final TenantOutgoingEmailOutboxStore store;
    private final Microsoft365MessageTraceClient client;
    private final EmailRecipientIdentity recipient;
    private final OutgoingEmailFeedbackService feedback;
    private final boolean enabled;
    private final boolean feedbackEnabled;
    private final int maxPerRun;

    public Microsoft365MessageTraceReconciler(TenantAdminService tenantAdmin,
            BackgroundExecutionContextService background,
            TenantOutgoingEmailOutboxStore store,
            Microsoft365MessageTraceClient client,
            EmailRecipientIdentity recipient,
            OutgoingEmailFeedbackService feedback,
            @Value("${mawa.outgoing.m365.trace.enabled:false}") boolean enabled,
            @Value("${mawa.outgoing.feedback.enabled:false}") boolean feedbackEnabled,
            @Value("${mawa.outgoing.m365.trace.max-per-run:5}") int maxPerRun) {
        this.tenantAdmin = tenantAdmin;
        this.background = background;
        this.store = store;
        this.client = client;
        this.recipient = recipient;
        this.feedback = feedback;
        this.enabled = enabled;
        this.feedbackEnabled = feedbackEnabled;
        this.maxPerRun = Math.max(1, Math.min(maxPerRun, 10));
    }

    @Scheduled(fixedDelayString = "${mawa.outgoing.m365.trace.poll-ms:300000}", initialDelay = 60000)
    public void reconcile() {
        if (!enabled) return;
        if (!feedbackEnabled || !recipient.configured()) {
            throw new IllegalStateException("Exchange trace requires enabled feedback and recipient HMAC");
        }
        // Local budget, in addition to tenant-scoped DB claiming. If multiple Cloud Run
        // instances run the poller, configure total instance budget below M365 throttling.
        int remaining = maxPerRun;
        for (TenantDto tenant : tenantAdmin.getAll()) {
            if (remaining <= 0) break;
            if (tenant == null || tenant.getId() == null
                    || !tenant.getId().matches("[A-Za-z0-9_-]{1,128}")) continue;
            try {
                TenantContext.setCurrentTenant(tenant.getId());
                background.establish();
                List<TenantOutgoingEmailOutboxStore.TraceCandidate> pending =
                        store.findTraceCandidates(Math.min(2, remaining));
                for (TenantOutgoingEmailOutboxStore.TraceCandidate candidate : pending) {
                    remaining--;
                    try {
                        reconcileOne(candidate);
                    } catch (Exception ex) {
                        // Do not log addresses, tokens, response bodies, subject or MIME.
                        log.warn("Microsoft 365 trace check failed tenant={} message={} error={}",
                                tenant.getId(), candidate.requestId(), ex.getClass().getSimpleName());
                    }
                }
            } catch (Exception ex) {
                log.warn("Microsoft 365 tenant trace scan failed tenant={} error={}",
                        tenant.getId(), ex.getClass().getSimpleName());
            } finally {
                background.clear();
                TenantContext.clear();
            }
        }
    }

    void reconcileOne(TenantOutgoingEmailOutboxStore.TraceCandidate candidate) {
        if (candidate == null || !Microsoft365MessageTraceClient.validMessageId(candidate.messageId())) {
            return;
        }
        List<Microsoft365MessageTraceClient.Trace> traces = client.findByMessageId(
                candidate.messageId(), candidate.acceptedAt());
        List<Microsoft365MessageTraceClient.Trace> matching = traces.stream()
                .filter(t -> candidate.messageId().equals(t.messageId()))
                .filter(t -> t.sender().equalsIgnoreCase(client.approvedSender()))
                .filter(t -> matchesRecipient(t.recipient(), candidate.recipientHash()))
                .filter(t -> !t.receivedAt().isBefore(candidate.acceptedAt().minus(Duration.ofHours(1))))
                .filter(t -> !t.receivedAt().isAfter(candidate.acceptedAt().plus(Duration.ofDays(4))))
                .toList();
        if (matching.size() != 1) {
            // No match or ambiguous match must never confirm delivery.
            store.recordTraceObservation(candidate.requestId(),
                    matching.isEmpty() ? "NOT_FOUND" : "AMBIGUOUS", null);
            return;
        }
        Microsoft365MessageTraceClient.Trace match = matching.get(0);
        String status = match.status().trim().toLowerCase(Locale.ROOT);
        String traceStatus = switch (status) {
            case "delivered" -> "DELIVERED";
            case "failed" -> "FAILED";
            case "pending", "gettingstatus" -> "PENDING";
            case "quarantined" -> "QUARANTINED";
            case "filteredasspam" -> "FILTERED_AS_SPAM";
            case "expanded" -> "EXPANDED";
            default -> "UNKNOWN";
        };
        store.recordTraceObservation(candidate.requestId(), traceStatus, match.traceId());
        if ("DELIVERED".equals(traceStatus)) {
            // Stable, tenant-local replay ID. No raw recipient data or trace result retained.
            String eventId = eventId(candidate.requestId(), match.traceId());
            feedback.recordVerifiedEvent(candidate.requestId(), eventId, "DELIVERED");
        }
        // Failed/quarantined trace is diagnostic, not proof of a permanent hard bounce.
        // Do NOT suppress the recipient unless separately verified by Microsoft NDR evidence.
    }

    private boolean matchesRecipient(String email, String expectedHash) {
        try { return recipient.fingerprint(email).equals(expectedHash); }
        catch (IllegalArgumentException ignored) { return false; }
    }

    static String eventId(String requestId, String traceId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((requestId + ":" + traceId + ":DELIVERED")
                    .getBytes(StandardCharsets.UTF_8));
            return "m365-trace-" + HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to identify Exchange trace", ex);
        }
    }
}
