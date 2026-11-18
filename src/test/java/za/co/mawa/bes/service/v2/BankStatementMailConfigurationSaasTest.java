package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import za.co.mawa.bes.configuration.gcp.GcpTenantSecretService;
import za.co.mawa.bes.service.SettingService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BankStatementMailConfigurationSaasTest {
    private BankStatementMailConfigurationService.Configuration input(boolean enabled, String mailbox) {
        return new BankStatementMailConfigurationService.Configuration(enabled, "MICROSOFT_365_SAAS",
                mailbox, "bank@example.org", "statement", "", "", "", "", 14, "",
                "001234567", true, "DISCONNECTED", "", "", "", "04:00");
    }

    @Test void sharedMicrosoftAppDoesNotRequireCustomerClientCredentials() {
        SettingService settings = mock(SettingService.class);
        GcpTenantSecretService secrets = mock(GcpTenantSecretService.class);
        BankStatementMailConfigurationService service = new BankStatementMailConfigurationService(settings, secrets);
        assertDoesNotThrow(() -> service.save(input(false, "statements@example.org")));
        verify(settings, never()).upsertSetting(eq("CLIENT-SECRET-REF"), anyString(), anyString());
        verify(settings).upsertSetting("PROVIDER", "BANK-STATEMENT-MAIL", "MICROSOFT_365_SAAS");
    }

    @Test void cannotEnableUnverifiedMailbox() {
        SettingService settings = mock(SettingService.class);
        GcpTenantSecretService secrets = mock(GcpTenantSecretService.class);
        BankStatementMailConfigurationService service = new BankStatementMailConfigurationService(settings, secrets);
        assertThrows(IllegalArgumentException.class, () -> service.save(input(true, "statements@example.org")));
        verify(settings, never()).upsertSetting(eq("ENABLED"), anyString(), eq("true"));
    }

    @Test void changingMailboxInvalidatesPreviousVerification() {
        SettingService settings = mock(SettingService.class);
        when(settings.getSetting("MAILBOX", "BANK-STATEMENT-MAIL")).thenReturn("old@example.org");
        when(settings.getSetting("MS365-CONNECTION-STATUS", "BANK-STATEMENT-MAIL")).thenReturn("VERIFIED");
        when(settings.getSetting("MS365-VERIFIED-MAILBOX", "BANK-STATEMENT-MAIL")).thenReturn("old@example.org");
        BankStatementMailConfigurationService service = new BankStatementMailConfigurationService(settings, mock(GcpTenantSecretService.class));
        assertDoesNotThrow(() -> service.save(input(false, "new@example.org")));
        verify(settings).upsertSetting("MS365-VERIFIED-MAILBOX", "BANK-STATEMENT-MAIL", "");
        verify(settings).upsertSetting("MS365-CONNECTION-STATUS", "BANK-STATEMENT-MAIL", "MAILBOX_ACCESS_REQUIRED");
    }


    @Test void missingTenantScheduleDefaultsToFourAmCat() {
        SettingService settings = mock(SettingService.class);
        BankStatementMailConfigurationService service = new BankStatementMailConfigurationService(
                settings, mock(GcpTenantSecretService.class));
        assertEquals("04:00", service.get().dailyRunTime());
        when(settings.getSetting("DAILY-RUN-TIME", "BANK-STATEMENT-MAIL")).thenReturn("23:45");
        assertEquals("23:45", service.get().dailyRunTime());
    }

    @Test void legacySavePreservesAlreadyConfiguredTenantTime() {
        SettingService settings = mock(SettingService.class);
        when(settings.getSetting("DAILY-RUN-TIME", "BANK-STATEMENT-MAIL")).thenReturn("18:20");
        BankStatementMailConfigurationService service = new BankStatementMailConfigurationService(
                settings, mock(GcpTenantSecretService.class));
        var old = input(false, "statements@example.org");
        var payload = new BankStatementMailConfigurationService.Configuration(
                old.enabled(), old.provider(), old.mailbox(), old.sender(), old.subjectFilter(),
                old.tenantId(), old.clientId(), old.clientSecretRef(), old.refreshTokenRef(),
                old.lookbackDays(), old.lastSuccessDate(), old.accountNumbers(), old.requireDmarc(),
                old.connectionStatus(), old.connectedDirectoryId(), old.verifiedMailbox(),
                old.connectedAt(), null);
        service.save(payload);
        verify(settings).upsertSetting("DAILY-RUN-TIME", "BANK-STATEMENT-MAIL", "18:20");
    }

    @Test void persistsValidRunTimeAndRejectsMalformedBeforeMutatingSettings() {
        SettingService settings = mock(SettingService.class);
        BankStatementMailConfigurationService service = new BankStatementMailConfigurationService(
                settings, mock(GcpTenantSecretService.class));
        var valid = input(false, "statements@example.org");
        var custom = new BankStatementMailConfigurationService.Configuration(
                valid.enabled(), valid.provider(), valid.mailbox(), valid.sender(),
                valid.subjectFilter(), valid.tenantId(), valid.clientId(),
                valid.clientSecretRef(), valid.refreshTokenRef(), valid.lookbackDays(),
                valid.lastSuccessDate(), valid.accountNumbers(), valid.requireDmarc(),
                valid.connectionStatus(), valid.connectedDirectoryId(), valid.verifiedMailbox(),
                valid.connectedAt(), "00:05");
        service.save(custom);
        verify(settings).upsertSetting("DAILY-RUN-TIME", "BANK-STATEMENT-MAIL", "00:05");
        clearInvocations(settings);
        for (String invalid : new String[]{"", "24:00", "4:00", "12:60", "12:00:00"}) {
            var bad = new BankStatementMailConfigurationService.Configuration(
                    custom.enabled(), custom.provider(), custom.mailbox(), custom.sender(),
                    custom.subjectFilter(), custom.tenantId(), custom.clientId(),
                    custom.clientSecretRef(), custom.refreshTokenRef(), custom.lookbackDays(),
                    custom.lastSuccessDate(), custom.accountNumbers(), custom.requireDmarc(),
                    custom.connectionStatus(), custom.connectedDirectoryId(), custom.verifiedMailbox(),
                    custom.connectedAt(), invalid);
            assertThrows(IllegalArgumentException.class, () -> service.save(bad), invalid);
        }
        verify(settings, never()).upsertSetting(anyString(), anyString(), anyString());
    }
}
