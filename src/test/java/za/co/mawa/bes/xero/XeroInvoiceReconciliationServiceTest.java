package za.co.mawa.bes.xero;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class XeroInvoiceReconciliationServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void matchesOnlyExactNumberCurrencyAndTotal() throws Exception {
        var json = mapper.readTree(""" 
            {"Invoices":[{"InvoiceID":"id-1","InvoiceNumber":"INV-1","CurrencyCode":"ZAR","Total":125.00}]}
            """);
        assertEquals("id-1", XeroInvoiceReconciliationService.uniqueInvoiceMatch(json, "INV-1", "ZAR", 12500L));
        assertNull(XeroInvoiceReconciliationService.uniqueInvoiceMatch(json, "INV-2", "ZAR", 12500L));
        assertNull(XeroInvoiceReconciliationService.uniqueInvoiceMatch(json, "INV-1", "USD", 12500L));
        assertNull(XeroInvoiceReconciliationService.uniqueInvoiceMatch(json, "INV-1", "ZAR", 12501L));
    }

    @Test void conflictingMatchesAreNotLinked() throws Exception {
        var json = mapper.readTree("""
            {"Invoices":[{"InvoiceID":"a","InvoiceNumber":"INV-1","CurrencyCode":"ZAR","Total":125},
                         {"InvoiceID":"b","InvoiceNumber":"INV-1","CurrencyCode":"ZAR","Total":125}]}
            """);
        assertNull(XeroInvoiceReconciliationService.uniqueInvoiceMatch(json, "INV-1", "ZAR", 12500L));
    }
    @Test void paymentReconciliationRequiresUniqueExactMatch() throws Exception {
        var json = mapper.readTree("""
            {"Invoices":[{"InvoiceID":"invoice-1","Payments":[
              {"PaymentID":"pay-1","Amount":190.00,"Reference":"RECEIPT-10","DateString":"2026-10-16T00:00:00"}
            ]}]}
            """);
        assertEquals("pay-1", XeroInvoiceReconciliationService.uniquePaymentMatch(
                json, "invoice-1", "RECEIPT-10", "2026-10-16", 19000L));
        assertNull(XeroInvoiceReconciliationService.uniquePaymentMatch(
                json, "invoice-1", "RECEIPT-11", "2026-10-16", 19000L));
        assertNull(XeroInvoiceReconciliationService.uniquePaymentMatch(
                json, "invoice-1", "RECEIPT-10", "2026-10-15", 19000L));
        assertNull(XeroInvoiceReconciliationService.uniquePaymentMatch(
                json, "invoice-1", "RECEIPT-10", "2026-10-16", 18000L));
    }

    @Test void duplicatePaymentMatchesRemainQuarantined() throws Exception {
        var json = mapper.readTree("""
            {"Invoices":[{"InvoiceID":"invoice-1","Payments":[
              {"PaymentID":"pay-1","Amount":190,"Reference":"R1","DateString":"2026-10-16T00:00:00"},
              {"PaymentID":"pay-2","Amount":190,"Reference":"R1","DateString":"2026-10-16T00:00:00"}
            ]}]}
            """);
        assertNull(XeroInvoiceReconciliationService.uniquePaymentMatch(
                json, "invoice-1", "R1", "2026-10-16", 19000L));
    }
}
