package za.co.mawa.bes.xero;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.service.TenantAdminService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class XeroAccountingConfigurationServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private XeroAccountingConfigurationService.Options options() throws Exception {
        return XeroAccountingConfigurationService.parseOptions("org-a", mapper.readTree("""
                [
                  {"AccountID":"sales-a","Code":"410","Name":"Service revenue","Type":"REVENUE","Status":"ACTIVE"},
                  {"AccountID":"bad-200","Code":"200","Name":"Archived sales","Type":"SALES","Status":"ARCHIVED"},
                  {"AccountID":"expense-a","Code":"500","Name":"Expenses","Type":"EXPENSE","Status":"ACTIVE"},
                  {"AccountID":"bank-a","Code":"1133640","Name":"Bank","Type":"BANK","Status":"ACTIVE"},
                  {"AccountID":"clear-a","Code":"120","Name":"Clearing","Type":"CURRENT","Status":"ACTIVE","EnablePaymentsToAccount":true},
                  {"AccountID":"old-bank","Code":"90","Name":"Old bank","Type":"BANK","Status":"ARCHIVED"}
                ]
                """), mapper.readTree("""
                [
                  {"TaxType":"OUTPUT","Name":"Sales VAT","Status":"ACTIVE","CanApplyToRevenue":true,"DisplayTaxRate":15},
                  {"TaxType":"NONE","Name":"No tax","Status":"ACTIVE","CanApplyToRevenue":true,"DisplayTaxRate":0},
                  {"TaxType":"INPUT","Name":"Purchases VAT","Status":"ACTIVE","CanApplyToRevenue":false},
                  {"TaxType":"OLD","Name":"Archived VAT","Status":"DELETED","CanApplyToRevenue":true}
                ]
                """));
    }

    @Test void filtersRealRevenuePaymentAndTaxOptions() throws Exception {
        var options = options();
        assertEquals(java.util.List.of("sales-a"), options.invoiceAccounts().stream().map(XeroAccountingConfigurationService.AccountOption::accountId).toList());
        assertEquals(java.util.Set.of("bank-a", "clear-a"), options.paymentAccounts().stream().map(XeroAccountingConfigurationService.AccountOption::accountId).collect(java.util.stream.Collectors.toSet()));
        assertEquals(java.util.Set.of("OUTPUT", "NONE"), options.taxRates().stream().map(XeroAccountingConfigurationService.TaxOption::taxType).collect(java.util.stream.Collectors.toSet()));
    }

    @Test void rejectsInvalidAccountsTaxesAndOtherOrganisation() throws Exception {
        var options = options();
        assertThrows(IllegalArgumentException.class, () -> XeroAccountingConfigurationService.validateSelection(options,
                new XeroAccountingConfigurationService.SaveRequest("org-a", "bad-200", "bank-a", "OUTPUT")));
        assertThrows(IllegalArgumentException.class, () -> XeroAccountingConfigurationService.validateSelection(options,
                new XeroAccountingConfigurationService.SaveRequest("org-a", "sales-a", "expense-a", "OUTPUT")));
        assertThrows(IllegalArgumentException.class, () -> XeroAccountingConfigurationService.validateSelection(options,
                new XeroAccountingConfigurationService.SaveRequest("org-a", "sales-a", "bank-a", "INPUT")));
        assertThrows(IllegalArgumentException.class, () -> XeroAccountingConfigurationService.validateSelection(options,
                new XeroAccountingConfigurationService.SaveRequest("org-b", "sales-a", "bank-a", "OUTPUT")));
    }

    @Test void persistsAccountIdsAndCodesAndDetectsChangedCode() throws Exception {
        var options = options();
        var mapping = XeroAccountingConfigurationService.validateSelection(options,
                new XeroAccountingConfigurationService.SaveRequest("org-a", "sales-a", "bank-a", "OUTPUT"));
        assertEquals("410", mapping.invoiceAccountCode());
        assertEquals("1133640", mapping.paymentAccountCode());
        assertEquals(mapping, XeroAccountingMapping.parse(mapping.serialise()));
        assertTrue(XeroAccountingConfigurationService.mappingValid(options, mapping));
        assertFalse(XeroAccountingConfigurationService.mappingValid(options,
                new XeroAccountingMapping("org-a", "sales-a", "200", "bank-a", "1133640", "OUTPUT")));
        assertFalse(mapping.matchesOrganisation("org-b"));
    }

    @Test void noGuessedAccountOrTaxDefaultAndValidatedMappingWins() {
        var store = mock(SettingService.class);
        var service = new XeroIntegrationSettingsService();
        ReflectionTestUtils.setField(service, "settingService", store);
        ReflectionTestUtils.setField(service, "tenantAdminService", mock(TenantAdminService.class));
        assertNull(service.invoiceAccountCode());
        assertNull(service.invoiceTaxType());
        assertThrows(IllegalStateException.class, () -> service.assertPostingConfiguration("org-a"));
        var mapping = new XeroAccountingMapping("org-a", "sales-a", "410", "bank-a", "1133640", "OUTPUT");
        when(store.getSetting("ACCOUNTING-MAPPING", "XERO")).thenReturn(mapping.serialise());
        assertEquals("410", service.invoiceAccountCode());
        assertEquals("OUTPUT", service.invoiceTaxType());
        assertEquals("1133640", service.paymentAccountCode());
        assertDoesNotThrow(() -> service.assertPostingConfiguration("org-a"));
        assertThrows(IllegalStateException.class, () -> service.assertPostingConfiguration("org-b"));
    }
}
