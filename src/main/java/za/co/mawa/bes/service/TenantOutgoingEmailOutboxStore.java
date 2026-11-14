package za.co.mawa.bes.service;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.TenantContext;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

/**
 * MySQL tenant-schema outbox. Uses Hibernate's tenant-bound connection, NOT an unscoped DataSource.
 * Claim and acknowledgement are separate transactions so SMTP never runs under a DB lock.
 */
@Repository
public class TenantOutgoingEmailOutboxStore {
    private final EntityManager entityManager;

    // Keep legacy Phase 2 outboxes working until the Phase 3 migration has run everywhere.
    @Value("${mawa.outgoing.feedback.enabled:false}")
    private boolean feedbackEnabled;

    public TenantOutgoingEmailOutboxStore(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    private Session session() {
        if (!StringUtils.hasText(TenantContext.getCurrentTenant())) {
            throw new IllegalStateException("Tenant context required for outgoing mail");
        }
        return entityManager.unwrap(Session.class);
    }

    @Transactional
    public String enqueue(String source, String resourceId, String idempotencyKey, byte[] mime) {
        if (!source.matches("[a-z0-9-]{1,64}") || resourceId == null || resourceId.length() > 128
                || !idempotencyKey.matches("[a-f0-9]{64}") || mime == null || mime.length == 0) {
            throw new IllegalArgumentException("Invalid outgoing mail enqueue request");
        }
        final String newId = UUID.randomUUID().toString();
        return session().doReturningWork(connection -> {
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO outbound_email_message
                    (id, source, resource_id, idempotency_key, state, mime_data,
                     attempt_count, next_attempt_at, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 'QUEUED', ?, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                    ON DUPLICATE KEY UPDATE id = id
                    """)) {
                insert.setString(1, newId);
                insert.setString(2, source);
                insert.setString(3, resourceId);
                insert.setString(4, idempotencyKey);
                insert.setBytes(5, mime);
                insert.executeUpdate();
            }
            try (PreparedStatement find = connection.prepareStatement("""
                    SELECT id FROM outbound_email_message WHERE source = ? AND idempotency_key = ?
                    """)) {
                find.setString(1, source);
                find.setString(2, idempotencyKey);
                try (ResultSet rows = find.executeQuery()) {
                    if (rows.next()) return rows.getString(1);
                }
            }
            throw new SQLException("Outgoing mail enqueue did not persist");
        });
    }

    /** Only called when a recipient HMAC key is configured and Phase 3 schema is migrated. */
    @Transactional
    public String enqueue(String source, String resourceId, String idempotencyKey, byte[] mime,
                          String recipientHash) {
        if (source == null || !source.matches("[a-z0-9-]{1,64}")
                || resourceId == null || resourceId.length() > 128 || resourceId.isBlank()
                || idempotencyKey == null || !idempotencyKey.matches("[a-f0-9]{64}")
                || recipientHash == null || !recipientHash.matches("[a-f0-9]{64}")
                || mime == null || mime.length == 0) {
            throw new IllegalArgumentException("Invalid outgoing mail enqueue request");
        }
        if (!feedbackEnabled) {
            // Keep disabled feature deployments on the exact Phase 2 SQL path.
            return enqueue(source, resourceId, idempotencyKey, mime);
        }
        return session().doReturningWork(connection -> {
            try (PreparedStatement blocked = connection.prepareStatement("""
                    SELECT 1 FROM outbound_email_suppression WHERE recipient_hash=? LIMIT 1
                    """)) {
                blocked.setString(1, recipientHash);
                try (ResultSet row = blocked.executeQuery()) {
                    if (row.next()) throw new IllegalStateException("Email recipient is suppressed");
                }
            }
            String newId = UUID.randomUUID().toString();
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO outbound_email_message
                    (id, source, resource_id, idempotency_key, state, mime_data, recipient_hash,
                     attempt_count, next_attempt_at, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 'QUEUED', ?, ?, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                    ON DUPLICATE KEY UPDATE id=id
                    """)) {
                insert.setString(1, newId);
                insert.setString(2, source);
                insert.setString(3, resourceId);
                insert.setString(4, idempotencyKey);
                insert.setBytes(5, mime);
                insert.setString(6, recipientHash);
                insert.executeUpdate();
            }
            try (PreparedStatement find = connection.prepareStatement("""
                    SELECT id FROM outbound_email_message WHERE source=? AND idempotency_key=?
                    """)) {
                find.setString(1, source);
                find.setString(2, idempotencyKey);
                try (ResultSet rows = find.executeQuery()) {
                    if (rows.next()) return rows.getString(1);
                }
            }
            throw new SQLException("Outgoing mail enqueue did not persist");
        });
    }

