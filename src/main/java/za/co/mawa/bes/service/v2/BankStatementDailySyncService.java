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

    // Poll every minute when the service is running. Catch up if no instance was
    // available at the selected wall-clock time; the successful scan date is tenant-local.
    @Scheduled(fixedDelayString = "${mawa.bank-statements.dispatcher-delay-ms:60000}",
               initialDelayString = "${mawa.bank-statements.initial-delay-ms:30000}")
    public void syncAllTenants() {
        LocalDate today = LocalDate.now(CAT);
        LocalTime now = LocalTime.now(CAT);
        for (TenantDto tenant : tenants.getAll()) {
            if (tenant == null || tenant.getId() == null || tenant.getId().isBlank()) continue;
            try {
                TenantContext.setCurrentTenant(tenant.getId());
                background.establish();
                var configuration = settings.get();
                if (shouldRun(configuration, today, now)) {
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

    static boolean shouldRun(BankStatementMailConfigurationService.Configuration configuration,
                             LocalDate today, LocalTime now) {
        if (!configuration.enabled() || today.toString().equals(configuration.lastSuccessDate()))
            return false;
        String configured = configuration.dailyRunTime();
        LocalTime runTime = LocalTime.parse(configured == null
                ? BankStatementMailConfigurationService.DEFAULT_DAILY_RUN_TIME : configured);
        return !now.isBefore(runTime);
    }
}
