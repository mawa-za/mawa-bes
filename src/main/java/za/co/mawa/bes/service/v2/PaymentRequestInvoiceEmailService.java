package za.co.mawa.bes.service.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.dto.EmailDto;
import za.co.mawa.bes.dto.File;
import za.co.mawa.bes.dto.PropertyDto;
import za.co.mawa.bes.entity.AttachmentEntity;
import za.co.mawa.bes.entity.v2.PaymentRequestEntity;
import za.co.mawa.bes.enums.PaymentRequestStatus;
import za.co.mawa.bes.repository.AttachmentRepository;
import za.co.mawa.bes.repository.v2.PaymentRequestRepository;
import za.co.mawa.bes.service.AttachmentService;
import za.co.mawa.bes.service.EmailService;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class PaymentRequestInvoiceEmailService {
    private static final Logger log = LoggerFactory.getLogger(PaymentRequestInvoiceEmailService.class);
    private static final String CONFIG_ID = "DEFAULT";
    private static final String DEFAULT_DOCUMENT_TYPE = "SUPPLIER-INVOICE";
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final JdbcTemplate jdbcTemplate;
    private final PaymentRequestRepository paymentRequestRepository;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentService attachmentService;
    private final EmailService emailService;

    public PaymentRequestInvoiceEmailService(
            JdbcTemplate jdbcTemplate,
            PaymentRequestRepository paymentRequestRepository,
            AttachmentRepository attachmentRepository,
            AttachmentService attachmentService,
            EmailService emailService) {
        this.jdbcTemplate = jdbcTemplate;
        this.paymentRequestRepository = paymentRequestRepository;
        this.attachmentRepository = attachmentRepository;
        this.attachmentService = attachmentService;
        this.emailService = emailService;
    }

    public Map<String, Object> getConfiguration() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM payment_request_invoice_email_configuration WHERE id = ?", CONFIG_ID);
        if (rows.isEmpty()) {
            jdbcTemplate.update("""
                    INSERT INTO payment_request_invoice_email_configuration(id, enabled, subject_template, document_types)
                    VALUES(?, 0, 'Approved supplier invoice payment request {{requestNo}}', ?)
                    """, CONFIG_ID, DEFAULT_DOCUMENT_TYPE);
            rows = jdbcTemplate.queryForList(
                    "SELECT * FROM payment_request_invoice_email_configuration WHERE id = ?", CONFIG_ID);
        }
        Map<String, Object> response = new LinkedHashMap<>(rows.get(0));
        List<String> documentTypes = configuredDocumentTypes(response.get("document_types"));
        response.put("document_types", String.join(";", documentTypes));
        response.put("documentTypes", documentTypes);
        response.put("deliverySummary", deliverySummary());
        response.put("recentFailures", recentFailures());
        return response;
    }

    @Transactional
    public Map<String, Object> saveConfiguration(Map<String, Object> request, String userId) {
        boolean enabled = booleanValue(request == null ? null : request.get("enabled"));
        String recipients = normalizeRecipients(text(request, "recipientEmails"));
        if (enabled && !StringUtils.hasText(recipients)) {
            throw new IllegalArgumentException("At least one invoice email recipient is required before enabling the feature");
        }
        validateRecipients(recipients);
        String subject = firstNonBlank(
                text(request, "subjectTemplate"),
                "Approved supplier invoice payment request {{requestNo}}"
        );
        String body = text(request, "bodyMessage");
        boolean documentTypesSupplied = request != null && request.containsKey("documentTypes");
        List<String> documentTypes = documentTypesSupplied
                ? normalizeDocumentTypes(request.get("documentTypes"))
                : configuredDocumentTypes(getConfiguration().get("document_types"));
        if (enabled && documentTypes.isEmpty()) {
            throw new IllegalArgumentException("At least one payment request document type is required before enabling the feature");
        }
        validateDocumentTypes(documentTypes);
        String storedDocumentTypes = String.join(";", documentTypes.isEmpty() ? List.of(DEFAULT_DOCUMENT_TYPE) : documentTypes);
        jdbcTemplate.update("""
                INSERT INTO payment_request_invoice_email_configuration(
                    id, enabled, recipient_emails, subject_template, body_message, document_types, created_by, updated_by
                ) VALUES(?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    enabled = VALUES(enabled),
                    recipient_emails = VALUES(recipient_emails),
                    subject_template = VALUES(subject_template),
                    body_message = VALUES(body_message),
                    document_types = VALUES(document_types),
                    updated_by = VALUES(updated_by)
                """, CONFIG_ID, enabled, recipients, subject, body, storedDocumentTypes, actor(userId), actor(userId));
        return getConfiguration();
    }

    /**
     * Called after approval. Failures are recorded but deliberately do not roll
     * back the business approval transaction.
     */
    public Map<String, Object> deliverAfterApproval(String paymentRequestId, String triggeredBy) {
        try {
            return deliver(paymentRequestId, triggeredBy, false);
        } catch (Exception ex) {
            log.error("Approved payment request document email failed for payment request {}", paymentRequestId, ex);
            return Map.of(
                    "paymentRequestId", paymentRequestId,
                    "status", "FAILED",
                    "message", rootMessage(ex)
            );
        }
    }

    public Map<String, Object> retry(String paymentRequestId, String triggeredBy) {
        return deliver(paymentRequestId, triggeredBy, true);
    }

    public Map<String, Object> runBackfill(Integer requestedLimit, Boolean retryFailed, String triggeredBy) {
        int limit = Math.max(1, Math.min(requestedLimit == null ? 250 : requestedLimit, 1000));
        boolean includeFailed = Boolean.TRUE.equals(retryFailed);
        List<String> configuredTypes = configuredDocumentTypes(getConfiguration().get("document_types"));
        List<String> ids = findBackfillCandidates(configuredTypes, limit, includeFailed);

        int sent = 0;
        int skipped = 0;
        int failed = 0;
        List<Map<String, Object>> results = new ArrayList<>();
        for (String id : ids) {
            try {
                Map<String, Object> result = deliver(id, triggeredBy, includeFailed);
                results.add(result);
                String status = String.valueOf(result.get("status"));
                if ("SENT".equals(status)) sent++;
                else if ("FAILED".equals(status)) failed++;
                else skipped++;
            } catch (Exception ex) {
                failed++;
                log.error("Payment request document email backfill failed for payment request {}", id, ex);
                results.add(Map.of("paymentRequestId", id, "status", "FAILED", "message", rootMessage(ex)));
            }
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("selected", ids.size());
        response.put("processed", ids.size());
        response.put("sent", sent);
        response.put("skipped", skipped);
        response.put("failed", failed);
        response.put("limit", limit);
        response.put("documentTypes", configuredTypes);
        response.put("results", results);
        response.put("deliverySummary", deliverySummary());
        return response;
    }

    private List<String> findBackfillCandidates(List<String> configuredTypes, int limit, boolean includeFailed) {
        if (configuredTypes.isEmpty()) return List.of();

        String placeholders = String.join(",", java.util.Collections.nCopies(configuredTypes.size(), "?"));
        String sql = """
                SELECT pr.id
                  FROM payment_request pr
                 WHERE pr.status IN ('APPROVED', 'QUEUED_FOR_PAYMENT', 'PROCESSED', 'PAID')
                   AND EXISTS (
                       SELECT 1
                         FROM attachment a
                        WHERE a.object_id = pr.id
                          AND UPPER(COALESCE(a.document_type, '')) IN (%s)
                   )
                 ORDER BY COALESCE(pr.approved_at, pr.created_at), pr.id
                 LIMIT ? OFFSET ?
                """.formatted(placeholders);

        List<String> selected = new ArrayList<>();
        int pageSize = Math.min(500, Math.max(100, limit));
        int offset = 0;
        while (selected.size() < limit) {
            List<Object> args = new ArrayList<>(configuredTypes.size() + 2);
            configuredTypes.stream().map(value -> value.toUpperCase(Locale.ROOT)).forEach(args::add);
            args.add(pageSize);
            args.add(offset);

            List<String> page = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> rs.getString(1),
                    args.toArray()
            );
            if (page.isEmpty()) break;
            offset += page.size();

            for (String id : page) {
                if (isBackfillDeliveryEligible(id, includeFailed)) {
                    selected.add(id);
                    if (selected.size() >= limit) break;
                }
            }
            if (page.size() < pageSize) break;
        }
        return selected;
    }

    private boolean isBackfillDeliveryEligible(String paymentRequestId, boolean includeFailed) {
        Map<String, Object> existing = existingDelivery(paymentRequestId);
        if (existing == null) return true;
        String status = string(existing.get("status"));
        if (!StringUtils.hasText(status)) return false;
        status = status.toUpperCase(Locale.ROOT);
        if ("SENT".equals(status)) return false;
        return includeFailed && ("FAILED".equals(status)
                || "MISSING_ATTACHMENT".equals(status)
                || "NOT_CONFIGURED".equals(status));
    }

    private Map<String, Object> deliver(String paymentRequestId, String triggeredBy, boolean allowRetry) {
        PaymentRequestEntity payment = paymentRequestRepository.findById(paymentRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Payment request not found: " + paymentRequestId));
        if (!isApprovedOrLater(payment.getStatus())) {
            return result(paymentRequestId, "NOT_APPROVED", "Payment request has not been approved");
        }

        Map<String, Object> config = getConfiguration();
        List<String> configuredTypes = configuredDocumentTypes(config.get("document_types"));
        List<AttachmentEntity> documents = findConfiguredAttachments(paymentRequestId, configuredTypes);
        // Eligibility is document-driven only. Request type is deliberately not used.
        if (documents.isEmpty()) {
            return result(paymentRequestId, "NOT_APPLICABLE",
                    "No attachment matches the configured payment request document types");
        }

        Map<String, Object> existing = existingDelivery(paymentRequestId);
        if (!allowRetry && existing != null && "SENT".equals(string(existing.get("status")))) {
            return result(paymentRequestId, "ALREADY_SENT", "Configured documents were already emailed");
        }

        if (!booleanValue(config.get("enabled")) || !StringUtils.hasText(string(config.get("recipient_emails")))) {
            record(payment, documents.isEmpty() ? null : documents.get(0), string(config.get("recipient_emails")),
                    "NOT_CONFIGURED", triggeredBy,
                    "Approved payment request document email is disabled or no recipient email is configured", allowRetry);
            return result(paymentRequestId, "NOT_CONFIGURED", "Email configuration is not enabled");
        }

        try {
            String recipients = string(config.get("recipient_emails"));
            EmailDto email = new EmailDto();
            email.setTo(recipients);
            email.setSubject(render(string(config.get("subject_template")), payment, configuredTypes));
            email.setTemplate("payment-request-invoice-approved");
            email.setProperties(List.of(
                    property("requestNo", payment.getRequestNo()),
                    property("payeeName", payment.getPayeeName()),
                    property("supplierName", payment.getPayeeName()),
                    property("documentTypes", String.join(", ", configuredTypes)),
                    property("invoiceNo", payment.getInvoiceNo()),
                    property("amount", payment.getAmount() == null ? "R 0.00" : "R " + payment.getAmount().setScale(2, RoundingMode.HALF_UP)),
                    property("paymentReason", payment.getPaymentReason()),
                    property("bodyMessage", string(config.get("body_message"))),
                    property("attachmentCount", String.valueOf(documents.size()))
            ));
            List<File> files = new ArrayList<>();
            for (int i = 0; i < documents.size(); i++) {
                AttachmentEntity document = documents.get(i);
                String extension = firstNonBlank(document.getExtension(), extensionFromContentType(document.getContentType()), "pdf")
                        .replace(".", "");
                File file = new File();
                file.setOwner(payment.getPayeeName());
                file.setName(attachmentFileName(document, payment, i, documents.size()));
                file.setType(extension);
                file.setContent(Base64.getEncoder().encodeToString(attachmentService.getBytes(document)));
                files.add(file);
            }
            email.setFiles(files);
            emailService.send(email);
            record(payment, documents.get(0), recipients, "SENT", triggeredBy, null, true);
            Map<String, Object> sent = result(paymentRequestId, "SENT", documents.size() + " document(s) emailed to " + recipients);
            sent.put("attachmentCount", documents.size());
            sent.put("documentTypes", configuredTypes);
            return sent;
        } catch (Exception ex) {
            log.error("Payment request document email delivery failed for payment request {}", paymentRequestId, ex);
            record(payment, documents.get(0), string(config.get("recipient_emails")), "FAILED", triggeredBy, rootMessage(ex), true);
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Unable to email approved payment request documents", ex);
        }
    }

    private List<AttachmentEntity> findConfiguredAttachments(String paymentRequestId, List<String> configuredTypes) {
        if (configuredTypes.isEmpty()) return List.of();
        return attachmentRepository.findByObjectId(paymentRequestId).stream()
                .filter(attachment -> configuredTypes.stream()
                        .anyMatch(type -> type.equalsIgnoreCase(firstNonBlank(attachment.getDocumentType(), ""))))
                .toList();
    }

    private void record(PaymentRequestEntity payment,
                        AttachmentEntity attachment,
                        String recipients,
                        String status,
                        String triggeredBy,
                        String error,
                        boolean updateExisting) {
        Map<String, Object> existing = existingDelivery(payment.getId());
        if (existing == null) {
            jdbcTemplate.update("""
                    INSERT INTO payment_request_invoice_email_delivery(
                        id, payment_request_id, attachment_id, recipient_emails, status,
                        attempt_count, last_attempt_at, sent_at, error_message, triggered_by
                    ) VALUES(?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP, ?, ?, ?)
                    """, UUID.randomUUID().toString(), payment.getId(), attachment == null ? null : attachment.getId(),
                    recipients, status, "SENT".equals(status) ? new Date() : null, truncate(error), actor(triggeredBy));
        } else if (updateExisting || !"SENT".equals(string(existing.get("status")))) {
            jdbcTemplate.update("""
                    UPDATE payment_request_invoice_email_delivery
                       SET attachment_id = ?, recipient_emails = ?, status = ?,
                           attempt_count = attempt_count + 1, last_attempt_at = CURRENT_TIMESTAMP,
                           sent_at = ?, error_message = ?, triggered_by = ?
                     WHERE payment_request_id = ?
                    """, attachment == null ? null : attachment.getId(), recipients, status,
                    "SENT".equals(status) ? new Date() : null, truncate(error), actor(triggeredBy), payment.getId());
        }
    }

    private Map<String, Object> existingDelivery(String paymentRequestId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM payment_request_invoice_email_delivery WHERE payment_request_id = ?", paymentRequestId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Map<String, Object> deliverySummary() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT status, COUNT(*) AS total
                  FROM payment_request_invoice_email_delivery
                 GROUP BY status
                 ORDER BY status
                """);
        Map<String, Object> summary = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            summary.put(string(row.get("status")), row.get("total"));
        }
        return summary;
    }

    private List<Map<String, Object>> recentFailures() {
        return jdbcTemplate.queryForList("""
                SELECT d.payment_request_id AS paymentRequestId,
                       pr.request_no AS requestNo,
                       d.attachment_id AS attachmentId,
                       d.recipient_emails AS recipientEmails,
                       d.attempt_count AS attemptCount,
                       d.last_attempt_at AS lastAttemptAt,
                       d.error_message AS errorMessage
                  FROM payment_request_invoice_email_delivery d
                  LEFT JOIN payment_request pr ON pr.id = d.payment_request_id
                 WHERE d.status = 'FAILED'
                 ORDER BY d.last_attempt_at DESC
                 LIMIT 20
                """);
    }

    private boolean isApprovedOrLater(PaymentRequestStatus status) {
        return status == PaymentRequestStatus.APPROVED
                || status == PaymentRequestStatus.QUEUED_FOR_PAYMENT
                || status == PaymentRequestStatus.PROCESSED
                || status == PaymentRequestStatus.PAID;
    }

    private Map<String, Object> result(String paymentRequestId, String status, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentRequestId", paymentRequestId);
        result.put("status", status);
        result.put("message", message);
        return result;
    }

    private PropertyDto property(String key, String value) {
        PropertyDto property = new PropertyDto();
        property.setKey(key);
        property.setValue(value == null ? "" : value);
        return property;
    }

    private String render(String template, PaymentRequestEntity payment, List<String> configuredTypes) {
        String value = firstNonBlank(template, "Approved supplier invoice payment request {{requestNo}}");
        boolean supplierInvoiceOnly = configuredTypes.size() == 1
                && DEFAULT_DOCUMENT_TYPE.equalsIgnoreCase(configuredTypes.get(0));
        if (!supplierInvoiceOnly
                && "Approved supplier invoice payment request {{requestNo}}".equals(value)) {
            value = "Approved payment request {{requestNo}}";
        }
        return value
                .replace("{{requestNo}}", firstNonBlank(payment.getRequestNo(), payment.getId()))
                .replace("{{invoiceNo}}", firstNonBlank(payment.getInvoiceNo(), ""))
                .replace("{{supplierName}}", firstNonBlank(payment.getPayeeName(), "Payee"))
                .replace("{{payeeName}}", firstNonBlank(payment.getPayeeName(), "Payee"))
                .replace("{{documentTypes}}", String.join(", ", configuredTypes));
    }

    private void validateRecipients(String recipients) {
        if (!StringUtils.hasText(recipients)) return;
        for (String address : recipients.split(";")) {
            if (!EMAIL.matcher(address.trim()).matches()) {
                throw new IllegalArgumentException("Invalid recipient email address: " + address);
            }
        }
    }

    private String normalizeRecipients(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        List<String> result = new ArrayList<>();
        for (String item : raw.split("[,;\\n\\r]+")) {
            String value = item.trim().toLowerCase(Locale.ROOT);
            if (StringUtils.hasText(value) && !result.contains(value)) result.add(value);
        }
        return String.join(";", result);
    }


    private List<String> configuredDocumentTypes(Object raw) {
        List<String> configured = normalizeDocumentTypes(raw);
        return configured.isEmpty() ? List.of(DEFAULT_DOCUMENT_TYPE) : configured;
    }

    private List<String> normalizeDocumentTypes(Object raw) {
        List<String> result = new ArrayList<>();
        if (raw instanceof Iterable<?> values) {
            for (Object value : values) addDocumentType(result, value);
        } else if (raw != null) {
            for (String value : raw.toString().split("[,;\n\r]+")) addDocumentType(result, value);
        }
        return result;
    }

    private void addDocumentType(List<String> result, Object raw) {
        String value = string(raw);
        if (!StringUtils.hasText(value)) return;
        String normalized = value.toUpperCase(Locale.ROOT);
        if (!result.contains(normalized)) result.add(normalized);
    }

    private void validateDocumentTypes(List<String> documentTypes) {
        if (documentTypes.isEmpty()) return;
        List<String> available = jdbcTemplate.queryForList("""
                SELECT UPPER(code)
                  FROM field_option
                 WHERE field = 'DOCUMENT-TYPE-PAYMENT-REQUEST'
                   AND (valid_from IS NULL OR valid_from <= CURRENT_DATE)
                   AND (valid_to IS NULL OR valid_to >= CURRENT_DATE)
                """, String.class);
        for (String documentType : documentTypes) {
            if (!available.contains(documentType)) {
                throw new IllegalArgumentException("Invalid payment request document type: " + documentType);
            }
        }
    }

    private String attachmentFileName(AttachmentEntity attachment, PaymentRequestEntity payment, int index, int total) {
        String base = safeFileName(firstNonBlank(attachment.getDocumentType(), DEFAULT_DOCUMENT_TYPE).toLowerCase(Locale.ROOT));
        String reference = safeFileName(firstNonBlank(payment.getInvoiceNo(), payment.getRequestNo(), payment.getId()));
        return total > 1 ? base + "-" + reference + "-" + (index + 1) : base + "-" + reference;
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean b) return b;
        String text = string(value);
        return "1".equals(text) || "true".equalsIgnoreCase(text) || "yes".equalsIgnoreCase(text);
    }

    private String text(Map<String, Object> values, String key) {
        return values == null ? null : string(values.get(key));
    }

    private String string(Object value) {
        return value == null ? null : value.toString().trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (StringUtils.hasText(value)) return value.trim();
        return null;
    }

    private String extensionFromContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) return null;
        if (contentType.contains("pdf")) return "pdf";
        if (contentType.contains("png")) return "png";
        if (contentType.contains("jpeg") || contentType.contains("jpg")) return "jpg";
        return null;
    }

    private String safeFileName(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]+", "-").replaceAll("-+", "-");
    }

    private String actor(String value) {
        return StringUtils.hasText(value) ? value.trim() : "system";
    }

    private String truncate(String value) {
        return value == null || value.length() <= 4000 ? value : value.substring(0, 4000);
    }

    private String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null) cursor = cursor.getCause();
        return firstNonBlank(cursor.getMessage(), cursor.getClass().getSimpleName());
    }
}
