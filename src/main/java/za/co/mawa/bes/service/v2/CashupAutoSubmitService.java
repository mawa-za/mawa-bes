package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.dto.v2.payapp.CashupSubmitForApprovalRequest;
import za.co.mawa.bes.entity.v2.CashupEntity;
import za.co.mawa.bes.repository.v2.CashupRepository;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.service.TenantAdminService;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashupAutoSubmitService {
    public static final String CASHUP_AUTO_SUBMIT_JOB = "CASHUP_AUTO_SUBMIT";
    private static final String GROUP = "CASHUP-AUTO-SUBMIT";
    private static final String ENABLED = "ENABLED";
    private static final String RUN_TIME = "RUN-TIME";
    private static final String LAST_RUN_AT = "LAST-RUN-AT";
    private static final String STATUS_OPEN = "OPEN";
    private static final LocalTime DEFAULT_RUN_TIME = LocalTime.MIDNIGHT;
    private static final ZoneId CASHUP_ZONE = ZoneId.of("Africa/Johannesburg");
    private static final DateTimeFormatter RUN_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final TenantAdminService tenantAdminService;
    private final SettingService settingService;
    private final CashupRepository cashupRepository;
    private final CashupService cashupService;

    @Scheduled(fixedDelayString = "${mawa.scheduler.dispatcher-delay-ms:30000}")
    public void submitOpenCashupsForAllTenants() {
        final List<TenantDto> tenants;
        try {
            tenants = tenantAdminService.getAll();
        } catch (RuntimeException ex) {
            log.error("Cashup auto-submit skipped because tenant discovery is unavailable: {}", ex.getMessage());
            return;
        }

        for (TenantDto tenant : tenants) {
            if (tenant == null || tenant.getId() == null || tenant.getId().isBlank()) {
                continue;
            }
            try {
                TenantContext.setCurrentTenant(tenant.getId());
                if (isEnabled() && isDue()) {
                    Map<String, Object> result = submitCurrentTenantOpenCashups(scheduledCutoff());
                    markRun();
                    log.info("Cashup auto-submit completed for tenant {}: {}", tenant.getId(), result);
                }
            } catch (Exception ex) {
                log.error("Cashup auto-submit failed for tenant {}: {}", tenant.getId(), ex.getMessage(), ex);
            } finally {
                TenantContext.clear();
            }
        }
    }

    public boolean isEnabled() {
        String value = settingService.getSetting(ENABLED, GROUP);
        return value == null || value.isBlank()
                || "true".equalsIgnoreCase(value) || "1".equals(value) || "Y".equalsIgnoreCase(value);
    }

    public String getRunTime() {
        return resolveRunTime().format(RUN_TIME_FORMAT);
    }

    public LocalDateTime getLastRunAt() {
        String value = settingService.getSetting(LAST_RUN_AT, GROUP);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    public LocalDateTime getNextRunAt() {
        if (!isEnabled()) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now(CASHUP_ZONE);
        LocalDateTime lastRun = getLastRunAt();
        LocalDateTime scheduledToday = now.toLocalDate().atTime(resolveRunTime());
        if (lastRun == null) {
            return scheduledToday.isAfter(now) ? scheduledToday : now;
        }
        LocalDateTime next = lastRun.toLocalDate().plusDays(1).atTime(resolveRunTime());
        return next.isAfter(now) ? next : now;
    }

    public void updateSettings(boolean enabled, String runTime) {
        LocalTime parsed = parseRunTime(runTime);
        settingService.upsertSetting(ENABLED, GROUP, String.valueOf(enabled));
        settingService.upsertSetting(RUN_TIME, GROUP, parsed.format(RUN_TIME_FORMAT));
    }

    public Map<String, Object> runNow() {
        Map<String, Object> result = submitCurrentTenantOpenCashups();
        markRun();
        return result;
    }

    public Map<String, Object> submitCurrentTenantOpenCashups() {
        return submitCashups(cashupRepository
                .findByStatusIgnoreCaseOrderByCashupDateAscCreatedAtAsc(STATUS_OPEN));
    }

    private Map<String, Object> submitCurrentTenantOpenCashups(LocalDateTime cutoff) {
        return submitCashups(cashupRepository
                .findByStatusIgnoreCaseAndCreatedAtLessThanEqualOrderByCashupDateAscCreatedAtAsc(STATUS_OPEN, cutoff));
    }

    private Map<String, Object> submitCashups(List<CashupEntity> openCashups) {
        int submitted = 0;
        int failed = 0;
        for (CashupEntity cashup : openCashups) {
            try {
                CashupSubmitForApprovalRequest request = new CashupSubmitForApprovalRequest();
                request.setRequesterId(cashup.getUserId());
                cashupService.submitForApproval(cashup.getId(), request);
                submitted++;
            } catch (Exception ex) {
                failed++;
                log.error("Unable to auto-submit cashup {}: {}", cashup.getId(), ex.getMessage(), ex);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("openCashupsFound", openCashups.size());
        result.put("submitted", submitted);
        result.put("failed", failed);
        return result;
    }

    private LocalDateTime scheduledCutoff() {
        return LocalDateTime.now(CASHUP_ZONE).toLocalDate().atTime(resolveRunTime());
    }

    private boolean isDue() {
        LocalDateTime now = LocalDateTime.now(CASHUP_ZONE);
        LocalDateTime scheduledToday = now.toLocalDate().atTime(resolveRunTime());
        if (now.isBefore(scheduledToday)) {
            return false;
        }
        LocalDateTime lastRun = getLastRunAt();
        return lastRun == null || lastRun.toLocalDate().isBefore(now.toLocalDate());
    }

    private void markRun() {
        settingService.upsertSetting(LAST_RUN_AT, GROUP, LocalDateTime.now(CASHUP_ZONE).toString());
    }

    private LocalTime resolveRunTime() {
        String value = settingService.getSetting(RUN_TIME, GROUP);
        if (value == null || value.isBlank()) {
            return DEFAULT_RUN_TIME;
        }
        try {
            return LocalTime.parse(value.trim(), RUN_TIME_FORMAT);
        } catch (DateTimeParseException ignored) {
            return DEFAULT_RUN_TIME;
        }
    }

    private LocalTime parseRunTime(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_RUN_TIME;
        }
        try {
            return LocalTime.parse(value.trim(), RUN_TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("runTime must use HH:mm format");
        }
    }
}
