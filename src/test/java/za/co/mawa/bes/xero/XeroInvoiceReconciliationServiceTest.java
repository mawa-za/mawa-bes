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
}
