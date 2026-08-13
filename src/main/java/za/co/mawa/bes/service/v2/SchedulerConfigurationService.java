package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.v2.schedule.ScheduledJobSettingsDto;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SchedulerConfigurationService {
    private final MigrateService migrateService;
    private final CashupAutoSubmitService cashupAutoSubmitService;

    public List<ScheduledJobSettingsDto> getJobs() {
        return List.of(getLegacyMembershipMigrationJob(), getCashupAutoSubmitJob());
    }

    public ScheduledJobSettingsDto getJob(String jobCode) {
        if (MigrateService.LEGACY_MEMBERSHIP_MIGRATION_JOB.equalsIgnoreCase(jobCode)) {
            return getLegacyMembershipMigrationJob();
        }
        if (CashupAutoSubmitService.CASHUP_AUTO_SUBMIT_JOB.equalsIgnoreCase(jobCode)) {
            return getCashupAutoSubmitJob();
        }
        throw new IllegalArgumentException("Unknown scheduled job: " + jobCode);
    }

    public ScheduledJobSettingsDto updateJob(String jobCode, ScheduledJobSettingsDto request) {
        if (MigrateService.LEGACY_MEMBERSHIP_MIGRATION_JOB.equalsIgnoreCase(jobCode)) {
            migrateService.updateScheduledMigrationSettings(request.isEnabled(), request.getIntervalMinutes());
            return getLegacyMembershipMigrationJob();
        }
        if (CashupAutoSubmitService.CASHUP_AUTO_SUBMIT_JOB.equalsIgnoreCase(jobCode)) {
            cashupAutoSubmitService.updateSettings(request.isEnabled(), request.getRunTime());
            return getCashupAutoSubmitJob();
        }
        throw new IllegalArgumentException("Unknown scheduled job: " + jobCode);
    }

    public Object runNow(String jobCode) {
        if (MigrateService.LEGACY_MEMBERSHIP_MIGRATION_JOB.equalsIgnoreCase(jobCode)) {
            return migrateService.runScheduledMigrationNow();
        }
        if (CashupAutoSubmitService.CASHUP_AUTO_SUBMIT_JOB.equalsIgnoreCase(jobCode)) {
            return cashupAutoSubmitService.runNow();
        }
        throw new IllegalArgumentException("Unknown scheduled job: " + jobCode);
    }

    private ScheduledJobSettingsDto getCashupAutoSubmitJob() {
        return ScheduledJobSettingsDto.builder()
                .jobCode(CashupAutoSubmitService.CASHUP_AUTO_SUBMIT_JOB)
                .name("Open cashup auto-submit")
                .description("Automatically closes all OPEN cashups into AWAITING_DEPOSITS once per day at the configured tenant time, matching a manual/device cashup close.")
                .enabled(cashupAutoSubmitService.isEnabled())
                .runTime(cashupAutoSubmitService.getRunTime())
                .lastRunAt(cashupAutoSubmitService.getLastRunAt() == null ? null : cashupAutoSubmitService.getLastRunAt().toString())
                .nextRunAt(cashupAutoSubmitService.getNextRunAt() == null ? null : cashupAutoSubmitService.getNextRunAt().toString())
                .build();
    }

    private ScheduledJobSettingsDto getLegacyMembershipMigrationJob() {
        return ScheduledJobSettingsDto.builder()
                .jobCode(MigrateService.LEGACY_MEMBERSHIP_MIGRATION_JOB)
                .name("Legacy membership migration")
                .description("Migrates old transaction-based memberships into the v2 membership tables. Keep disabled after migration is complete.")
                .enabled(migrateService.isScheduledMigrationEnabled())
                .intervalMinutes(migrateService.getScheduledMigrationIntervalMinutes())
                .lastRunAt(migrateService.getScheduledMigrationLastRunAt() == null ? null : migrateService.getScheduledMigrationLastRunAt().toString())
                .nextRunAt(migrateService.getScheduledMigrationNextRunAt() == null ? null : migrateService.getScheduledMigrationNextRunAt().toString())
                .build();
    }
}
