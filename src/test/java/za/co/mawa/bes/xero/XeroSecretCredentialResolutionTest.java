package za.co.mawa.bes.xero;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.mawa.bes.configuration.gcp.GcpTenantSecretService;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.service.TenantAdminService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class XeroSecretCredentialResolutionTest {
    private XeroAuthService auth;
    private GcpTenantSecretService secrets;

    @BeforeEach
    void setup() {
        auth = new XeroAuthService();
        secrets = mock(GcpTenantSecretService.class);
        ReflectionTestUtils.setField(auth, "gcpTenantSecretService", secrets);
        ReflectionTestUtils.setField(auth, "settingService", mock(SettingService.class));
    }

    @Test
    void selectedOrganisationPrefersSecretOverLegacyTenantProperty() {
        TenantAdminService admin = mock(TenantAdminService.class);
        ReflectionTestUtils.setField(auth, "tenantAdminService", admin);
        when(admin.getTenantProperty("tenant-a"))
                .thenReturn("{\"XERO-TENANT-ID\":\"old-organisation-id\"}");
        String secret = "mawa-hotfix-tenant-xero-tenant-id";
        when(secrets.findSettingSecretReference("TENANT-ID", "XERO")).thenReturn(secret);
        when(secrets.accessSecretReference(secret)).thenReturn("correct-organisation-id");

        assertEquals("correct-organisation-id", auth.getXeroProperty("tenant-a", XeroUtils.XERO_TENANT_ID));
        verify(secrets, never()).resolveTenantProperty(any(), eq(XeroUtils.XERO_TENANT_ID));
    }

    @Test
    void emptyConfiguredOrganisationSecretFailsClosed() {
        String secret = "mawa-hotfix-tenant-xero-tenant-id";
        when(secrets.findSettingSecretReference("TENANT-ID", "XERO")).thenReturn(secret);
        when(secrets.accessSecretReference(secret)).thenReturn(" ");

        assertThrows(IllegalStateException.class,
                () -> auth.getXeroProperty("", XeroUtils.XERO_TENANT_ID));
    }

    @Test
    void clientIdIgnoresStalePlaintextTenantProperty() {
        String reference = "mawa-hotfix-tenant-xero-client-id";
        when(secrets.findSettingSecretReference("CLIENT-ID", "XERO")).thenReturn(reference);
        when(secrets.accessSecretReference(reference)).thenReturn("correct-client-id");

        assertEquals("correct-client-id", auth.getXeroProperty("", XeroUtils.XERO_CLIENT_ID));
        verify(secrets, never()).resolveTenantProperty(any(), anyString());
        verify(secrets, never()).resolveSetting(anyString(), anyString());
    }

    @Test
    void missingClientSecretReferenceFailsClosed() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> auth.getXeroProperty("", XeroUtils.XERO_CLIENT_SECRET));
        assertTrue(error.getMessage().contains("Missing GCP Secret Manager reference"));
        verify(secrets, never()).resolveSetting(anyString(), anyString());
    }

    @Test
    void emptySecretVersionFailsClosed() {
        String reference = "mawa-hotfix-tenant-xero-secret-key";
        when(secrets.findSettingSecretReference("SECRET-KEY", "XERO")).thenReturn(reference);
        when(secrets.accessSecretReference(reference)).thenReturn(" ");
        assertThrows(IllegalStateException.class,
                () -> auth.getXeroProperty("", XeroUtils.XERO_CLIENT_SECRET));
    }
}