    public record Lease(String id, String source, byte[] mime, int attempt, String token) {}
    public record EmailStatus(String requestId, String status, int attempts,
                              String createdAt, String acceptedAt, String deliveredAt,
                              String lastFeedbackAt) {}

    /** Claim with MySQL 8 SKIP LOCKED; a crashed lease can be reclaimed. */
    @Transactional
    public Optional<Lease> claimNext(int maxAttempts) {
        return session().doReturningWork(connection -> {
            // An instance may die mid-SMTP. Expired leases do not bypass max attempts.
            try (PreparedStatement exhausted = connection.prepareStatement("""
                    UPDATE outbound_email_message
                       SET state='FAILED', lease_token=NULL, lease_until=NULL,
                           last_error='WorkerLeaseExpired', updated_at=UTC_TIMESTAMP(6)
                     WHERE state='PROCESSING' AND lease_until < UTC_TIMESTAMP(6)
                       AND attempt_count >= ?
                    """)) {
                exhausted.setInt(1, maxAttempts);
                exhausted.executeUpdate();
            }
            if (feedbackEnabled) {
                // Reject queued mail to previously bounced/complained recipients before SMTP dispatch.
                try (PreparedStatement suppress = connection.prepareStatement("""
                        UPDATE outbound_email_message m
                           SET m.state='SUPPRESSED', m.mime_data=NULL,
                               m.updated_at=UTC_TIMESTAMP(6), m.last_error='RecipientSuppressed'
                         WHERE m.state IN ('QUEUED','RETRY_WAIT')
                           AND m.recipient_hash IS NOT NULL
                           AND EXISTS (SELECT 1 FROM outbound_email_suppression s
                                       WHERE s.recipient_hash=m.recipient_hash)
                        """)) {
                    suppress.executeUpdate();
                }
            }
            String id = null, source = null;
            byte[] data = null;
            int attempts = 0;
            try (PreparedStatement find = connection.prepareStatement("""
                    SELECT id, source, mime_data, attempt_count
                      FROM outbound_email_message
                     WHERE ((state IN ('QUEUED', 'RETRY_WAIT') AND next_attempt_at <= UTC_TIMESTAMP(6))
                         OR (state = 'PROCESSING' AND lease_until < UTC_TIMESTAMP(6)))
                       AND mime_data IS NOT NULL AND attempt_count < ?
                     ORDER BY next_attempt_at, created_at
                     LIMIT 1 FOR UPDATE SKIP LOCKED
                    """)) {
                find.setInt(1, maxAttempts);
                try (ResultSet rows = find.executeQuery()) {
                    if (rows.next()) {
                        id = rows.getString(1);
                        source = rows.getString(2);
                        data = rows.getBytes(3);
                        attempts = rows.getInt(4) + 1;
                    }
                }
            }
            if (id == null) return Optional.empty();
            String token = UUID.randomUUID().toString();
            try (PreparedStatement claim = connection.prepareStatement("""
                    UPDATE outbound_email_message
                       SET state = 'PROCESSING', lease_token = ?,
                           lease_until = DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 180 SECOND),
                           attempt_count = ?, updated_at = UTC_TIMESTAMP(6)
                     WHERE id = ?
                    """)) {
                claim.setString(1, token);
                claim.setInt(2, attempts);
                claim.setString(3, id);
                if (claim.executeUpdate() != 1) throw new SQLException("Unable to claim email");
            }
            return Optional.of(new Lease(id, source, data, attempts, token));
        });
    }

