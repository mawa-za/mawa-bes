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
                "001234567", true, "DISCONNECTED", "", "", "");
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
}
