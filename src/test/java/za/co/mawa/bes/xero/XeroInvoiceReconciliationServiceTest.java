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

    @Test void creditNoteMatchesOnlyUniqueReferenceCurrencyAndTotal() throws Exception {
        var json = mapper.readTree("""
            {"CreditNotes":[{"CreditNoteID":"cn-1","Reference":"CN-1","CurrencyCode":"ZAR","Total":95.50}]}
            """);
        assertEquals("cn-1", XeroInvoiceReconciliationService.uniqueCreditNoteMatch(json, "CN-1", "ZAR", 9550L));
        assertNull(XeroInvoiceReconciliationService.uniqueCreditNoteMatch(json, "CN-1", "ZAR", 9551L));
        assertNull(XeroInvoiceReconciliationService.uniqueCreditNoteMatch(json, "CN-1", "USD", 9550L));
        var duplicates = mapper.readTree("""
            {"CreditNotes":[{"CreditNoteID":"a","Reference":"CN-1","CurrencyCode":"ZAR","Total":95.50},
                            {"CreditNoteID":"b","Reference":"CN-1","CurrencyCode":"ZAR","Total":95.50}]}
            """);
        assertNull(XeroInvoiceReconciliationService.uniqueCreditNoteMatch(duplicates, "CN-1", "ZAR", 9550L));
    }

    @Test void allocationRequiresExactUniqueInvoiceAmountAndDate() throws Exception {
        var json = mapper.readTree("""
            {"CreditNotes":[{"CreditNoteID":"cn-1","Allocations":[
             {"Invoice":{"InvoiceID":"inv-1"},"Amount":95.50,"DateString":"2026-10-16T00:00:00"}]}]}
            """);
        assertTrue(XeroInvoiceReconciliationService.uniqueAllocationMatch(json, "cn-1", "inv-1", "2026-10-16", 9550L));
        assertFalse(XeroInvoiceReconciliationService.uniqueAllocationMatch(json, "cn-1", "inv-2", "2026-10-16", 9550L));
        assertFalse(XeroInvoiceReconciliationService.uniqueAllocationMatch(json, "cn-1", "inv-1", "2026-10-15", 9550L));
        var duplicates = mapper.readTree("""
            {"CreditNotes":[{"CreditNoteID":"cn-1","Allocations":[
             {"Invoice":{"InvoiceID":"inv-1"},"Amount":95.50,"DateString":"2026-10-16"},
             {"Invoice":{"InvoiceID":"inv-1"},"Amount":95.50,"DateString":"2026-10-16"}]}]}
            """);
        assertFalse(XeroInvoiceReconciliationService.uniqueAllocationMatch(duplicates, "cn-1", "inv-1", "2026-10-16", 9550L));
    }
}
