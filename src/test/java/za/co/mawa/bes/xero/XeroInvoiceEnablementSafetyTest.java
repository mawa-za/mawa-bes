package za.co.mawa.bes.xero;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.service.TenantAdminService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class XeroInvoiceEnablementSafetyTest {
    @Test
    void explicitOffWinsOverLegacyEnabledTenantProperty() {
        XeroIntegrationSettingsService service = new XeroIntegrationSettingsService();
        SettingService settingService = mock(SettingService.class);
        TenantAdminService tenantAdminService = mock(TenantAdminService.class);
        ReflectionTestUtils.setField(service, "settingService", settingService);
        ReflectionTestUtils.setField(service, "tenantAdminService", tenantAdminService);
        when(settingService.getSetting("INVOICE-INTEGRATION-ENABLED", "XERO")).thenReturn("false");
        when(settingService.getSetting("INTEGRATION", "XERO")).thenReturn("false");
        assertFalse(service.isInvoiceIntegrationEnabled());
        assertFalse(service.isIntegrationEnabled());
    }

    @Test
    void missingSettingsDefaultToDisabled() {
        XeroIntegrationSettingsService service = new XeroIntegrationSettingsService();
        ReflectionTestUtils.setField(service, "settingService", mock(SettingService.class));
        ReflectionTestUtils.setField(service, "tenantAdminService", mock(TenantAdminService.class));
        assertFalse(service.isInvoiceIntegrationEnabled());
        assertFalse(service.isIntegrationEnabled());
    }
}
