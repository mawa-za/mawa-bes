package za.co.mawa.bes.xero;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoiceLineEntity;
import za.co.mawa.bes.entity.InvoicePaymentEntity;
import za.co.mawa.bes.entity.PartnerContactEntity;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.PartnerIdentityEntity;
import za.co.mawa.bes.repository.InvoiceRepository;
import za.co.mawa.bes.repository.InvoicePaymentRepository;
import za.co.mawa.bes.repository.PartnerContactRepository;
import za.co.mawa.bes.repository.PartnerIdentityRepository;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.ProductRepository;

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
import java.sql.Date;
import java.util.Map;
import java.util.List;
import java.util.Locale;

@Service
public class XeroInvoicePushService {

    private static final String DEFAULT_INVOICE_URL = "https://api.xero.com/api.xro/2.0/Invoices";
    private static final String DEFAULT_PAYMENT_URL = "https://api.xero.com/api.xro/2.0/Payments";
    private static final String DEFAULT_CREDIT_NOTE_URL = "https://api.xero.com/api.xro/2.0/CreditNotes";

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoicePaymentRepository invoicePaymentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PartnerRepository partnerRepository;
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PartnerIdentityRepository partnerIdentityRepository;

    @Autowired
    private PartnerContactRepository partnerContactRepository;

    @Autowired
    private XeroAuthService xeroAuthService;

