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
import za.co.mawa.bes.enums.PaymentRequestType;
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
                    INSERT INTO payment_request_invoice_email_configuration(id, enabled, subject_template)
                    VALUES(?, 0, 'Approved supplier invoice payment request {{requestNo}}')
                    """, CONFIG_ID);
            rows = jdbcTemplate.queryForList(
                    "SELECT * FROM payment_request_invoice_email_configuration WHERE id = ?", CONFIG_ID);
        }
        Map<String, Object> response = new LinkedHashMap<>(rows.get(0));
        response.put("deliverySummary", deliverySummary());
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
        jdbcTemplate.update("""
                INSERT INTO payment_request_invoice_email_configuration(
                    id, enabled, recipient_emails, subject_template, body_message, created_by, updated_by
                ) VALUES(?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    enabled = VALUES(enabled),
                    recipient_emails = VALUES(recipient_emails),
                    subject_template = VALUES(subject_template),
                    body_message = VALUES(body_message),
                    updated_by = VALUES(updated_by)
                """, CONFIG_ID, enabled, recipients, subject, body, actor(userId), actor(userId));
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
            log.error("Approved supplier invoice email failed for payment request {}", paymentRequestId, ex);
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
        String sql = """
                SELECT pr.id
                  FROM payment_request pr
                  LEFT JOIN payment_request_invoice_email_delivery d
                    ON d.payment_request_id = pr.id
                 WHERE pr.request_type = 'SUPPLIER_INVOICE'
                   AND pr.status IN ('APPROVED', 'QUEUED_FOR_PAYMENT', 'PROCESSED', 'PAID')
                   AND (d.id IS NULL OR (? = 1 AND d.status IN ('FAILED', 'MISSING_ATTACHMENT', 'NOT_CONFIGURED')))
                 ORDER BY COALESCE(pr.approved_at, pr.created_at), pr.id
                 LIMIT ?
                """;
        List<String> ids = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getString(1),
                includeFailed ? 1 : 0,
                limit
        );
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
        response.put("results", results);
        response.put("deliverySummary", deliverySummary());
        return response;
    }

    private Map<String, Object> deliver(String paymentRequestId, String triggeredBy, boolean allowRetry) {
        PaymentRequestEntity payment = paymentRequestRepository.findById(paymentRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Payment request not found: " + paymentRequestId));
        if (payment.getRequestType() != PaymentRequestType.SUPPLIER_INVOICE) {
            return result(paymentRequestId, "NOT_APPLICABLE", "Only Supplier Invoice payment requests are emailed");
        }
        if (!isApprovedOrLater(payment.getStatus())) {
            return result(paymentRequestId, "NOT_APPROVED", "Payment request has not been approved");
        }

        Map<String, Object> config = getConfiguration();
        if (!booleanValue(config.get("enabled")) || !StringUtils.hasText(string(config.get("recipient_emails")))) {
            record(payment, null, string(config.get("recipient_emails")), "NOT_CONFIGURED", triggeredBy,
                    "Approved invoice email is disabled or no recipient email is configured", allowRetry);
            return result(paymentRequestId, "NOT_CONFIGURED", "Email configuration is not enabled");
        }

        Map<String, Object> existing = existingDelivery(paymentRequestId);
        if (!allowRetry && existing != null && "SENT".equals(string(existing.get("status")))) {
            return result(paymentRequestId, "ALREADY_SENT", "Invoice was already emailed");
        }

        AttachmentEntity invoice = findInvoiceAttachment(paymentRequestId);
        if (invoice == null) {
            record(payment, null, string(config.get("recipient_emails")), "MISSING_ATTACHMENT", triggeredBy,
                    "No supplier invoice attachment was found", allowRetry);
            return result(paymentRequestId, "MISSING_ATTACHMENT", "Attach a supplier invoice before retrying");
        }

        try {
            String recipients = string(config.get("recipient_emails"));
            EmailDto email = new EmailDto();
            email.setTo(recipients);
            email.setSubject(render(string(config.get("subject_template")), payment));
            email.setTemplate("payment-request-invoice-approved");
            email.setProperties(List.of(
                    property("requestNo", payment.getRequestNo()),
                    property("supplierName", payment.getPayeeName()),
                    property("invoiceNo", payment.getInvoiceNo()),
                    property("amount", payment.getAmount() == null ? "R 0.00" : "R " + payment.getAmount().setScale(2, RoundingMode.HALF_UP)),
                    property("paymentReason", payment.getPaymentReason()),
                    property("bodyMessage", string(config.get("body_message")))
            ));
            String extension = firstNonBlank(invoice.getExtension(), extensionFromContentType(invoice.getContentType()), "pdf");
            File file = new File();
            file.setOwner(payment.getPayeeName());
            file.setName(safeFileName("supplier-invoice-" + firstNonBlank(payment.getInvoiceNo(), payment.getRequestNo(), payment.getId())));
            file.setType(extension.replace(".", ""));
            file.setContent(Base64.getEncoder().encodeToString(attachmentService.getBytes(invoice)));
            email.setFiles(List.of(file));
            emailService.send(email);
            record(payment, invoice, recipients, "SENT", triggeredBy, null, true);
            return result(paymentRequestId, "SENT", "Invoice emailed to " + recipients);
        } catch (Exception ex) {
            record(payment, invoice, string(config.get("recipient_emails")), "FAILED", triggeredBy, rootMessage(ex), true);
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Unable to email approved supplier invoice", ex);
        }
    }

    private AttachmentEntity findInvoiceAttachment(String paymentRequestId) {
        List<AttachmentEntity> attachments = attachmentRepository.findByObjectId(paymentRequestId);
        return attachments.stream()
                .filter(a -> containsIgnoreCase(a.getDocumentType(), "INVOICE"))
                .findFirst()
                .orElseGet(() -> attachments.size() == 1 ? attachments.get(0) : null);
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

    private String render(String template, PaymentRequestEntity payment) {
        String value = firstNonBlank(template, "Approved supplier invoice payment request {{requestNo}}");
        return value
                .replace("{{requestNo}}", firstNonBlank(payment.getRequestNo(), payment.getId()))
                .replace("{{invoiceNo}}", firstNonBlank(payment.getInvoiceNo(), ""))
                .replace("{{supplierName}}", firstNonBlank(payment.getPayeeName(), "Supplier"));
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

    private boolean containsIgnoreCase(String value, String fragment) {
        return value != null && value.toUpperCase(Locale.ROOT).contains(fragment.toUpperCase(Locale.ROOT));
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
