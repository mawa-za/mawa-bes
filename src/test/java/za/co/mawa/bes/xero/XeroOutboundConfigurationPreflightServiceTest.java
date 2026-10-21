package za.co.mawa.bes.xero;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.v2.integration.XeroActivationResponseDto;
import za.co.mawa.bes.service.v2.integration.XeroActivationService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class XeroOutboundConfigurationPreflightServiceTest {
    @Mock JdbcTemplate jdbc;
    @Mock XeroIntegrationSettingsService settings;
    @Mock XeroActivationService activation;

    @Test
    void refusesUnauthenticatedCallerBeforeAccessingConfiguration() {
        try (MockedStatic<UserContext> user = mockStatic(UserContext.class)) {
            user.when(UserContext::getCurrentUserId).thenReturn(null);
            var service = new XeroOutboundConfigurationPreflightService(jdbc, settings, activation);
            assertThrows(AccessDeniedException.class, service::assess);
            verifyNoInteractions(jdbc, settings, activation);
        }
    }

    @Test
    void keepsReleaseBlockedAndNeverUpdatesAnyRecords() {
        try (MockedStatic<UserContext> user = mockStatic(UserContext.class)) {
            user.when(UserContext::getCurrentUserId).thenReturn("user-1");
            when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("user-1"))).thenReturn(1);
            when(activation.secretNames()).thenReturn(XeroActivationResponseDto.builder()
                    .selectedTenantId("xero-tenant-1")
                    .invoiceIntegrationEnabled(false)
                    .invoiceIntegrationRequested(false)
                    .integrationStatus("AUTHORISED").build());
            when(settings.invoiceAccountCode()).thenReturn("200");
            when(settings.invoiceTaxType()).thenReturn("NONE");
            when(settings.lineAmountTypes()).thenReturn("Exclusive");
            when(settings.paymentAccountCode()).thenReturn("123");
            Map<String, Object> result = new XeroOutboundConfigurationPreflightService(jdbc, settings, activation).assess();
            assertEquals("LOCAL_CONFIGURATION_READ_ONLY", result.get("mode"));
            assertEquals(false, result.get("releaseAllowed"));
            assertEquals(false, result.get("externalLedgerVerified"));
            verify(jdbc, never()).update(anyString(), any(Object[].class));
        }
    }
}
