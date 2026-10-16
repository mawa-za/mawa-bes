package za.co.mawa.bes.xero;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.mawa.bes.configuration.context.UserContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Tenant-scoped read-only quarantine worklist and conservative reconciliation dispatch. */
@Service
public class XeroReconciliationOperationsService {
    private static final Logger log = LoggerFactory.getLogger(XeroReconciliationOperationsService.class);
    private final JdbcTemplate jdbc;
    private final XeroInvoiceReconciliationService reconciliation;

    public XeroReconciliationOperationsService(JdbcTemplate jdbc,
                                               XeroInvoiceReconciliationService reconciliation) {
        this.jdbc = jdbc;
        this.reconciliation = reconciliation;
    }

    public List<Map<String, Object>> pending() {
        requireAccountingAdministrator();
        List<Map<String, Object>> rows = new ArrayList<>();
        append(rows, "INVOICE", """
                SELECT id, invoice_no AS reference, integration_error AS error,
                       xero_invoice_id AS external_id
                  FROM invoice WHERE integration_status = 'RECONCILIATION_REQUIRED'
                  ORDER BY id DESC LIMIT 100
                """);
        append(rows, "PAYMENT", """
                SELECT p.id, p.reference_no AS reference, p.xero_sync_error AS error,
                       p.xero_payment_id AS external_id
                  FROM invoice_payment p WHERE p.xero_sync_status = 'RECONCILIATION_REQUIRED'
                  ORDER BY p.id DESC LIMIT 100
                """);
        append(rows, "CREDIT_NOTE", """
                SELECT id, credit_note_no AS reference, xero_sync_error AS error,
                       xero_credit_note_id AS external_id
                  FROM credit_note WHERE xero_sync_status = 'RECONCILIATION_REQUIRED'
                  ORDER BY id DESC LIMIT 100
                """);
        return rows;
    }

    /** Explicit Xero capability, never implied by broad workcentre access. */
    private void requireAccountingAdministrator() {
        String userId = UserContext.getCurrentUserId();
        if (userId == null || userId.isBlank()) {
            throw new AccessDeniedException("Xero reconciliation requires an authenticated ERP user");
        }
        Integer allowed = jdbc.queryForObject("""
                SELECT COUNT(*) FROM user_role ur
                JOIN `role` r ON r.id = ur.role
                JOIN `user` u ON u.id = ur.user
                JOIN xero_reconciliation_role_permission xp ON xp.role_id = r.id
                WHERE ur.user = ? AND xp.can_check = 1
                  AND u.status = 'ACTIVE'
                  AND (u.expires_at IS NULL OR u.expires_at > NOW())
                  AND (ur.valid_from IS NULL OR ur.valid_from <= CURRENT_DATE)
                  AND (ur.valid_to IS NULL OR ur.valid_to >= CURRENT_DATE)
                """, Integer.class, userId);
        if (allowed == null || allowed == 0) {
            log.warn("Denied Xero reconciliation access for ERP user {}", userId);
            throw new AccessDeniedException("Insufficient Xero reconciliation permission");
        }
    }

    private void audit(String auditId, String operator, String type, String transactionId,
                       String outcome, String detail) {
        jdbc.update("""
                INSERT INTO xero_reconciliation_audit
                    (id, operator_user_id, transaction_type, transaction_id, action,
                     outcome, detail, occurred_at)
                VALUES (?, ?, ?, ?, 'CHECK_XERO', ?, ?, CURRENT_TIMESTAMP(6))
                """, auditId, operator, type, transactionId, outcome, detail);
    }

