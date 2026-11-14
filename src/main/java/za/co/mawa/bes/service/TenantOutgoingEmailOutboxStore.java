package za.co.mawa.bes.service;

import jakarta.persistence.EntityManager;
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

    public record Lease(String id, String source, byte[] mime, int attempt, String token) {}
    public record EmailStatus(String requestId, String status, int attempts,
                              String createdAt, String acceptedAt) {}

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
                    SELECT state, attempt_count, created_at, accepted_at
                      FROM outbound_email_message
                     WHERE id=? AND source='invoice' AND resource_id=?
                    """)) {
                find.setString(1, requestId);
                find.setString(2, invoiceId);
                try (ResultSet rows = find.executeQuery()) {
                    if (!rows.next()) return Optional.empty();
                    return Optional.of(new EmailStatus(requestId, rows.getString(1), rows.getInt(2),
                            rows.getString(3), rows.getString(4)));
                }
            }
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
