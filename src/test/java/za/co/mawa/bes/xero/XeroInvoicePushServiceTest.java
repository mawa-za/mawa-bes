package za.co.mawa.bes.xero;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.repository.InvoicePaymentRepository;
import za.co.mawa.bes.repository.InvoiceRepository;
import za.co.mawa.bes.repository.PartnerContactRepository;
import za.co.mawa.bes.repository.PartnerIdentityRepository;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.ProductRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XeroInvoicePushServiceTest {

    @InjectMocks private XeroInvoicePushService service;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoicePaymentRepository invoicePaymentRepository;
    @Mock private PartnerRepository partnerRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PartnerIdentityRepository partnerIdentityRepository;
    @Mock private PartnerContactRepository partnerContactRepository;
    @Mock private XeroAuthService xeroAuthService;
    @Mock private XeroIntegrationSettingsService settings;
    @Mock private XeroMasterDataPushService masterDataPushService;

    @Test
    void quarantinedInvoiceCannotBeResentBeforeReconciliation() {
        InvoiceEntity invoice = InvoiceEntity.builder()
                .id("invoice-quarantine")
                .integrationStatus("RECONCILIATION_REQUIRED")
                .lines(new ArrayList<>())
                .build();
        when(invoiceRepository.findByIdWithLines("invoice-quarantine"))
                .thenReturn(Optional.of(invoice));

        assertThrows(XeroAmbiguousResponseException.class,
                () -> service.pushInvoice("invoice-quarantine"));
        verifyNoInteractions(xeroAuthService);
    }

    @Test
    void sendsMawaInvoiceNumberAndStoredXeroIdOnUpdate() {
        when(partnerRepository.findById("partner-1")).thenReturn(Optional.empty());
        when(partnerIdentityRepository.findPartnerIdentityByPartner("partner-1")).thenReturn(new ArrayList<>());
        when(partnerContactRepository.findContactsByPartner("partner-1")).thenReturn(new ArrayList<>());
        when(settings.invoiceAccountCode()).thenReturn("200");
        when(settings.invoiceTaxType()).thenReturn("NONE");
        when(settings.lineAmountTypes()).thenReturn("Exclusive");
        InvoiceEntity invoice = InvoiceEntity.builder()
                .id("invoice-1")
                .invoiceNo("INV-000123")
                .xeroInvoiceId("xero-invoice-id")
                .partnerId("partner-1")
                .invoiceDate(LocalDate.of(2026, 9, 13))
                .status("ISSUED")
                .totalCents(12500L)
                .currency("ZAR")
                .lines(new ArrayList<>())
                .build();

        JsonNode payload = service.buildInvoicePayload(invoice).path("Invoices").get(0);

        assertEquals("INV-000123", payload.path("InvoiceNumber").asText());
        assertEquals("xero-invoice-id", payload.path("InvoiceID").asText());
        assertEquals("AUTHORISED", payload.path("Status").asText());
    }

    @Test
    void mapsMawaLifecycleWithoutSendingPaidAsInvoiceStatus() {
        assertEquals("DRAFT", service.mapInvoiceStatus("AWAITING_APPROVAL"));
        assertEquals("AUTHORISED", service.mapInvoiceStatus("ISSUED"));
        assertEquals("AUTHORISED", service.mapInvoiceStatus("PARTIALLY_PAID"));
        assertEquals("AUTHORISED", service.mapInvoiceStatus("PAID"));
        assertEquals("VOIDED", service.mapInvoiceStatus("CANCELLED"));
    }
    @Test
    void paymentRetryKeyIsStableAndScopedToTenantAndOperation() {
        String first = XeroInvoicePushService.operationIdempotencyKey("xero-tenant-a", "payment-create:payment-1");
        assertEquals(first, XeroInvoicePushService.operationIdempotencyKey("xero-tenant-a", "payment-create:payment-1"));
        assertNotEquals(first, XeroInvoicePushService.operationIdempotencyKey("xero-tenant-b", "payment-create:payment-1"));
        assertNotEquals(first, XeroInvoicePushService.operationIdempotencyKey("xero-tenant-a", "payment-create:payment-2"));
        assertNotEquals(first, XeroInvoicePushService.operationIdempotencyKey("xero-tenant-a", "credit-note-create:payment-1"));
    }

    @Test
    void creditNoteCreationAndAllocationHaveIndependentRetryKeys() {
        assertNotEquals(
                XeroInvoicePushService.operationIdempotencyKey("tenant", "credit-note-create:note-1"),
                XeroInvoicePushService.operationIdempotencyKey("tenant", "credit-note-allocate:note-1"));
    }
}
