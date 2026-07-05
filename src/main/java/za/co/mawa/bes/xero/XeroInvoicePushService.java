package za.co.mawa.bes.xero;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoiceLineEntity;
import za.co.mawa.bes.entity.PartnerContactEntity;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.PartnerIdentityEntity;
import za.co.mawa.bes.repository.InvoiceRepository;
import za.co.mawa.bes.repository.PartnerContactRepository;
import za.co.mawa.bes.repository.PartnerIdentityRepository;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.service.TenantAdminService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class XeroInvoicePushService {

    private static final String DEFAULT_INVOICE_URL = "https://api.xero.com/api.xro/2.0/Invoices";

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private PartnerIdentityRepository partnerIdentityRepository;

    @Autowired
    private PartnerContactRepository partnerContactRepository;

    @Autowired
    private TenantAdminService tenantAdminService;

    @Autowired
    private XeroAuthService xeroAuthService;

    @Autowired
    private XeroIntegrationSettingsService xeroIntegrationSettingsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void pushInvoice(String invoiceId) throws IOException {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        if (!isBlank(invoice.getXeroInvoiceId())) {
            invoice.setIntegrationStatus("POSTED");
            invoiceRepository.save(invoice);
            return;
        }

        invoice.setIntegrationStatus("SENDING");
        invoice.setIntegrationLastAttemptAt(LocalDateTime.now());
        invoice.setIntegrationError(null);
        invoiceRepository.save(invoice);

        try {
            String xeroConfigTenant = xeroAuthService.checkXeroInfo();
            if (isBlank(xeroConfigTenant)) {
                throw new IOException("Xero integration is enabled but no Xero refresh token/service provider link is configured");
            }

            String accessToken = xeroAuthService.refreshAccessToken(xeroConfigTenant);
            String xeroTenantId = xeroAuthService.getXeroProperty(xeroConfigTenant, XeroUtils.XERO_TENANT_ID);
            if (isBlank(xeroTenantId)) {
                throw new IOException("Xero tenant id is not configured");
            }

            String invoiceUrl = xeroAuthService.getXeroProperty(xeroConfigTenant, "XERO-BASE-URL", DEFAULT_INVOICE_URL);

            ObjectNode payload = buildInvoicePayload(invoice);
            JsonNode response = postInvoice(invoiceUrl, accessToken, xeroTenantId, payload);
            JsonNode xeroInvoice = response.path("Invoices").isArray() && response.path("Invoices").size() > 0
                    ? response.path("Invoices").get(0)
                    : null;
            if (xeroInvoice == null || xeroInvoice.isMissingNode() || xeroInvoice.isNull()) {
                throw new IOException("Xero invoice response did not contain an invoice");
            }

            invoice.setXeroInvoiceId(xeroInvoice.path("InvoiceID").asText(null));
            invoice.setXeroInvoiceNo(xeroInvoice.path("InvoiceNumber").asText(null));
            invoice.setIntegrationStatus("POSTED");
            invoice.setIntegrationError(null);
            invoice.setIntegrationPostedAt(LocalDateTime.now());
            invoiceRepository.save(invoice);
        } catch (Exception e) {
            markFailed(invoice.getId(), e.getMessage());
            if (e instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Failed to push invoice to Xero: " + e.getMessage(), e);
        }
    }

    public void markFailed(String invoiceId, String errorMessage) {
        if (isBlank(invoiceId)) {
            return;
        }
        invoiceRepository.findById(invoiceId).ifPresent(invoice -> {
            invoice.setIntegrationStatus("FAILED");
            invoice.setIntegrationLastAttemptAt(LocalDateTime.now());
            invoice.setIntegrationError(trim(errorMessage, 2000));
            invoiceRepository.save(invoice);
        });
    }

    private ObjectNode buildInvoicePayload(InvoiceEntity invoice) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode invoices = objectMapper.createArrayNode();
        ObjectNode xeroInvoice = objectMapper.createObjectNode();

        xeroInvoice.put("Type", "ACCREC");
        xeroInvoice.set("Contact", buildContact(invoice.getPartnerId()));
        if (invoice.getInvoiceDate() != null) {
            xeroInvoice.put("Date", invoice.getInvoiceDate().toString());
        }
        if (invoice.getDueDate() != null) {
            xeroInvoice.put("DueDate", invoice.getDueDate().toString());
        }
        xeroInvoice.put("Reference", firstNonBlank(invoice.getExternalRef(), invoice.getInvoiceNo(), invoice.getId()));
        xeroInvoice.put("Status", xeroIntegrationSettingsService.invoiceStatus());
        xeroInvoice.put("CurrencyCode", firstNonBlank(invoice.getCurrency(), "ZAR"));
        xeroInvoice.put("LineAmountTypes", xeroIntegrationSettingsService.lineAmountTypes());

        ArrayNode lineItems = objectMapper.createArrayNode();
        if (invoice.getLines() != null) {
            for (InvoiceLineEntity line : invoice.getLines()) {
                lineItems.add(buildLineItem(line));
            }
        }
        if (lineItems.isEmpty()) {
            lineItems.add(buildFallbackLineItem(invoice));
        }
        xeroInvoice.set("LineItems", lineItems);

        invoices.add(xeroInvoice);
        root.set("Invoices", invoices);
        return root;
    }

    private ObjectNode buildContact(String partnerId) {
        ObjectNode contact = objectMapper.createObjectNode();
        String xeroContactId = getXeroContactId(partnerId);
        if (!isBlank(xeroContactId)) {
            contact.put("ContactID", xeroContactId);
            return contact;
        }

        PartnerEntity partner = partnerRepository.findById(partnerId).orElse(null);
        String name = partner == null ? partnerId : firstNonBlank(joinNames(partner), partner.getNo(), partner.getId());
        contact.put("Name", name);

        String email = getEmailAddress(partnerId);
        if (!isBlank(email)) {
            contact.put("EmailAddress", email);
        }
        return contact;
    }

    private ObjectNode buildLineItem(InvoiceLineEntity line) {
        ObjectNode lineItem = objectMapper.createObjectNode();
        lineItem.put("Description", firstNonBlank(line.getDescription(), "Invoice line"));
        lineItem.put("Quantity", line.getQuantity() == null ? 1.0 : line.getQuantity());
        lineItem.put("UnitAmount", centsToAmount(line.getUnitPriceCents()));
        lineItem.put("AccountCode", xeroIntegrationSettingsService.invoiceAccountCode());
        lineItem.put("TaxType", xeroIntegrationSettingsService.invoiceTaxType());
        return lineItem;
    }

    private ObjectNode buildFallbackLineItem(InvoiceEntity invoice) {
        ObjectNode lineItem = objectMapper.createObjectNode();
        lineItem.put("Description", firstNonBlank(invoice.getNotes(), "Invoice " + invoice.getInvoiceNo()));
        lineItem.put("Quantity", 1);
        lineItem.put("UnitAmount", centsToAmount(invoice.getTotalCents()));
        lineItem.put("AccountCode", xeroIntegrationSettingsService.invoiceAccountCode());
        lineItem.put("TaxType", xeroIntegrationSettingsService.invoiceTaxType());
        return lineItem;
    }

    private JsonNode postInvoice(String invoiceUrl, String accessToken, String xeroTenantId, ObjectNode payload) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(invoiceUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setRequestProperty("Xero-Tenant-Id", xeroTenantId);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = objectMapper.writeValueAsBytes(payload);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            String responseBody = readResponse(connection, responseCode >= 300);
            if (responseCode >= 300) {
                throw new IOException("Xero invoice request failed. Response code: " + responseCode + ". Response: " + responseBody);
            }
            return objectMapper.readTree(responseBody);
        } catch (SocketTimeoutException e) {
            throw new IOException("Xero invoice request timed out: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readResponse(HttpURLConnection connection, boolean error) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                error && connection.getErrorStream() != null ? connection.getErrorStream() : connection.getInputStream(),
                StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }

    private String getXeroContactId(String partnerId) {
        if (isBlank(partnerId)) {
            return null;
        }
        List<PartnerIdentityEntity> identities = partnerIdentityRepository.findPartnerIdentityByPartner(partnerId);
        for (PartnerIdentityEntity identity : identities) {
            if (identity.getPartnerIdentityPK() != null
                    && XeroUtils.XERO_CONTACT_ID.equals(identity.getPartnerIdentityPK().getType())) {
                return identity.getPartnerIdentityPK().getValue();
            }
        }
        return null;
    }

    private String getEmailAddress(String partnerId) {
        if (isBlank(partnerId)) {
            return null;
        }
        List<PartnerContactEntity> contacts = partnerContactRepository.findContactsByPartner(partnerId);
        for (PartnerContactEntity contact : contacts) {
            if (contact.getPartnerContactPK() != null
                    && contact.getPartnerContactPK().getType() != null
                    && contact.getPartnerContactPK().getType().toUpperCase().contains("EMAIL")) {
                return contact.getValue();
            }
        }
        return null;
    }

    private String joinNames(PartnerEntity partner) {
        return String.join(" ",
                emptyIfNull(partner.getName1()),
                emptyIfNull(partner.getName2()),
                emptyIfNull(partner.getName3())
        ).trim();
    }

    private double centsToAmount(Long cents) {
        return BigDecimal.valueOf(cents == null ? 0L : cents)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim());
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
