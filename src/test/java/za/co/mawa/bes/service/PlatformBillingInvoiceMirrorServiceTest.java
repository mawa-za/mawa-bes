package za.co.mawa.bes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.entity.PlatformBillingInvoiceMirrorEntity;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.PlatformBillingInvoiceMirrorRepository;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlatformBillingInvoiceMirrorServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private PlatformBillingInvoiceMirrorRepository repository;
    private PlatformBillingInvoiceMirrorService service;

    @BeforeEach
    void prepare() {
        repository = mock(PlatformBillingInvoiceMirrorRepository.class);
        TenantService tenants = mock(TenantService.class);
        Properties properties = new Properties();
        properties.setProperty("TENANT.TYPE", "PLATFORM_OPERATOR");
        when(tenants.getTenantProperties("operator")).thenReturn(properties);
        service = new PlatformBillingInvoiceMirrorService(
                repository, tenants, mock(UserAccessService.class), mapper,
                mock(PartnerRepository.class), mock(InvoiceService.class));
        ReflectionTestUtils.setField(service, "configuredOperatorTenantId", "operator");
        TenantContext.setCurrentTenant("operator");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private JsonNode invoice(String status, String amount) throws Exception {
        return mapper.readTree("""
                {"id":19,"tenantId":"customer-001","invoiceNumber":"SAAS-001",
                 "status":"%s","currency":"ZAR","subtotal":%s,"taxAmount":0,"totalAmount":%s,
                 "lines":[{"description":"SaaS plan","quantity":1,"netAmount":%s,
                           "taxAmount":0,"totalAmount":%s}]}
                """.formatted(status, amount, amount, amount, amount));
    }

    @Test
    void invalidLineTotalsAreRejectedBeforePersistence() throws Exception {
        JsonNode data = invoice("ISSUED", "100.00");
        ((com.fasterxml.jackson.databind.node.ObjectNode) data.path("lines").get(0)).put("netAmount", 99.0);
        assertThrows(IllegalArgumentException.class, () -> service.accept(data));
        verify(repository, never()).save(any());
    }

    @Test
    void importingTheSameSourceInvoiceTwiceIsIdempotent() throws Exception {
        PlatformBillingInvoiceMirrorEntity existing = new PlatformBillingInvoiceMirrorEntity();
        JsonNode payload = invoice("ISSUED", "100.00");
        existing.setSourceInvoiceId(19L);
        existing.setSourceTenantId("customer-001");
        existing.setSourceInvoiceNumber("SAAS-001");
        existing.setSnapshotJson(payload.toString());
        existing.setAccountingState("ERP_INVOICE_POSTED");
        when(repository.findById(19L)).thenReturn(Optional.of(existing));
        Map<String, Object> result = service.accept(payload);
        assertEquals(Boolean.FALSE, result.get("changed"));
        assertEquals("ERP_INVOICE_POSTED", result.get("state"));
        verify(repository, never()).save(any());
    }

    @Test
    void paidInvoiceRequiresPaymentReconciliationNotSyntheticPayment() throws Exception {
        when(repository.findById(19L)).thenReturn(Optional.empty());
        service.accept(invoice("PAID", "100.00"));
        verify(repository).save(argThat(row -> "PAYMENT_RECONCILIATION_REQUIRED".equals(row.getAccountingState())
                && row.getErpInvoiceId() == null));
    }

    @Test
    void nonOperatorTenantCannotImportBillingRevenue() throws Exception {
        TenantContext.setCurrentTenant("other-tenant");
        assertThrows(SecurityException.class, () -> service.accept(invoice("ISSUED", "100.00")));
        verify(repository, never()).save(any());
    }
}
