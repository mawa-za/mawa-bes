package za.co.mawa.bes.service;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.configuration.context.TenantContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Atomic, tenant-local daily submission limits for the internal gateway only.
 * Reservations participate in the SAME transaction as replay nonce and outbox insert.
 * Replays of a previously enqueued (source, resource, eventId) never consume capacity again.
 * Exchange Online throughput limits are separate and must be controlled operationally.
 */
@Service
public class OutgoingGatewayQuotaService {
    private final EntityManager entityManager;
    private final boolean enabled;
    private final int adminDailyLimit;
    private final int billingDailyLimit;
    private final int reportingDailyLimit;

    public OutgoingGatewayQuotaService(EntityManager entityManager,
            @Value("${mawa.outgoing.gateway.quota.enabled:false}") boolean enabled,
            @Value("${mawa.outgoing.gateway.quota.admin.daily-limit:50}") int adminDailyLimit,
            @Value("${mawa.outgoing.gateway.quota.billing.daily-limit:500}") int billingDailyLimit,
            @Value("${mawa.outgoing.gateway.quota.reporting.daily-limit:100}") int reportingDailyLimit) {
        this.entityManager = entityManager;
        this.enabled = enabled;
        this.adminDailyLimit = adminDailyLimit;
        this.billingDailyLimit = billingDailyLimit;
        this.reportingDailyLimit = reportingDailyLimit;
        if (adminDailyLimit < 1 || billingDailyLimit < 1 || reportingDailyLimit < 1) {
            throw new IllegalArgumentException("Outgoing gateway daily limits must be positive");
        }
    }

    public static final class QuotaExceededException extends RuntimeException {
        public QuotaExceededException() { super("Daily outgoing email submission quota reached"); }
    }

    /** Reserve capacity ONLY for a new event; caller must have a bound tenant transaction. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reserve(String client, String resourceId, String eventId) {
        if (TenantContext.getCurrentTenant() == null || TenantContext.getCurrentTenant().isBlank()) {
            throw new SecurityException("Tenant context required for outgoing email quota");
        }
        int max = switch (client) {
            case "admin" -> adminDailyLimit;
            case "billing" -> billingDailyLimit;
            case "reporting" -> reportingDailyLimit;
            default -> throw new SecurityException("Unknown outgoing email service");
        };
        if (resourceId == null || !resourceId.matches("[A-Za-z0-9._/-]{1,100}")
                || eventId == null || !eventId.matches("[A-Za-z0-9_-]{8,100}")) {
            throw new IllegalArgumentException("Invalid outgoing email quota event");
        }
        if (!enabled) return; // No SQL references to the new table before the migration is applied.
        final String dedup = QueuedOutgoingEmailService.hash(client + "\n" + resourceId + "\n" + eventId);
        entityManager.unwrap(Session.class).doWork(connection -> {
            // PRIMARY KEY on (client_id,quota_day) serializes the daily budget on all Cloud Run workers.
            try (PreparedStatement init = connection.prepareStatement("""
                    INSERT INTO outbound_email_gateway_daily_quota
                    (client_id, quota_day, accepted_count, updated_at)
                    VALUES (?, UTC_DATE(), 0, UTC_TIMESTAMP(6))
                    ON DUPLICATE KEY UPDATE client_id=client_id
                    """)) {
                init.setString(1, client);
                init.executeUpdate();
            }
            int used;
            try (PreparedStatement lock = connection.prepareStatement("""
                    SELECT accepted_count FROM outbound_email_gateway_daily_quota
                    WHERE client_id=? AND quota_day=UTC_DATE() FOR UPDATE
                    """)) {
                lock.setString(1, client);
                try (ResultSet rows = lock.executeQuery()) {
                    if (!rows.next()) throw new SQLException("Outgoing mail quota row unavailable");
                    used = rows.getInt(1);
                }
            }
            // The stable outbox key is the authority for retries, even across UTC days.
            try (PreparedStatement existing = connection.prepareStatement("""
                    SELECT id FROM outbound_email_message
                    WHERE source=? AND idempotency_key=? LIMIT 1 FOR UPDATE
                    """)) {
                existing.setString(1, client);
                existing.setString(2, dedup);
                try (ResultSet rows = existing.executeQuery()) {
                    if (rows.next()) return;
                }
            }
            if (used >= max) throw new QuotaExceededException();
            try (PreparedStatement charge = connection.prepareStatement("""
                    UPDATE outbound_email_gateway_daily_quota
                    SET accepted_count=accepted_count+1, updated_at=UTC_TIMESTAMP(6)
                    WHERE client_id=? AND quota_day=UTC_DATE() AND accepted_count < ?
                    """)) {
                charge.setString(1, client);
                charge.setInt(2, max);
                if (charge.executeUpdate() != 1) throw new QuotaExceededException();
            }
        });
    }
}
