package za.co.mawa.bes.xero;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.repository.InvoiceRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/** Conservative, explicitly invoked reconciliation; never triggers a Xero write. */
@Service
public class XeroInvoiceReconciliationService {
    private static final String DEFAULT_URL = "https://api.xero.com/api.xro/2.0/Invoices";
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private XeroAuthService auth;

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
