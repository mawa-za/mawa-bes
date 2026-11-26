package za.co.mawa.bes.xero;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.gcp.GcpTenantSecretService;
import za.co.mawa.bes.configuration.gcp.TenantSecretNameService;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.service.v2.integration.XeroActivationService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class XeroActivationStatusTest {
    @Test void pendingOrganisationSelectionReturnsAuthorisedConnections() throws Exception {
        var settings = mock(SettingService.class);
        var auth = mock(XeroAuthService.class);
        when(settings.getSetting("INTEGRATION-STATUS", "XERO"))
                .thenReturn("PENDING_ORGANISATION_SELECTION");
        var organisation = za.co.mawa.bes.dto.v2.integration.XeroConnectionDto.builder()
                .tenantId("xero-demo-organisation").tenantName("Demo Org").build();
        when(auth.getConnectionsForCurrentTenant()).thenReturn(java.util.List.of(organisation));
        var activation = new XeroActivationService(mock(GcpTenantSecretService.class),
                mock(TenantSecretNameService.class), settings, mock(Environment.class), auth,
                mock(XeroMasterDataQueueService.class), mock(XeroOAuthStateService.class), mock(XeroAccountingConfigurationService.class));
        assertEquals("xero-demo-organisation", activation.connections().get(0).getTenantId());
    }

    @Test void pendingAuthorisationIsReadableWithoutPopulatedOrganisationSecret() {
        var settings = mock(SettingService.class);
        var secrets = mock(TenantSecretNameService.class);
        var auth = mock(XeroAuthService.class);
        when(settings.getSetting("INTEGRATION-STATUS", "XERO")).thenReturn("PENDING_AUTHORISATION");
        when(secrets.currentTenantSecretName(anyString(), anyString())).thenReturn("secret-placeholder");
        var activation = new XeroActivationService(mock(GcpTenantSecretService.class),
                secrets, settings, mock(Environment.class), auth,
                mock(XeroMasterDataQueueService.class), mock(XeroOAuthStateService.class), mock(XeroAccountingConfigurationService.class));
        TenantContext.setCurrentTenant("tenant-a");
        try {
            var status = activation.secretNames();
            assertEquals("PENDING_AUTHORISATION", status.getIntegrationStatus());
            assertNull(status.getSelectedTenantId());
            verify(auth, never()).getXeroProperty(anyString(), anyString());
        } finally {
            TenantContext.clear();
        }
    }
    @Test void enablementRequiresLiveValidatedMapping() {
        var store = mock(SettingService.class);
        var config = mock(XeroAccountingConfigurationService.class);
        when(store.getSetting("INTEGRATION-STATUS", "XERO")).thenReturn("AUTHORISED");
        doThrow(new IllegalStateException("Mapping invalid")).when(config).validateForEnablement();
        var activation = new XeroActivationService(mock(GcpTenantSecretService.class),
                mock(TenantSecretNameService.class), store, mock(Environment.class), mock(XeroAuthService.class),
                mock(XeroMasterDataQueueService.class), mock(XeroOAuthStateService.class), config);
        var request = new za.co.mawa.bes.dto.v2.integration.XeroInvoiceIntegrationRequestDto();
        request.setEnabled(true);
        assertThrows(IllegalStateException.class, () -> activation.updateInvoiceIntegration(request));
        verify(store, never()).upsertSetting(eq("INVOICE-INTEGRATION-ENABLED"), eq("XERO"), eq("true"));
    }
}
