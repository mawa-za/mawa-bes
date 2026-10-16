package za.co.mawa.bes.xero;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tenant-scoped read-only quarantine worklist and conservative reconciliation dispatch. */
@Service
public class XeroReconciliationOperationsService {
    private final JdbcTemplate jdbc;
    private final XeroInvoiceReconciliationService reconciliation;

    public XeroReconciliationOperationsService(JdbcTemplate jdbc,
                                               XeroInvoiceReconciliationService reconciliation) {
        this.jdbc = jdbc;
        this.reconciliation = reconciliation;
    }

    public List<Map<String, Object>> pending() {
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

    private void append(List<Map<String, Object>> result, String type, String sql) {
        jdbc.queryForList(sql).forEach(row -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", type);
            entry.putAll(row);
            result.add(entry);
        });
    }

    /** Never releases quarantine; remote reads only. */
    public Map<String, Object> check(String type, String id) throws IOException {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Transaction ID is required");
        boolean matched = switch (type == null ? "" : type) {
            case "INVOICE" -> reconciliation.reconcileInvoice(id);
            case "PAYMENT" -> reconciliation.reconcilePayment(id);
            case "CREDIT_NOTE" -> reconciliation.reconcileCreditNote(id);
            case "CREDIT_NOTE_ALLOCATION" -> reconciliation.reconcileCreditNoteAllocation(id);
            default -> throw new IllegalArgumentException("Unsupported Xero reconciliation type");
        };
        return Map.of("matched", matched, "quarantineRetained", true,
                "message", matched ? "Match linked; quarantine retained for operator review"
                        : "No new unique match linked; quarantine retained");
    }
}