    @Transactional
    public boolean accepted(Lease lease, String smtpMessageId) {
        return session().doReturningWork(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE outbound_email_message
                       SET state='ACCEPTED', mime_data=NULL, lease_token=NULL, lease_until=NULL,
                           accepted_at=UTC_TIMESTAMP(6), updated_at=UTC_TIMESTAMP(6),
                           smtp_message_id=?, last_error=NULL
                     WHERE id=? AND state='PROCESSING' AND lease_token=?
                    """)) {
                update.setString(1, smtpMessageId == null ? null : smtpMessageId.substring(0, Math.min(255, smtpMessageId.length())));
                update.setString(2, lease.id());
                update.setString(3, lease.token());
                if (update.executeUpdate() != 1) return false;
            }
            logAttempt(connection, lease, "ACCEPTED", null);
            return true;
        });
    }

    @Transactional
    public boolean failed(Lease lease, boolean permanent, int maxAttempts, String safeCode) {
        return session().doReturningWork(connection -> {
            boolean terminal = permanent || lease.attempt() >= maxAttempts;
            int delaySeconds = retryDelaySeconds(lease.attempt());
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE outbound_email_message
                       SET state=?, lease_token=NULL, lease_until=NULL, last_error=?,
                           next_attempt_at=DATE_ADD(UTC_TIMESTAMP(6), INTERVAL ? SECOND),
                           updated_at=UTC_TIMESTAMP(6)
                     WHERE id=? AND state='PROCESSING' AND lease_token=?
                    """)) {
                update.setString(1, terminal ? "FAILED" : "RETRY_WAIT");
                update.setString(2, sanitizedCode(safeCode));
                update.setInt(3, delaySeconds);
                update.setString(4, lease.id());
                update.setString(5, lease.token());
                if (update.executeUpdate() != 1) return false;
            }
            logAttempt(connection, lease, terminal ? "FAILED" : "RETRY_WAIT", safeCode);
            return true;
        });
    }

    private static String sanitizedCode(String code) {
        String safe = (code == null ? "Unknown" : code).replaceAll("[^A-Za-z0-9_]", "_");
        return safe.substring(0, Math.min(64, safe.length()));
    }

    static int retryDelaySeconds(int attempt) {
        return (int) Math.min(3600L, 30L << Math.min(7, Math.max(0, attempt - 1)));
    }

    private void logAttempt(Connection c, Lease lease, String state, String code) throws SQLException {
        try (PreparedStatement insert = c.prepareStatement("""
                INSERT INTO outbound_email_attempt (email_id, attempt_no, result_state, error_code, completed_at)
                VALUES (?, ?, ?, ?, UTC_TIMESTAMP(6))
                """)) {
            insert.setString(1, lease.id());
            insert.setInt(2, lease.attempt());
            insert.setString(3, state);
            insert.setString(4, code == null ? null : sanitizedCode(code));
            insert.executeUpdate();
        }
    }

    @Transactional(readOnly = true)
    public Optional<EmailStatus> findInvoiceStatus(String invoiceId, String requestId) {
        return session().doReturningWork(connection -> {
            try (PreparedStatement find = connection.prepareStatement("""
                    SELECT state, attempt_count, created_at, accepted_at, delivered_at, last_feedback_at
                      FROM outbound_email_message
                     WHERE id=? AND source='invoice' AND resource_id=?
                    """)) {
                find.setString(1, requestId);
                find.setString(2, invoiceId);
                try (ResultSet rows = find.executeQuery()) {
                    if (!rows.next()) return Optional.empty();
                    return Optional.of(new EmailStatus(requestId, rows.getString(1), rows.getInt(2),
                            rows.getString(3), rows.getString(4), rows.getString(5), rows.getString(6)));
                }
            }
        });
    }

    /**
     * Apply a provider adapter's VERIFIED event. A transactional row lock serialises
     * status updates and event replays per message. Do not use from public endpoints.
     */
    @Transactional
    public boolean applyVerifiedFeedback(String requestId, String providerEventId, String eventType) {
        if (!feedbackEnabled) throw new IllegalStateException("Outgoing delivery feedback is disabled");
        if (!java.util.Set.of("DELIVERED", "BOUNCED", "COMPLAINT").contains(eventType)) {
            throw new IllegalArgumentException("Unsupported delivery event type");
        }
        return session().doReturningWork(connection -> {
            String current, recipientHash;
            try (PreparedStatement lookup = connection.prepareStatement("""
                    SELECT state, recipient_hash FROM outbound_email_message WHERE id=? FOR UPDATE
                    """)) {
                lookup.setString(1, requestId);
                try (ResultSet row = lookup.executeQuery()) {
                    if (!row.next()) return false;
                    current = row.getString(1);
                    recipientHash = row.getString(2);
                }
            }
            // Event replay is idempotent. A reused provider event identifier with a different
            // recipient/message or type must fail rather than mutate the wrong tenant record.
            try (PreparedStatement replay = connection.prepareStatement("""
                    SELECT email_id, event_type FROM outbound_email_delivery_event WHERE provider_event_id=?
                    """)) {
                replay.setString(1, providerEventId);
                try (ResultSet row = replay.executeQuery()) {
                    if (row.next()) {
                        if (!requestId.equals(row.getString(1)) || !eventType.equals(row.getString(2))) {
                            throw new IllegalStateException("Conflicting provider delivery event ID");
                        }
                        return true;
                    }
                }
            }
            // Do not invent provider delivery where SMTP hasn't acknowledged acceptance.
            if (!java.util.Set.of("ACCEPTED", "DELIVERED", "BOUNCED", "COMPLAINED")
                    .contains(current)) {
                throw new IllegalStateException("Email was not accepted by its sending provider");
            }
            String next = switch (eventType) {
                case "DELIVERED" -> current.equals("BOUNCED") || current.equals("COMPLAINED")
                        ? current : "DELIVERED";
                case "BOUNCED" -> current.equals("COMPLAINED") ? current : "BOUNCED";
                case "COMPLAINT" -> "COMPLAINED";
                default -> throw new IllegalArgumentException("Unsupported delivery event type");
            };
            try (PreparedStatement event = connection.prepareStatement("""
                    INSERT INTO outbound_email_delivery_event
                    (provider_event_id, email_id, event_type, received_at)
                    VALUES (?, ?, ?, UTC_TIMESTAMP(6))
                    """)) {
                event.setString(1, providerEventId);
                event.setString(2, requestId);
                event.setString(3, eventType);
                event.executeUpdate();
            }
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE outbound_email_message
                       SET state=?, updated_at=UTC_TIMESTAMP(6), last_feedback_at=UTC_TIMESTAMP(6),
                           delivered_at=CASE WHEN ?='DELIVERED' AND delivered_at IS NULL
                                             THEN UTC_TIMESTAMP(6) ELSE delivered_at END
                     WHERE id=?
                    """)) {
                update.setString(1, next);
                update.setString(2, next);
                update.setString(3, requestId);
                update.executeUpdate();
            }
            if ((eventType.equals("BOUNCED") || eventType.equals("COMPLAINT"))
                    && recipientHash != null) {
                try (PreparedStatement suppress = connection.prepareStatement("""
                        INSERT INTO outbound_email_suppression
                        (recipient_hash, reason, first_email_id, created_at, updated_at)
                        VALUES (?, ?, ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                        ON DUPLICATE KEY UPDATE reason=VALUES(reason), updated_at=UTC_TIMESTAMP(6)
                        """)) {
                    suppress.setString(1, recipientHash);
                    suppress.setString(2, eventType);
                    suppress.setString(3, requestId);
                    suppress.executeUpdate();
                }
            }
            return true;
        });
    }

    /** Keep status/attempt audit; remove obsolete unsuccessful MIME content after retention. */
    @Transactional
    public int pruneFailedMime(int days) {
        return session().doReturningWork(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE outbound_email_message SET mime_data=NULL, updated_at=UTC_TIMESTAMP(6)
                    WHERE state='FAILED' AND mime_data IS NOT NULL
                      AND created_at < DATE_SUB(UTC_TIMESTAMP(6), INTERVAL ? DAY)
                    """)) {
                update.setInt(1, Math.max(1, days));
                return update.executeUpdate();
            }
        });
    }
}
