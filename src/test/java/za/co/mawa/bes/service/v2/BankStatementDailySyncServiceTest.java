package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class BankStatementDailySyncServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 18);

    private BankStatementMailConfigurationService.Configuration configuration(
            boolean enabled, String time, String lastSuccess) {
        return new BankStatementMailConfigurationService.Configuration(
                enabled, "GMAIL", "bank@example.org", "sender@example.org", "", "", "", "", "",
                14, lastSuccess, "001234", true, "", "", "", "", time);
    }

    @Test void usesPerTenantRunTimeAndCatchesUpAfterScheduledTime() {
        var early = configuration(true, "09:30", null);
        var late = configuration(true, "16:45", null);
        assertFalse(BankStatementDailySyncService.shouldRun(early, TODAY, LocalTime.of(9, 29)));
        assertTrue(BankStatementDailySyncService.shouldRun(early, TODAY, LocalTime.of(9, 30)));
        assertTrue(BankStatementDailySyncService.shouldRun(early, TODAY, LocalTime.of(12, 0)));
        assertFalse(BankStatementDailySyncService.shouldRun(late, TODAY, LocalTime.of(12, 0)));
        assertTrue(BankStatementDailySyncService.shouldRun(late, TODAY, LocalTime.of(16, 45)));
    }

    @Test void honorsDefaultFourAmMidnightAndSuccessfulDailyDedup() {
        var oldTenant = configuration(true, null, null);
        assertFalse(BankStatementDailySyncService.shouldRun(oldTenant, TODAY, LocalTime.of(3, 59)));
        assertTrue(BankStatementDailySyncService.shouldRun(oldTenant, TODAY, LocalTime.of(4, 0)));
        assertTrue(BankStatementDailySyncService.shouldRun(configuration(true, "00:00", null),
                TODAY, LocalTime.MIDNIGHT));
        assertFalse(BankStatementDailySyncService.shouldRun(configuration(true, "00:00", TODAY.toString()),
                TODAY, LocalTime.of(23, 59)));
        assertFalse(BankStatementDailySyncService.shouldRun(configuration(false, "00:00", null),
                TODAY, LocalTime.of(23, 59)));
        assertTrue(BankStatementDailySyncService.shouldRun(configuration(true, "04:00", "2026-11-17"),
                TODAY, LocalTime.of(7, 0)));
    }
}
