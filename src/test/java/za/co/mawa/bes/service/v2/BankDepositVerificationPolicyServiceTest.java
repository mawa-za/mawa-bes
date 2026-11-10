package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.service.SettingService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BankDepositVerificationPolicyServiceTest {
    private final SettingService settings = mock(SettingService.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final BankTransactionService bank = mock(BankTransactionService.class);
    private final BankDepositVerificationPolicyService policy = new BankDepositVerificationPolicyService(settings, jdbc, bank);

    @AfterEach void reset() { UserContext.clear(); }

    @Test void defaultOffAllowsUnlinkedEftButAlwaysChecksSuppliedDeposit() {
        assertFalse(policy.get().requiredNow());
        assertNull(policy.allocateIfProvided("", 10000L, "PAYMENT_BATCH", "payment-1", "cashier", LocalDateTime.now()));
        assertNull(policy.allocateIfProvided(null, 10000L, "PAYMENT_BATCH", "payment-1", "cashier", LocalDateTime.now()));
        when(bank.allocate("DEP-00000001", 10000L, "PAYMENT_BATCH", "payment-1", "cashier")).thenReturn(1L);
        assertEquals(1L, policy.allocateIfProvided("DEP-00000001", 10000L, "PAYMENT_BATCH", "payment-1", "cashier", LocalDateTime.now()));
        verify(bank, times(1)).allocate(anyString(), anyLong(), anyString(), anyString(), anyString());
    }

    @Test void enabledPolicyRequiresNewDepositsButGrandfathersEarlierCaptures() {
        when(settings.getSetting("REQUIRE-DEPOSIT-ID", BankDepositVerificationPolicyService.GROUP)).thenReturn("true");
        when(settings.getSetting("EFFECTIVE-FROM", BankDepositVerificationPolicyService.GROUP)).thenReturn("2026-11-10");
        // UTC+2, so UTC 21:00 the previous calendar day is 23:00 CAT.
        assertFalse(policy.requiredAt(LocalDateTime.of(2026, 11, 9, 21, 0)));
        assertTrue(policy.requiredAt(LocalDateTime.of(2026, 11, 9, 22, 0)));
        assertDoesNotThrow(() -> policy.validateProvidedOrOptional(null, LocalDateTime.of(2026, 11, 9, 21, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> policy.validateProvidedOrOptional(null, LocalDateTime.of(2026, 11, 10, 10, 0)));
    }


    @Test void activationTimestampProtectsEarlierSameDayTransactions() {
        when(settings.getSetting("REQUIRE-DEPOSIT-ID", BankDepositVerificationPolicyService.GROUP)).thenReturn("true");
        when(settings.getSetting("EFFECTIVE-FROM", BankDepositVerificationPolicyService.GROUP)).thenReturn("2026-11-10");
        when(settings.getSetting("ACTIVATED-AT-UTC", BankDepositVerificationPolicyService.GROUP)).thenReturn("2026-11-10T12:00:00");
        assertFalse(policy.requiredAt(LocalDateTime.of(2026, 11, 10, 11, 59)));
        assertTrue(policy.requiredAt(LocalDateTime.of(2026, 11, 10, 12, 1)));
    }

    @Test void enablingEnforcementInPastIsRejectedWithoutWrites() {
        assertThrows(IllegalArgumentException.class,
                () -> policy.save(new BankDepositVerificationPolicyService.Update(true, LocalDate.of(2020, 1, 1))));
        verifyNoInteractions(jdbc);
        verify(settings, never()).upsertSetting(anyString(), anyString(), anyString());
    }
}
