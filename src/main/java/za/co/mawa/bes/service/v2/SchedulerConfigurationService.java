package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.v2.schedule.ScheduledJobSettingsDto;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SchedulerConfigurationService {
    private final MigrateService migrateService;

    public List<ScheduledJobSettingsDto> getJobs() {
        return List.of(getLegacyMembershipMigrationJob());
    }

    public ScheduledJobSettingsDto getJob(String jobCode) {
        if (MigrateService.LEGACY_MEMBERSHIP_MIGRATION_JOB.equalsIgnoreCase(jobCode)) {
            return getLegacyMembershipMigrationJob();
        }
        throw new IllegalArgumentException("Unknown scheduled job: " + jobCode);
    }

    public ScheduledJobSettingsDto updateJob(String jobCode, ScheduledJobSettingsDto request) {
        if (!MigrateService.LEGACY_MEMBERSHIP_MIGRATION_JOB.equalsIgnoreCase(jobCode)) {
            throw new IllegalArgumentException("Unknown scheduled job: " + jobCode);
        }
        migrateService.updateScheduledMigrationSettings(request.isEnabled(), request.getIntervalMinutes());
        return getLegacyMembershipMigrationJob();
    }

    public Object runNow(String jobCode) {
        if (MigrateService.LEGACY_MEMBERSHIP_MIGRATION_JOB.equalsIgnoreCase(jobCode)) {
            return migrateService.runScheduledMigrationNow();
        }
        throw new IllegalArgumentException("Unknown scheduled job: " + jobCode);
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
