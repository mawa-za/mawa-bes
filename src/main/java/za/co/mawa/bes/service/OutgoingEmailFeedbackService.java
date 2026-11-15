package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Locale;

/**
 * Provider-neutral delivery feedback ingestion seam. This has NO public HTTP endpoint.
 * The future provider adapter MUST authenticate/verify provider signatures, bind the
 * provider message ID to an existing request, enforce replay protections, and call
 * recordVerifiedEvent only after that verification. Do not send arbitrary browser events here.
 */
@Service
public class OutgoingEmailFeedbackService {
    private final TenantOutgoingEmailOutboxStore store;
    private final EmailRecipientIdentity identity;
    private final boolean enabled;

    public OutgoingEmailFeedbackService(TenantOutgoingEmailOutboxStore store,
                                        EmailRecipientIdentity identity,
                                        @Value("${mawa.outgoing.feedback.enabled:false}") boolean enabled) {
        this.store = store;
        this.identity = identity;
        this.enabled = enabled;
        if (enabled && !identity.configured()) {
            throw new IllegalStateException("Outgoing delivery feedback requires a configured recipient HMAC key");
        }
    }

    public boolean recordVerifiedEvent(String requestId, String providerEventId, String rawType) {
        if (!enabled) throw new IllegalStateException("Outgoing delivery feedback is disabled");
        if (!identity.configured()) throw new IllegalStateException("Outgoing delivery feedback HMAC key is missing");
        if (rawType == null) throw new IllegalArgumentException("Delivery event type is required");
        String type = rawType.trim().toUpperCase(Locale.ROOT);
        if (!type.equals("DELIVERED") && !type.equals("BOUNCED") && !type.equals("COMPLAINT")) {
            throw new IllegalArgumentException("Unsupported delivery event type");
        }
        if (requestId == null || !requestId.matches("[a-fA-F0-9-]{36}")
                || providerEventId == null || !providerEventId.matches("[A-Za-z0-9._:-]{1,128}")) {
            throw new IllegalArgumentException("Invalid delivery event identity");
        }
        return store.applyVerifiedFeedback(requestId, providerEventId, type);
    }
}
