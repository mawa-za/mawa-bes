package za.co.mawa.bes.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import za.co.mawa.bes.dto.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/** Common in-process outbox API, reusable by future billing, reporting and admin integrations. */
@Service
public class QueuedOutgoingEmailService {
    private static final int MAX_MIME_BYTES = 10 * 1024 * 1024;
    private final SharedOutgoingEmailService transport;
    private final TenantOutgoingEmailOutboxStore store;
    private final EmailRecipientIdentity recipientIdentity;

    @Autowired
    public QueuedOutgoingEmailService(SharedOutgoingEmailService transport, TenantOutgoingEmailOutboxStore store,
                                      EmailRecipientIdentity recipientIdentity) {
        this.transport = transport;
        this.store = store;
        this.recipientIdentity = recipientIdentity;
    }

    /** Test/backwards-compatible constructor when recipient fingerprinting is not configured. */
    QueuedOutgoingEmailService(SharedOutgoingEmailService transport, TenantOutgoingEmailOutboxStore store) {
        this(transport, store, null);
    }

    /**
     * Calls with the same source/resource/recipient/content in a five-minute slot reuse one logical request.
     * This avoids quick double-clicks but deliberately allows a later manual resend.
     */
    public String queue(String to, String subject, String html, List<File> attachments,
                        String source, String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            throw new IllegalArgumentException("Email resource ID is required");
        }
        byte[] mime = transport.compose(to, subject, html, attachments);
        if (mime.length > MAX_MIME_BYTES) {
            throw new IllegalArgumentException("Email exceeds the configured attachment size limit");
        }
        // MIME Message-ID changes for every compose; hash stable request metadata instead.
        StringBuilder stable = new StringBuilder(source).append('\n').append(resourceId).append('\n')
                .append(to.toLowerCase(java.util.Locale.ROOT)).append('\n').append(subject)
                .append('\n').append(html);
        if (attachments != null) {
            for (File file : attachments) {
                if (file != null) stable.append('\n').append(file.getName()).append(':')
                        .append(file.getType()).append(':').append(file.getContent());
            }
        }
        String fingerprint = hash(stable.append('\n').append(Instant.now().getEpochSecond() / 300).toString());
        String recipientHash = recipientIdentity == null ? null : recipientIdentity.fingerprint(to);
        return recipientHash == null
                ? store.enqueue(source, resourceId, fingerprint, mime)
                : store.enqueue(source, resourceId, fingerprint, mime, recipientHash);
    }

    public Optional<TenantOutgoingEmailOutboxStore.EmailStatus> invoiceStatus(String invoiceId, String requestId) {
        return store.findInvoiceStatus(invoiceId, requestId);
    }

    static String hash(String value) {
        return HexFormat.of().formatHex(hashBytes(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] hashBytes(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
