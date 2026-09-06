package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.service.TenantAdminService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FuneralMortuaryAutoCheckoutService {
    private static final String GROUP = "FUNERAL-SERVICE";
    private static final String ENABLED = "AUTOMATIC-MORTUARY-CHECKOUT-ENABLED";
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Africa/Johannesburg");

    private final TenantAdminService tenantAdminService;
    private final BackgroundExecutionContextService backgroundExecutionContextService;
    private final SettingService settingService;
    private final JdbcTemplate jdbcTemplate;

    @Scheduled(fixedDelayString = "${mawa.scheduler.dispatcher-delay-ms:30000}")
    public void checkoutForAllTenants() {
        final List<TenantDto> tenants;
        try {
            tenants = tenantAdminService.getAll();
        } catch (RuntimeException error) {
            log.error("Automatic mortuary checkout skipped: {}", error.getMessage());
            return;
        }
        for (TenantDto tenant : tenants) {
            if (tenant == null || tenant.getId() == null || tenant.getId().isBlank()) continue;
            try {
                TenantContext.setCurrentTenant(tenant.getId());
                backgroundExecutionContextService.establish();
                if (Boolean.parseBoolean(settingService.getSetting(ENABLED, GROUP))) {
                    int count = checkoutPastFunerals();
                    if (count > 0) log.info("Automatically checked out {} deceased record(s) for tenant {}", count, tenant.getId());
                }
            } catch (Exception error) {
                log.error("Automatic mortuary checkout failed for tenant {}: {}", tenant.getId(), error.getMessage(), error);
            } finally {
                backgroundExecutionContextService.clear();
                TenantContext.clear();
            }
        }
    }

    int checkoutPastFunerals() {
        return jdbcTemplate.update("""
                UPDATE funeral_mortuary_inventory inventory
                JOIN funeral_service service ON service.mortuary_inventory_id = inventory.id
                   SET inventory.status = 'CHECKED_OUT',
                       inventory.checkout_date = UTC_TIMESTAMP(),
                       inventory.release_to = COALESCE(NULLIF(inventory.release_to, ''), 'AUTOMATED - FUNERAL DATE PASSED'),
                       inventory.updated_at = UTC_TIMESTAMP()
                 WHERE inventory.status = 'IN_MORTUARY'
                   AND UPPER(COALESCE(service.status, '')) <> 'CANCELLED'
                   AND service.funeral_date < ?
                """, LocalDate.now(DEFAULT_ZONE));
    }
}
