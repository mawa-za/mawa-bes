package za.co.mawa.bes.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.service.v2.BackgroundExecutionContextService;

import java.util.List;
import java.util.Optional;

/** Polls tenant-local MySQL outboxes with a safe background identity. Never runs SMTP in a DB transaction. */
@Service
public class OutgoingEmailQueueWorker {
    private static final Logger log = LoggerFactory.getLogger(OutgoingEmailQueueWorker.class);
    private final TenantAdminService tenants;
    private final BackgroundExecutionContextService background;
    private final TenantOutgoingEmailOutboxStore store;
    private final SharedOutgoingEmailService transport;
    private final boolean enabled;
    private final int maxAttempts;

    public OutgoingEmailQueueWorker(TenantAdminService tenants,
                                    BackgroundExecutionContextService background,
                                    TenantOutgoingEmailOutboxStore store,
                                    SharedOutgoingEmailService transport,
                                    @Value("${mawa.outgoing.queue.enabled:false}") boolean enabled,
                                    @Value("${mawa.outgoing.queue.max-attempts:5}") int maxAttempts) {
        this.tenants = tenants;
        this.background = background;
        this.store = store;
        this.transport = transport;
        this.enabled = enabled;
        this.maxAttempts = Math.max(1, Math.min(maxAttempts, 20));
    }

    @Scheduled(fixedDelayString = "${mawa.outgoing.queue.poll-ms:15000}", initialDelay = 15000)
    public void dispatch() {
        if (!enabled) return;
        final List<TenantDto> all;
        try {
            all = tenants.getAll();
        } catch (Exception e) {
            log.warn("Outgoing mail tenant discovery unavailable ({})", e.getClass().getSimpleName());
            return;
        }
        for (TenantDto tenant : all) {
            if (tenant == null || tenant.getId() == null ||
                    !tenant.getId().matches("[A-Za-z0-9_-]{1,128}")) continue;
            try {
                TenantContext.setCurrentTenant(tenant.getId());
                background.establish();
                for (int i = 0; i < 5; i++) {
                    Optional<TenantOutgoingEmailOutboxStore.Lease> lease = store.claimNext(maxAttempts);
                    if (lease.isEmpty()) break;
                    deliver(lease.get());
                }
            } catch (Exception e) {
                log.warn("Outgoing mail worker failed tenant={} error={}", tenant.getId(),
                        e.getClass().getSimpleName());
            } finally {
                background.clear();
                TenantContext.clear();
            }
        }
    }

    private void deliver(TenantOutgoingEmailOutboxStore.Lease item) {
        try {
            String smtpMessageId = transport.sendPrepared(item.mime(), item.source(), item.id());
            if (!store.accepted(item, smtpMessageId)) {
                log.warn("Outgoing mail lease expired before acceptance acknowledgement id={}", item.id());
            }
        } catch (Exception ex) {
            boolean permanent = ex instanceof MailAuthenticationException
                    || ex instanceof MailParseException || ex instanceof IllegalArgumentException;
            try {
                store.failed(item, permanent, maxAttempts, ex.getClass().getSimpleName());
            } catch (Exception persistenceError) {
                // Lease expires and message becomes claimable. Delivery could have been accepted already:
                // SMTP cannot guarantee exactly-once in this crash window.
                log.error("Outgoing mail attempt outcome not persisted id={} error={}",
                        item.id(), persistenceError.getClass().getSimpleName());
            }
        }
    }

    @Scheduled(cron = "${mawa.outgoing.queue.cleanup-cron:0 10 3 * * *}")
    public void cleanup() {
        if (!enabled) return;
        try {
            for (TenantDto tenant : tenants.getAll()) {
                if (tenant == null || tenant.getId() == null ||
                        !tenant.getId().matches("[A-Za-z0-9_-]{1,128}")) continue;
                try {
                    TenantContext.setCurrentTenant(tenant.getId());
                    background.establish();
                    store.pruneFailedMime(7);
                } catch (Exception ex) {
                    log.warn("Outgoing email retention cleanup failed tenant={} error={}",
                            tenant.getId(), ex.getClass().getSimpleName());
                } finally {
                    background.clear();
                    TenantContext.clear();
                }
            }
        } catch (Exception e) {
            log.warn("Outgoing email retention sweep unavailable ({})", e.getClass().getSimpleName());
        }
    }
}
