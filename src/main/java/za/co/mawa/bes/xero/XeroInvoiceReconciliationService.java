package za.co.mawa.bes.xero;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoicePaymentEntity;
import za.co.mawa.bes.repository.InvoiceRepository;
import za.co.mawa.bes.repository.InvoicePaymentRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.sql.Date;
import java.util.Map;

/** Conservative, explicitly invoked reconciliation; never triggers a Xero write. */
@Service
public class XeroInvoiceReconciliationService {
    private static final String DEFAULT_URL = "https://api.xero.com/api.xro/2.0/Invoices";
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private InvoicePaymentRepository paymentRepository;
    @Autowired private XeroAuthService auth;
    @Autowired private JdbcTemplate jdbcTemplate;
    private static final String CREDIT_NOTES_URL = "https://api.xero.com/api.xro/2.0/CreditNotes";

    /** Returns true only when a single exact financial match can be safely linked. */
    public boolean reconcileInvoice(String invoiceId) throws IOException {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        if (!"RECONCILIATION_REQUIRED".equalsIgnoreCase(invoice.getIntegrationStatus())) {
            throw new IllegalStateException("Invoice is not quarantined for Xero reconciliation");
        }
        if (invoice.getXeroInvoiceId() != null && !invoice.getXeroInvoiceId().isBlank()) {
            // Existing external ID means payment/credit-note outcomes may be uncertain.
            // Reconciliation must not clear the parent quarantine here.
            return false;
        }
        if (invoice.getInvoiceNo() == null || invoice.getInvoiceNo().isBlank()) return false;

        String configTenant = auth.checkXeroInfo();
        if (configTenant == null || configTenant.isBlank()) throw new IOException("Xero is not connected");
        String tenantId = auth.getXeroProperty(configTenant, XeroUtils.XERO_TENANT_ID);
        if (tenantId == null || tenantId.isBlank()) throw new IOException("Xero organisation is not selected");
        String token = auth.refreshAccessToken(configTenant);
        String baseUrl = auth.getXeroProperty(configTenant, "XERO-BASE-URL", DEFAULT_URL);
        String separator = baseUrl.contains("?") ? "&" : "?";
        String url = baseUrl + separator + "InvoiceNumbers=" +
                URLEncoder.encode(invoice.getInvoiceNo(), StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Xero-Tenant-Id", tenantId);
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) throw new IOException("Xero reconciliation lookup failed: HTTP " + code);
            JsonNode body;
            try (var stream = conn.getInputStream()) { body = mapper.readTree(stream); }
            String matchedId = uniqueInvoiceMatch(body, invoice.getInvoiceNo(), invoice.getCurrency(), invoice.getTotalCents());
            if (matchedId == null) return false;
            // Recheck quarantine before persisting; never override a later operator action.
            InvoiceEntity current = invoiceRepository.findById(invoiceId).orElseThrow();
            if (!"RECONCILIATION_REQUIRED".equalsIgnoreCase(current.getIntegrationStatus()) ||
                    (current.getXeroInvoiceId() != null && !current.getXeroInvoiceId().isBlank())) return false;
            current.setXeroInvoiceId(matchedId);
            // Leave quarantine active: related payment/credit-note outcomes must be reconciled separately.
            current.setIntegrationError("Invoice matched in Xero (" + matchedId + "); verify payments and credit notes before release");
            current.setIntegrationLastAttemptAt(LocalDateTime.now());
            invoiceRepository.save(current);
            return true;
        } finally { conn.disconnect(); }
    }

    /** Read-only recovery: link a quarantined payment only on a single exact match. */
    public boolean reconcilePayment(String paymentId) throws IOException {
        InvoicePaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        if (!"RECONCILIATION_REQUIRED".equalsIgnoreCase(payment.getXeroSyncStatus())) {
            throw new IllegalStateException("Payment is not quarantined for Xero reconciliation");
        }
        if (payment.getXeroPaymentId() != null && !payment.getXeroPaymentId().isBlank()) return false;
        InvoiceEntity invoice = payment.getInvoice();
        if (invoice == null || invoice.getXeroInvoiceId() == null || invoice.getXeroInvoiceId().isBlank()) return false;
        // Reference is mandatory for safe matching. Amount alone cannot distinguish equal payments.
        if (payment.getReferenceNo() == null || payment.getReferenceNo().isBlank()
                || payment.getPaymentDate() == null || payment.getAmountCents() == null) return false;

        String configTenant = auth.checkXeroInfo();
        if (configTenant == null || configTenant.isBlank()) throw new IOException("Xero is not connected");
        String tenantId = auth.getXeroProperty(configTenant, XeroUtils.XERO_TENANT_ID);
        if (tenantId == null || tenantId.isBlank()) throw new IOException("Xero organisation is not selected");
        String token = auth.refreshAccessToken(configTenant);
        String base = auth.getXeroProperty(configTenant, "XERO-BASE-URL", DEFAULT_URL);
        String url = base.replaceAll("/+$", "") + "/" +
                URLEncoder.encode(invoice.getXeroInvoiceId(), StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Xero-Tenant-Id", tenantId);
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) throw new IOException("Xero payment lookup failed: HTTP " + code);
            JsonNode body;
            try (var stream = conn.getInputStream()) { body = mapper.readTree(stream); }
            String matchedId = uniquePaymentMatch(body, invoice.getXeroInvoiceId(),
                    payment.getReferenceNo(), payment.getPaymentDate().toLocalDate().toString(), payment.getAmountCents());
            if (matchedId == null) return false;
            InvoicePaymentEntity current = paymentRepository.findById(paymentId).orElseThrow();
            if (!"RECONCILIATION_REQUIRED".equalsIgnoreCase(current.getXeroSyncStatus())
                    || (current.getXeroPaymentId() != null && !current.getXeroPaymentId().isBlank())) return false;
            current.setXeroPaymentId(matchedId);
            current.setXeroSyncError("Matched Xero payment " + matchedId + "; verify before releasing quarantine");
            paymentRepository.save(current);
            return true;
        } finally { conn.disconnect(); }
    }


    /** Read-only credit note lookup; never releases RECONCILIATION_REQUIRED. */
    public boolean reconcileCreditNote(String creditNoteId) throws IOException {
        Map<String, Object> note = loadQuarantinedCreditNote(creditNoteId);
        String existing = stringValue(note.get("xero_credit_note_id"));
        if (existing != null) return false;
        String number = stringValue(note.get("credit_note_no"));
        String currency = stringValue(note.get("currency"));
        Long cents = note.get("total_cents") instanceof Number n ? n.longValue() : null;
        if (number == null || cents == null) return false;
        String url = creditNotesBaseUrl() + "?where=" + URLEncoder.encode(
                "Reference==\"" + number.replace("\"", "\\\"") + "\"", StandardCharsets.UTF_8);
        JsonNode response = getXero(url);
        String found = uniqueCreditNoteMatch(response, number, currency, cents);
        if (found == null) return false;
        return jdbcTemplate.update("""
                UPDATE credit_note SET xero_credit_note_id=?,
                    xero_sync_error='Credit note found in Xero; reconcile allocation before release'
                WHERE id=? AND xero_sync_status='RECONCILIATION_REQUIRED'
                    AND (xero_credit_note_id IS NULL OR xero_credit_note_id='')
                """, found, creditNoteId) == 1;
    }

    /** An allocation match records the local marker but retains quarantine for operator review. */
    public boolean reconcileCreditNoteAllocation(String creditNoteId) throws IOException {
        Map<String, Object> note = loadQuarantinedCreditNote(creditNoteId);
        if (note.get("xero_allocated_at") != null) return false;
        String xeroId = stringValue(note.get("xero_credit_note_id"));
        if (xeroId == null) return false;
        InvoiceEntity invoice = invoiceRepository.findById(String.valueOf(note.get("invoice_id"))).orElse(null);
        if (invoice == null || invoice.getXeroInvoiceId() == null || invoice.getXeroInvoiceId().isBlank()) return false;
        String date = note.get("credit_note_date") instanceof Date sqlDate
                ? sqlDate.toLocalDate().toString() : String.valueOf(note.get("credit_note_date"));
        JsonNode response = getXero(creditNotesBaseUrl() + "/" +
                URLEncoder.encode(xeroId, StandardCharsets.UTF_8));
        if (!uniqueAllocationMatch(response, xeroId, invoice.getXeroInvoiceId(), date,
                note.get("total_cents") instanceof Number n ? n.longValue() : null)) return false;
        return jdbcTemplate.update("""
                UPDATE credit_note SET xero_allocated_at=CURRENT_TIMESTAMP,
                    xero_sync_error='Xero allocation found; operator review required before release'
                WHERE id=? AND xero_sync_status='RECONCILIATION_REQUIRED'
                    AND xero_credit_note_id=? AND xero_allocated_at IS NULL
                """, creditNoteId, xeroId) == 1;
    }

    private Map<String, Object> loadQuarantinedCreditNote(String id) {
        Map<String, Object> note = jdbcTemplate.queryForMap("""
                SELECT id, invoice_id, credit_note_no, credit_note_date, currency, total_cents,
                       xero_credit_note_id, xero_allocated_at, xero_sync_status
                  FROM credit_note WHERE id=?
                """, id);
        if (!"RECONCILIATION_REQUIRED".equalsIgnoreCase(String.valueOf(note.get("xero_sync_status"))))
            throw new IllegalStateException("Credit note is not quarantined for Xero reconciliation");
        return note;
    }

    private static String stringValue(Object value) {
        return value == null || value.toString().isBlank() ? null : value.toString();
    }

    private String creditNotesBaseUrl() throws IOException {
        String config = auth.checkXeroInfo();
        if (config == null || config.isBlank()) throw new IOException("Xero is not connected");
        return auth.getXeroProperty(config, "XERO-CREDIT-NOTES-URL", CREDIT_NOTES_URL).replaceAll("/+$", "");
    }

    private JsonNode getXero(String url) throws IOException {
        String config = auth.checkXeroInfo();
        if (config == null || config.isBlank()) throw new IOException("Xero is not connected");
        String tenantId = auth.getXeroProperty(config, XeroUtils.XERO_TENANT_ID);
        if (tenantId == null || tenantId.isBlank()) throw new IOException("Xero organisation is not selected");
        String token = auth.refreshAccessToken(config);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Xero-Tenant-Id", tenantId);
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) throw new IOException("Xero reconciliation lookup failed: HTTP " + code);
            try (var stream = conn.getInputStream()) { return mapper.readTree(stream); }
        } finally { conn.disconnect(); }
    }

    static String uniqueCreditNoteMatch(JsonNode response, String reference, String currency, Long totalCents) {
        if (response == null || !response.path("CreditNotes").isArray() || reference == null || totalCents == null) return null;
        String expectedCurrency = currency == null || currency.isBlank() ? "ZAR" : currency.trim();
        BigDecimal expected = BigDecimal.valueOf(totalCents, 2);
        int count = 0;
        String result = null;
        for (JsonNode note : response.path("CreditNotes")) {
            if (!reference.equals(note.path("Reference").asText(null))) continue;
            if (!expectedCurrency.equalsIgnoreCase(note.path("CurrencyCode").asText(""))) continue;
            if (!note.path("Total").isNumber() || expected.compareTo(note.path("Total").decimalValue()) != 0) continue;
            String id = note.path("CreditNoteID").asText(null);
            if (id == null || id.isBlank()) continue;
            result = id;
            count++;
        }
        return count == 1 ? result : null;
    }

    static boolean uniqueAllocationMatch(JsonNode response, String creditNoteId, String invoiceId,
                                         String date, Long amountCents) {
        if (response == null || !response.path("CreditNotes").isArray() || creditNoteId == null ||
                invoiceId == null || date == null || amountCents == null) return false;
        BigDecimal expected = BigDecimal.valueOf(amountCents, 2);
        int matches = 0;
        int notes = 0;
        for (JsonNode note : response.path("CreditNotes")) {
            if (!creditNoteId.equals(note.path("CreditNoteID").asText(null))) continue;
            notes++;
            if (!note.path("Allocations").isArray()) return false;
            for (JsonNode allocation : note.path("Allocations")) {
                if (!invoiceId.equals(allocation.path("Invoice").path("InvoiceID").asText(null))) continue;
                if (!allocation.path("Amount").isNumber() ||
                        expected.compareTo(allocation.path("Amount").decimalValue()) != 0) continue;
                String xeroDate = allocation.path("DateString").asText(null);
                if (xeroDate == null || xeroDate.length() < 10 || !date.equals(xeroDate.substring(0, 10))) continue;
                matches++;
            }
        }
        return notes == 1 && matches == 1;
    }

    /** Xero invoice GET returns Payments on the invoice; never match solely by amount. */
    static String uniquePaymentMatch(JsonNode body, String invoiceId, String reference, String date, Long cents) {
        if (body == null || !body.path("Invoices").isArray() || invoiceId == null ||
                reference == null || reference.isBlank() || date == null || cents == null) return null;
        BigDecimal amount = BigDecimal.valueOf(cents, 2);
        int count = 0;
        String matched = null;
        for (JsonNode invoice : body.path("Invoices")) {
            if (!invoiceId.equals(invoice.path("InvoiceID").asText(null))) continue;
            if (!invoice.path("Payments").isArray()) continue;
            for (JsonNode item : invoice.path("Payments")) {
                if (!reference.equals(item.path("Reference").asText(null))) continue;
                if (!item.path("Amount").isNumber() ||
                        amount.compareTo(item.path("Amount").decimalValue()) != 0) continue;
                String actualDate = item.path("DateString").asText(null);
                if (actualDate == null || actualDate.length() < 10 || !date.equals(actualDate.substring(0, 10))) continue;
                String id = item.path("PaymentID").asText(null);
                if (id == null || id.isBlank()) continue;
                count++;
                matched = id;
            }
        }
        return count == 1 ? matched : null;
    }

    /** Requires exactly one matching invoice; avoids linking by invoice number alone. */
    static String uniqueInvoiceMatch(JsonNode response, String number, String currency, Long totalCents) {
        if (response == null || !response.path("Invoices").isArray() || number == null || totalCents == null) return null;
        String expectedCurrency = currency == null || currency.isBlank() ? "ZAR" : currency.trim();
        BigDecimal expectedAmount = BigDecimal.valueOf(totalCents, 2);
        int count = 0;
        String id = null;
        for (JsonNode item : response.path("Invoices")) {
            if (!number.equals(item.path("InvoiceNumber").asText(null))) continue;
            if (!expectedCurrency.equalsIgnoreCase(item.path("CurrencyCode").asText(""))) continue;
            if (!item.hasNonNull("Total") || !item.path("Total").isNumber()) continue;
            if (expectedAmount.compareTo(item.path("Total").decimalValue()) != 0) continue;
            String candidate = item.path("InvoiceID").asText(null);
            if (candidate == null || candidate.isBlank()) continue;
            count++;
            id = candidate;
        }
        return count == 1 ? id : null;
    }
}
