package za.co.mawa.bes.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import za.co.mawa.bes.configuration.context.TenantContext;

import java.sql.PreparedStatement;
import java.util.List;

/** Durable, tenant-bound, one-time submission of fixed templates (not arbitrary user HTML). */
@Service
public class OutgoingGatewaySubmissionService {
    private final EntityManager entityManager;
    private final QueuedOutgoingEmailService queue;
    private final OutgoingGatewayQuotaService quota;

    public OutgoingGatewaySubmissionService(EntityManager entityManager, QueuedOutgoingEmailService queue,
                                             OutgoingGatewayQuotaService quota) {
        this.entityManager = entityManager;
        this.queue = queue;
        this.quota = quota;
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record Request(String recipient, String reference, String eventId) {}
    @JsonIgnoreProperties(ignoreUnknown = false)
    public record StatusRequest(String reference, String requestId) {}
    public record Accepted(String status, String requestId) {}

    /** Read-only lookup bound to the authenticated caller's tenant and source. */
    @Transactional(readOnly = true)
    public java.util.Optional<TenantOutgoingEmailOutboxStore.EmailStatus> status(
            String client, StatusRequest request) {
        if (!StringUtils.hasText(TenantContext.getCurrentTenant())) {
            throw new SecurityException("Tenant context is required");
        }
        if (!java.util.Set.of("admin", "billing", "reporting").contains(client)) {
            throw new SecurityException("Unknown mail client");
        }
        if (request == null || request.reference() == null
                || !request.reference().matches("[A-Za-z0-9._/-]{1,100}")
                || request.requestId() == null
                || !request.requestId().matches("[0-9a-fA-F-]{36}")) {
            throw new IllegalArgumentException("Invalid outgoing mail status reference");
        }
        return queue.sourceStatus(client, request.reference(), request.requestId());
    }

    @Transactional
    public Accepted submit(String client, String nonce, Request request) {
        if (!StringUtils.hasText(TenantContext.getCurrentTenant())) {
            throw new SecurityException("Tenant context is required");
        }
        if (request == null || request.reference() == null
                || !request.reference().matches("[A-Za-z0-9._/-]{1,100}")
                || request.eventId() == null
                || !request.eventId().matches("[A-Za-z0-9_-]{8,100}")) {
            throw new IllegalArgumentException("Invalid mail reference or event ID");
        }
        String recipient = SharedOutgoingEmailService.validatedSingleAddress(request.recipient());
        // Only approved events from their named internal service. No client-controlled
        // From, Reply-To, subject, content, attachments or template identifiers.
        String subject = switch (client) {
            case "admin" -> "MAWA tenant notice";
            case "billing" -> "MAWA billing document available";
            case "reporting" -> "MAWA report available";
            default -> throw new SecurityException("Unknown mail client");
        };
        String description = switch (client) {
            case "admin" -> "A tenant administration update";
            case "billing" -> "A billing document";
            case "reporting" -> "A report";
            default -> throw new SecurityException("Unknown mail client");
        };
        String html = "<html><body><p>" + description
                + " is available in MAWA.</p><p>Reference: "
                + HtmlUtils.htmlEscape(request.reference()) + "</p></body></html>";
        // Anti-replay storage and tenant outbox INSERT run in the SAME transaction.
        entityManager.unwrap(Session.class).doWork(connection -> {
            try (PreparedStatement cleanup = connection.prepareStatement("""
                    DELETE FROM outbound_email_gateway_nonce
                     WHERE created_at < DATE_SUB(UTC_TIMESTAMP(6), INTERVAL 15 MINUTE)
                    """)) {
                cleanup.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO outbound_email_gateway_nonce(client_id, nonce, created_at)
                    VALUES (?, ?, UTC_TIMESTAMP(6))
                    """)) {
                insert.setString(1, client);
                insert.setString(2, nonce);
                try {
                    insert.executeUpdate(); // transaction rollbacks both nonce and enqueue on failure
                } catch (java.sql.SQLIntegrityConstraintViolationException replay) {
                    throw new SecurityException("Outgoing email request nonce already used");
                }
            }
        });
        // A charge and the outbox insert commit or roll back together; duplicate eventIds are free.
        quota.reserve(client, request.reference(), request.eventId());
        String id = queue.queueWithStableKey(recipient, subject, html, List.of(), client,
                request.reference(), request.eventId());
        return new Accepted("QUEUED", id);
    }
}
