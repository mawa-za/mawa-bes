package za.co.mawa.bes.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.gcp.GcpTenantSecretService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OutgoingEmailConfigurationServiceTest {
    private final SettingService settings = mock(SettingService.class);
    private final GcpTenantSecretService secrets = mock(GcpTenantSecretService.class);
    private final MockEnvironment environment = new MockEnvironment()
            .withProperty("MAWA_OUTGOING_SECRET_NAMESPACE", "hotfix");
    private final OutgoingEmailConfigurationService service =
            new OutgoingEmailConfigurationService(settings, secrets, environment);

    @AfterEach void clear() { TenantContext.clear(); }

    @Test void sharedProviderWorksForExistingTenantsWithoutCredentials() {
        TenantContext.setCurrentTenant("society");
        var config = service.get();
        assertTrue(config.enabled());
        assertEquals(OutgoingEmailConfigurationService.SHARED, config.provider());
        assertFalse(config.clientSecretConfigured());
        verifyNoInteractions(secrets);
    }

    @Test void tenantGraphSecretIsStoredInSecretManagerNotDatabase() {
        TenantContext.setCurrentTenant("phanka_society");
        var update = new OutgoingEmailConfigurationService.Update(true,
                OutgoingEmailConfigurationService.TENANT_GRAPH,
                "Phanka", "replies@phanka.co.za", "mailer@phanka.co.za",
                "12345678-1234-1234-1234-123456789012",
                "abcdefab-abcd-abcd-abcd-abcdefabcdef", "not-in-database");
        service.save(update);
        verify(secrets).createOrAddSecretVersion("mawa-outgoing-hotfix-phanka_society", "not-in-database");
        verify(settings, never()).upsertSetting(eq("CLIENT-SECRET"), anyString(), anyString());
        verify(settings).upsertSetting("CLIENT-SECRET-REF", "OUTGOING-EMAIL",
                "mawa-outgoing-hotfix-phanka_society");
    }

    @Test void rejectsInvalidSenderAndTenantBeforeWritingAnySetting() {
        TenantContext.setCurrentTenant("society");
        var update = new OutgoingEmailConfigurationService.Update(true,
                OutgoingEmailConfigurationService.TENANT_GRAPH, "\rBad", "", "mailer@phanka.co.za",
                "12345678-1234-1234-1234-123456789012",
                "abcdefab-abcd-abcd-abcd-abcdefabcdef", "secret");
        assertThrows(IllegalArgumentException.class, () -> service.save(update));
        verifyNoInteractions(settings, secrets);
        TenantContext.clear();
        assertThrows(IllegalStateException.class, service::get);
    }

    @Test void cannotActivateGraphWithoutStoredSecret() {
        TenantContext.setCurrentTenant("society");
        var update = new OutgoingEmailConfigurationService.Update(true,
                OutgoingEmailConfigurationService.TENANT_GRAPH, "", "", "mailer@phanka.co.za",
                "12345678-1234-1234-1234-123456789012",
                "abcdefab-abcd-abcd-abcd-abcdefabcdef", "");
        assertThrows(IllegalArgumentException.class, () -> service.save(update));
        verify(settings, never()).upsertSetting(anyString(), anyString(), anyString());
    }
}