    private void append(List<Map<String, Object>> result, String type, String sql) {
        jdbc.queryForList(sql).forEach(row -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", type);
            entry.putAll(row);
            result.add(entry);
        });
    }

    /** Tenant-scoped immutable operator history. This endpoint never changes financial state. */
    public List<Map<String, Object>> auditHistory(String type, String id) {
        requireAccountingAdministrator();
        validateTransaction(type, id);
        return jdbc.queryForList("""
                SELECT id, operator_user_id, transaction_type, transaction_id,
                       action, outcome, detail, occurred_at
                  FROM xero_reconciliation_audit
                 WHERE transaction_type = ? AND transaction_id = ?
                 ORDER BY occurred_at DESC, id DESC LIMIT 100
                """, type, id);
    }

    /** Local preflight only: never declares the external ledger balanced or releases quarantine. */
    public Map<String, Object> releaseReadiness(String type, String id) {
        requireAccountingAdministrator();
        validateTransaction(type, id);
        List<String> blockers = new ArrayList<>();
        blockers.add("Manual financial verification and audited release workflow are not implemented");
        if (!"INVOICE".equals(type)) {
            blockers.add("Release of " + type + " requires linked invoice and all related entries to be reviewed");
        } else {
            List<Map<String, Object>> invoices = jdbc.queryForList("""
                    SELECT integration_status, xero_invoice_id FROM invoice WHERE id = ?
                    """, id);
            if (invoices.isEmpty()) throw new IllegalArgumentException("Invoice not found");
            Map<String, Object> invoice = invoices.get(0);
            if (!"RECONCILIATION_REQUIRED".equals(String.valueOf(invoice.get("integration_status"))))
                blockers.add("Invoice is not in reconciliation quarantine");
            if (invoice.get("xero_invoice_id") == null || String.valueOf(invoice.get("xero_invoice_id")).isBlank())
                blockers.add("Xero invoice ID has not been recovered");
            // A count of quarantined records alone is insufficient: FAILED, SENDING,
            // missing external IDs and incomplete allocations also block release.
            List<Map<String, Object>> payments = jdbc.queryForList("""
                    SELECT id, status, xero_sync_status, xero_payment_id
                      FROM invoice_payment WHERE invoice_id = ?
                    """, id);
            for (Map<String, Object> payment : payments) {
                String paymentId = String.valueOf(payment.get("id"));
                String state = value(payment.get("xero_sync_status"));
                String externalId = value(payment.get("xero_payment_id"));
                boolean cancelled = "CANCELLED".equalsIgnoreCase(value(payment.get("status")));
                if (cancelled) {
                    if (externalId != null && !"DELETED".equalsIgnoreCase(state))
                        blockers.add("Payment " + paymentId + " cancellation not confirmed in Xero");
                    if ("RECONCILIATION_REQUIRED".equalsIgnoreCase(state) || "SENDING".equalsIgnoreCase(state))
                        blockers.add("Payment " + paymentId + " has an uncertain reversal outcome");
                } else if (externalId == null || !"POSTED".equalsIgnoreCase(state)) {
                    blockers.add("Payment " + paymentId + " is not confirmed POSTED with a Xero Payment ID"
                            + " (state: " + (state == null ? "UNSET" : state) + ")");
                }
            }
            List<Map<String, Object>> notes = jdbc.queryForList("""
                    SELECT id, status, xero_sync_status, xero_credit_note_id, xero_allocated_at
                      FROM credit_note WHERE invoice_id = ?
                    """, id);
            for (Map<String, Object> note : notes) {
                // Only issued notes are sent by the current Xero integration. Other
                // credit-note states require separate accounting policy review.
                if (!"ISSUED".equalsIgnoreCase(value(note.get("status")))) continue;
                String noteId = String.valueOf(note.get("id"));
                if (value(note.get("xero_credit_note_id")) == null
                        || note.get("xero_allocated_at") == null
                        || !"ALLOCATED".equalsIgnoreCase(value(note.get("xero_sync_status")))) {
                    blockers.add("Credit note " + noteId
                            + " is not confirmed ALLOCATED with Xero ID and allocation marker");
                }
            }
            blockers.add("External Xero ledger balance and transaction verification are not implemented");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("id", id);
        result.put("releaseAllowed", false);
        result.put("blockers", blockers);
        return result;
    }

    private static String value(Object field) {
        return field == null || field.toString().isBlank() ? null : field.toString();
    }

    private static void validateTransaction(String type, String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Transaction ID is required");
        if (!List.of("INVOICE", "PAYMENT", "CREDIT_NOTE", "CREDIT_NOTE_ALLOCATION").contains(type))
            throw new IllegalArgumentException("Unsupported Xero reconciliation type");
    }

    /** Never releases quarantine; remote reads only. Audit writes are mandatory. */
    public Map<String, Object> check(String type, String id) throws IOException {
        requireAccountingAdministrator();
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Transaction ID is required");
        if (!List.of("INVOICE", "PAYMENT", "CREDIT_NOTE", "CREDIT_NOTE_ALLOCATION").contains(type)) {
            throw new IllegalArgumentException("Unsupported Xero reconciliation type");
        }
        String operator = UserContext.getCurrentUserId();
        String auditId = UUID.randomUUID().toString();
        // An audit record must exist before contacting Xero; if this fails, no lookup occurs.
        audit(auditId, operator, type, id, "STARTED", null);
        try {
            boolean matched = switch (type) {
                case "INVOICE" -> reconciliation.reconcileInvoice(id);
                case "PAYMENT" -> reconciliation.reconcilePayment(id);
                case "CREDIT_NOTE" -> reconciliation.reconcileCreditNote(id);
                case "CREDIT_NOTE_ALLOCATION" -> reconciliation.reconcileCreditNoteAllocation(id);
                default -> throw new IllegalArgumentException("Unsupported Xero reconciliation type");
            };
            jdbc.update("""
                    UPDATE xero_reconciliation_audit SET outcome = ?, detail = ? WHERE id = ?
                    """, matched ? "MATCHED" : "NO_UNIQUE_MATCH",
                    "Quarantine retained; no transaction replay", auditId);
            return Map.of("matched", matched, "quarantineRetained", true,
                    "auditId", auditId,
                    "message", matched ? "Match linked; quarantine retained for operator review"
                            : "No new unique match linked; quarantine retained");
        } catch (IOException | RuntimeException ex) {
            try {
                jdbc.update("""
                        UPDATE xero_reconciliation_audit SET outcome = 'ERROR', detail = ? WHERE id = ?
                        """, ex.getClass().getSimpleName(), auditId);
            } catch (RuntimeException auditError) {
                log.error("Failed to record Xero reconciliation outcome for audit {}", auditId, auditError);
                ex.addSuppressed(auditError);
            }
            throw ex;
        }
    }
}