    @Autowired
    private XeroIntegrationSettingsService xeroIntegrationSettingsService;
    @Autowired
    private XeroMasterDataPushService xeroMasterDataPushService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void pushInvoice(String invoiceId) throws IOException {
        // Invoice lines are LAZY. Fetch them before the repository transaction
        // closes because the remainder of this method performs external calls.
        InvoiceEntity invoice = invoiceRepository.findByIdWithLines(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        boolean update = !isBlank(invoice.getXeroInvoiceId());
        invoice.setIntegrationStatus(update ? "UPDATING" : "SENDING");
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

            xeroMasterDataPushService.pushCustomer(invoice.getPartnerId());
            if (invoice.getLines() != null) {
                for (InvoiceLineEntity line : invoice.getLines()) {
                    if (!isBlank(line.getProductId())) xeroMasterDataPushService.pushProduct(line.getProductId());
                }
            }

            ObjectNode payload = buildInvoicePayload(invoice);
            JsonNode response = postInvoice(invoiceUrl, accessToken, xeroTenantId, payload);
            JsonNode xeroInvoice = response.path("Invoices").isArray() && response.path("Invoices").size() > 0
                    ? response.path("Invoices").get(0)
                    : null;
            if (xeroInvoice == null || xeroInvoice.isMissingNode() || xeroInvoice.isNull()) {
                throw new IOException("Xero invoice response did not contain an invoice");
            }

            String returnedInvoiceId = xeroInvoice.path("InvoiceID").asText(null);
            if (isBlank(returnedInvoiceId)) {
                throw new IOException("Xero invoice response did not contain InvoiceID");
            }
            invoice.setXeroInvoiceId(returnedInvoiceId);
            invoice.setXeroInvoiceNo(firstNonBlank(
                    xeroInvoice.path("InvoiceNumber").asText(null), invoice.getInvoiceNo()));
            invoice.setIntegrationStatus(update ? "UPDATED" : "POSTED");
            invoice.setIntegrationError(null);
            invoice.setIntegrationPostedAt(LocalDateTime.now());
            invoiceRepository.save(invoice);

            if ("AUTHORISED".equals(mapInvoiceStatus(invoice.getStatus()))) {
                syncCreditNotes(invoice, accessToken, xeroTenantId);
                syncPayments(invoice, accessToken, xeroTenantId);
            }
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

    ObjectNode buildInvoicePayload(InvoiceEntity invoice) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode invoices = objectMapper.createArrayNode();
        ObjectNode xeroInvoice = objectMapper.createObjectNode();

        xeroInvoice.put("Type", "ACCREC");
        if (!isBlank(invoice.getXeroInvoiceId())) {
            xeroInvoice.put("InvoiceID", invoice.getXeroInvoiceId());
        }
        if (!isBlank(invoice.getInvoiceNo())) {
            xeroInvoice.put("InvoiceNumber", invoice.getInvoiceNo());
        }
        xeroInvoice.set("Contact", buildContact(invoice.getPartnerId()));
        if (invoice.getInvoiceDate() != null) {
            xeroInvoice.put("Date", invoice.getInvoiceDate().toString());
        }
        if (invoice.getDueDate() != null) {
            xeroInvoice.put("DueDate", invoice.getDueDate().toString());
        }
        xeroInvoice.put("Reference", firstNonBlank(invoice.getExternalRef(), invoice.getInvoiceNo(), invoice.getId()));
        xeroInvoice.put("Status", mapInvoiceStatus(invoice.getStatus()));
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

    String mapInvoiceStatus(String mawaStatus) {
        String status = isBlank(mawaStatus) ? "DRAFT" : mawaStatus.trim().toUpperCase(Locale.ROOT);
        return switch (status) {
            case "CANCELLED", "CANCELED", "VOIDED" -> "VOIDED";
            case "DRAFT", "NEW", "AWAITING_APPROVAL", "REJECTED" -> "DRAFT";
            // Xero derives PAID from allocated payments; PAID must never be sent as
            // the invoice creation/update status.
            default -> "AUTHORISED";
        };
    }

    private void syncPayments(InvoiceEntity invoice, String accessToken, String xeroTenantId) throws IOException {
        List<InvoicePaymentEntity> payments = invoicePaymentRepository.findByInvoiceId(invoice.getId());
        if (payments.isEmpty()) return;

        String accountCode = xeroIntegrationSettingsService.paymentAccountCode();
        if (isBlank(accountCode)) {
            throw new IOException("Configure XERO / PAYMENT-ACCOUNT-CODE before synchronising invoice payments");
        }
        String paymentUrl = xeroAuthService.getXeroProperty(
                xeroAuthService.checkXeroInfo(), "XERO-PAYMENTS-URL", DEFAULT_PAYMENT_URL);

        for (InvoicePaymentEntity payment : payments) {
            if ("CANCELLED".equalsIgnoreCase(payment.getStatus())) {
                if (!isBlank(payment.getXeroPaymentId())
                        && !"DELETED".equalsIgnoreCase(payment.getXeroSyncStatus())) {
                    deletePayment(paymentUrl + "/" + payment.getXeroPaymentId(), accessToken, xeroTenantId);
                    payment.setXeroSyncStatus("DELETED");
                    payment.setXeroSyncError(null);
                    payment.setXeroSyncedAt(LocalDateTime.now());
                    invoicePaymentRepository.save(payment);
                }
                continue;
            }
            if (!isBlank(payment.getXeroPaymentId())) continue;
            payment.setXeroSyncStatus("SENDING");
            payment.setXeroSyncError(null);
            invoicePaymentRepository.save(payment);
            try {
                ObjectNode root = objectMapper.createObjectNode();
                ObjectNode xeroPayment = objectMapper.createObjectNode();
                ObjectNode invoiceReference = objectMapper.createObjectNode();
                invoiceReference.put("InvoiceID", invoice.getXeroInvoiceId());
                xeroPayment.set("Invoice", invoiceReference);
                ObjectNode account = objectMapper.createObjectNode();
                account.put("Code", accountCode);
                xeroPayment.set("Account", account);
                if (payment.getPaymentDate() != null) {
                    xeroPayment.put("Date", payment.getPaymentDate().toLocalDate().toString());
                }
                xeroPayment.put("Amount", centsToAmount(payment.getAmountCents()));
                if (!isBlank(payment.getReferenceNo())) {
                    xeroPayment.put("Reference", payment.getReferenceNo());
                }
                root.set("Payments", objectMapper.createArrayNode().add(xeroPayment));

                JsonNode response = postInvoice(paymentUrl, accessToken, xeroTenantId, root);
                JsonNode saved = response.path("Payments").isArray() && response.path("Payments").size() > 0
                        ? response.path("Payments").get(0) : null;
                String paymentId = saved == null ? null : saved.path("PaymentID").asText(null);
                if (isBlank(paymentId)) {
                    throw new IOException("Xero payment response did not contain PaymentID");
                }
                payment.setXeroPaymentId(paymentId);
                payment.setXeroSyncStatus("POSTED");
                payment.setXeroSyncError(null);
                payment.setXeroSyncedAt(LocalDateTime.now());
                invoicePaymentRepository.save(payment);
            } catch (Exception error) {
                payment.setXeroSyncStatus("FAILED");
                payment.setXeroSyncError(trim(error.getMessage(), 2000));
                invoicePaymentRepository.save(payment);
                if (error instanceof IOException ioException) throw ioException;
                throw new IOException("Failed to push invoice payment to Xero: " + error.getMessage(), error);
            }
        }
    }

    private void deletePayment(String endpoint, String accessToken, String xeroTenantId) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("DELETE");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setRequestProperty("Xero-Tenant-Id", xeroTenantId);
            connection.setRequestProperty("Accept", "application/json");
            int responseCode = connection.getResponseCode();
            if (responseCode >= 300 && responseCode != 404) {
                throw new IOException("Xero payment reversal failed. Response code: " + responseCode
                        + ". Response: " + readResponse(connection, true));
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void syncCreditNotes(InvoiceEntity invoice, String accessToken, String xeroTenantId) throws IOException {
        List<Map<String, Object>> notes = jdbcTemplate.queryForList("""
                SELECT id, credit_note_no, credit_note_date, reason, total_cents,
                       xero_credit_note_id, xero_allocated_at
                  FROM credit_note
                 WHERE invoice_id = ? AND UPPER(status) = 'ISSUED'
                 ORDER BY credit_note_date, created_at
                """, invoice.getId());
        if (notes.isEmpty()) return;

        String baseUrl = xeroAuthService.getXeroProperty(
                xeroAuthService.checkXeroInfo(), "XERO-CREDIT-NOTES-URL", DEFAULT_CREDIT_NOTE_URL);
        for (Map<String, Object> note : notes) {
            String noteId = String.valueOf(note.get("id"));
            String xeroCreditNoteId = valueAsString(note.get("xero_credit_note_id"));
            try {
                if (isBlank(xeroCreditNoteId)) {
                    jdbcTemplate.update("UPDATE credit_note SET xero_sync_status='SENDING', xero_sync_error=NULL WHERE id=?", noteId);
                    ObjectNode credit = objectMapper.createObjectNode();
                    credit.put("Type", "ACCRECCREDIT");
                    credit.set("Contact", buildContact(invoice.getPartnerId()));
                    Object noteDate = note.get("credit_note_date");
                    if (noteDate instanceof Date sqlDate) credit.put("Date", sqlDate.toLocalDate().toString());
                    else if (noteDate != null) credit.put("Date", noteDate.toString());
                    credit.put("Reference", valueAsString(note.get("credit_note_no")));
                    credit.put("Status", "AUTHORISED");
                    credit.put("LineAmountTypes", xeroIntegrationSettingsService.lineAmountTypes());
                    ObjectNode line = objectMapper.createObjectNode();
                    line.put("Description", firstNonBlank(valueAsString(note.get("reason")),
                            "Credit against " + invoice.getInvoiceNo()));
                    line.put("Quantity", 1);
                    line.put("UnitAmount", centsToAmount(numberAsLong(note.get("total_cents"))));
                    line.put("AccountCode", xeroIntegrationSettingsService.invoiceAccountCode());
                    line.put("TaxType", xeroIntegrationSettingsService.invoiceTaxType());
                    credit.set("LineItems", objectMapper.createArrayNode().add(line));
                    ObjectNode root = objectMapper.createObjectNode();
                    root.set("CreditNotes", objectMapper.createArrayNode().add(credit));

                    JsonNode response = postInvoice(baseUrl, accessToken, xeroTenantId, root);
                    JsonNode saved = response.path("CreditNotes").isArray() && response.path("CreditNotes").size() > 0
                            ? response.path("CreditNotes").get(0) : null;
                    xeroCreditNoteId = saved == null ? null : saved.path("CreditNoteID").asText(null);
                    if (isBlank(xeroCreditNoteId)) throw new IOException("Xero credit note response did not contain CreditNoteID");
                    jdbcTemplate.update("""
                            UPDATE credit_note
                               SET xero_credit_note_id=?, xero_sync_status='POSTED',
                                   xero_sync_error=NULL, xero_synced_at=CURRENT_TIMESTAMP
                             WHERE id=?
                            """, xeroCreditNoteId, noteId);
                }

                if (note.get("xero_allocated_at") == null) {
                    ObjectNode allocation = objectMapper.createObjectNode();
                    allocation.put("Amount", centsToAmount(numberAsLong(note.get("total_cents"))));
                    Object allocationDate = note.get("credit_note_date");
                    if (allocationDate instanceof Date sqlDate) allocation.put("Date", sqlDate.toLocalDate().toString());
                    else if (allocationDate != null) allocation.put("Date", allocationDate.toString());
                    ObjectNode invoiceReference = objectMapper.createObjectNode();
                    invoiceReference.put("InvoiceID", invoice.getXeroInvoiceId());
                    allocation.set("Invoice", invoiceReference);
                    ObjectNode root = objectMapper.createObjectNode();
                    root.set("Allocations", objectMapper.createArrayNode().add(allocation));
                    postInvoice(baseUrl + "/" + xeroCreditNoteId + "/Allocations",
                            accessToken, xeroTenantId, root);
                    jdbcTemplate.update("""
                            UPDATE credit_note
                               SET xero_sync_status='ALLOCATED', xero_sync_error=NULL,
                                   xero_allocated_at=CURRENT_TIMESTAMP
                             WHERE id=?
                            """, noteId);
                }
            } catch (Exception error) {
                jdbcTemplate.update("UPDATE credit_note SET xero_sync_status='FAILED', xero_sync_error=? WHERE id=?",
                        trim(error.getMessage(), 2000), noteId);
                if (error instanceof IOException ioException) throw ioException;
                throw new IOException("Failed to push credit note to Xero: " + error.getMessage(), error);
            }
        }
    }

    private ObjectNode buildContact(String partnerId) {
        ObjectNode contact = objectMapper.createObjectNode();
        PartnerEntity partner = partnerRepository.findById(partnerId).orElse(null);
        String xeroContactId = partner == null ? null : partner.getXeroContactId();
        if (isBlank(xeroContactId)) xeroContactId = getXeroContactId(partnerId);
        if (!isBlank(xeroContactId)) {
            contact.put("ContactID", xeroContactId);
            return contact;
        }

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
        if (!isBlank(line.getProductId())) {
            productRepository.findById(line.getProductId()).ifPresent(product -> {
                if (!isBlank(product.getCode())) lineItem.put("ItemCode", product.getCode());
            });
        }
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

    private long numberAsLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private String valueAsString(Object value) {
        return value == null ? null : value.toString();
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
