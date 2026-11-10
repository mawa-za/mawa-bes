package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.service.TenantAdminService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@Service @RequiredArgsConstructor @Slf4j
public class BankStatementDailySyncService {
    private static final ZoneId CAT = ZoneId.of("Africa/Johannesburg");
    private final TenantAdminService tenants;
    private final BankStatementMailConfigurationService settings;
    private final BankStatementMailboxService mailbox;
    private final BackgroundExecutionContextService background;

    // Catch up after 04:00 CAT if a Cloud Run instance was asleep at the exact time.
    @Scheduled(fixedDelayString = "${mawa.bank-statements.dispatcher-delay-ms:1800000}",
               initialDelayString = "${mawa.bank-statements.initial-delay-ms:30000}")
    public void syncAllTenants() {
        if (LocalTime.now(CAT).isBefore(LocalTime.of(4, 0))) return;
        for (TenantDto tenant : tenants.getAll()) {
            if (tenant == null || tenant.getId() == null || tenant.getId().isBlank()) continue;
            try {
                TenantContext.setCurrentTenant(tenant.getId());
                background.establish();
                var configuration = settings.get();
                if (configuration.enabled() && !LocalDate.now(CAT).toString().equals(configuration.lastSuccessDate())) {
                    var result = mailbox.sync();
                    log.info("Daily bank statement sync: tenant={}, attachments={}, new={}, failures={}",
                            tenant.getId(), result.attachments(), result.newTransactions(), result.failures());
                }
            } catch (Exception ex) {
                log.warn("Bank statement sync failed for tenant {} ({}).", tenant.getId(), ex.getClass().getSimpleName());
            } finally {
                background.clear();
                TenantContext.clear();
            }
        }
    }
}
