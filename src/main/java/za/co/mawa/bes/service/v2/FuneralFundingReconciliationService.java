package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Reconciles the family portion of an already-invoiced funeral when funeral
 * funding is approved after invoice generation.
 *
 * <p>Untouched family invoices are resized in place. Once an invoice has
 * payments, credit notes or accounting-integration activity, the original
 * invoice is preserved and an auditable credit note is issued instead.</p>
 */
@Service
@RequiredArgsConstructor
public class FuneralFundingReconciliationService {
    private static final Set<String> XERO_ACTIVE_STATUSES = Set.of(
            "QUEUED", "SENDING", "POSTED", "UPDATE_QUEUED", "SENT");

    private final JdbcTemplate jdbc;

    @Transactional
    public void reconcileProviderFamilyBalance(String providerTenantId,
                                               String funeralServiceId,
                                               String actor) {
        if (isBlank(providerTenantId) || isBlank(funeralServiceId)) return;

        String funeralService = qualified(providerTenantId, "funeral_service");
        String funeralServiceInvoice = qualified(providerTenantId, "funeral_service_invoice");
        String invoice = qualified(providerTenantId, "invoice");

        List<Map<String, Object>> services = jdbc.queryForList(
                "SELECT service_request_no,total_amount_cents,status FROM " + funeralService + " WHERE id=?",
                funeralServiceId);
        if (services.isEmpty()) return;
        Map<String, Object> service = services.get(0);
        long funeralTotal = number(service.get("total_amount_cents"));
        if (funeralTotal <= 0) return;

        Long allocated = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount_cents),0) FROM " + funeralServiceInvoice
                        + " WHERE funeral_service_id=? AND entity_type IN ('BURIAL_SOCIETY','GROUP_SOCIETY')",
                Long.class,
                funeralServiceId);
        long targetFamilyAmount = Math.max(0L, funeralTotal - Math.min(funeralTotal, value(allocated)));

        List<Map<String, Object>> familyInvoices = jdbc.queryForList("""
                SELECT fsi.id AS link_id,fsi.invoice_id,fsi.amount_cents AS link_amount_cents,
                       fsi.payment_request_id,
                       i.invoice_no,i.partner_id,i.total_cents,i.paid_cents,i.credited_cents,
                       i.balance_cents,i.currency,i.xero_invoice_id,i.integration_status
                  FROM %s fsi
                  JOIN %s i ON i.id=fsi.invoice_id
                 WHERE fsi.funeral_service_id=? AND fsi.entity_type='FAMILY_REP'
                 ORDER BY fsi.created_at,i.invoice_no
                """.formatted(funeralServiceInvoice, invoice), funeralServiceId);
        if (familyInvoices.isEmpty()) return;

        long remainingTarget = targetFamilyAmount;
        for (Map<String, Object> row : familyInvoices) {
            String invoiceId = Objects.toString(row.get("invoice_id"), null);
            if (isBlank(invoiceId)) continue;

            long currentNet = Math.max(0L,
                    number(row.get("total_cents")) - number(row.get("credited_cents")));
            long desiredNet = Math.min(currentNet, remainingTarget);
            remainingTarget = Math.max(0L, remainingTarget - desiredNet);

            if (currentNet > desiredNet) {
                long reduction = currentNet - desiredNet;
                if (hasIndependentActivity(providerTenantId, row)) {
                    issueCredit(providerTenantId, row, reduction, funeralServiceId, actor);
                } else {
                    resizeUntouchedInvoice(providerTenantId, row, desiredNet, actor);
                }
            }

            jdbc.update("UPDATE " + funeralServiceInvoice + " SET amount_cents=? WHERE id=?",
                    desiredNet, row.get("link_id"));
        }
    }

    private boolean hasIndependentActivity(String tenant, Map<String, Object> invoice) {
        if (!isBlank(Objects.toString(invoice.get("payment_request_id"), null))) return true;
        if (number(invoice.get("credited_cents")) > 0) return true;
        if (number(invoice.get("paid_cents")) > 0) return true;
        if (!isBlank(Objects.toString(invoice.get("xero_invoice_id"), null))) return true;
        String integrationStatus = Objects.toString(invoice.get("integration_status"), "")
                .trim().toUpperCase(Locale.ROOT);
        if (XERO_ACTIVE_STATUSES.contains(integrationStatus)) return true;
        Long paymentCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + qualified(tenant, "invoice_payment") + " WHERE invoice_id=?",
                Long.class,
                invoice.get("invoice_id"));
        return value(paymentCount) > 0;
    }

    private void resizeUntouchedInvoice(String tenant,
                                        Map<String, Object> invoice,
                                        long newTotalCents,
                                        String actor) {
        String invoiceId = Objects.toString(invoice.get("invoice_id"), null);
        String invoiceTable = qualified(tenant, "invoice");
        long paid = number(invoice.get("paid_cents"));
        long balance = Math.max(0L, newTotalCents - paid);
        String status = paid <= 0 ? "ISSUED" : (balance == 0 ? "PAID" : "PARTIALLY_PAID");

        resizeInvoiceLines(tenant, invoiceId, newTotalCents);
        jdbc.update("""
                UPDATE %s
                   SET subtotal_cents=?,tax_cents=0,discount_cents=0,total_cents=?,
                       balance_cents=?,status=?,updated_by=?,updated_at=CURRENT_TIMESTAMP
                 WHERE id=?
                """.formatted(invoiceTable),
                newTotalCents, newTotalCents, balance, status, effectiveActor(actor), invoiceId);
    }

    private void resizeInvoiceLines(String tenant, String invoiceId, long newTotalCents) {
        String lineTable = qualified(tenant, "invoice_line");
        List<Map<String, Object>> lines = jdbc.queryForList(
                "SELECT id,quantity,total_cents FROM " + lineTable
                        + " WHERE invoice_id=? AND COALESCE(show_amount,1)=1 ORDER BY created_at,id",
                invoiceId);
        if (lines.isEmpty()) return;

        long sourceTotal = lines.stream().mapToLong(line -> Math.max(0L, number(line.get("total_cents")))).sum();
        long allocated = 0L;
        for (int index = 0; index < lines.size(); index++) {
            Map<String, Object> line = lines.get(index);
            long amount;
            if (index == lines.size() - 1) {
                amount = Math.max(0L, newTotalCents - allocated);
            } else if (sourceTotal <= 0) {
                amount = 0L;
            } else {
                amount = Math.round((double) newTotalCents * Math.max(0L, number(line.get("total_cents"))) / sourceTotal);
                amount = Math.min(amount, Math.max(0L, newTotalCents - allocated));
            }
            allocated += amount;
            double quantity = line.get("quantity") instanceof Number number ? number.doubleValue() : 1.0;
            long unitPrice = quantity > 0 ? Math.round(amount / quantity) : amount;
            jdbc.update("""
                    UPDATE %s
                       SET unit_price_cents=?,discount_cents=0,tax_cents=0,subtotal_cents=?,total_cents=?
                     WHERE id=?
                    """.formatted(lineTable), unitPrice, amount, amount, line.get("id"));
        }
    }

    private void issueCredit(String tenant,
                             Map<String, Object> invoice,
                             long amountCents,
                             String funeralServiceId,
                             String actor) {
        if (amountCents <= 0) return;
        String invoiceId = Objects.toString(invoice.get("invoice_id"), null);
        long total = number(invoice.get("total_cents"));
        long existingCredit = number(invoice.get("credited_cents"));
        long available = Math.max(0L, total - existingCredit);
        long amount = Math.min(amountCents, available);
        if (amount <= 0) return;

        String creditId = UUID.randomUUID().toString();
        String creditNo = "CN-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        String reason = "Late funeral funding approved for service " + funeralServiceId;

        jdbc.update("INSERT INTO " + qualified(tenant, "credit_note")
                        + "(id,credit_note_no,invoice_id,partner_id,credit_note_date,reason,"
                        + "subtotal_cents,tax_cents,total_cents,currency,status,created_by,created_at)"
                        + " VALUES(?,?,?,?,?,?,?,0,?,?,'ISSUED',?,CURRENT_TIMESTAMP)",
                creditId, creditNo, invoiceId, invoice.get("partner_id"), LocalDate.now(), reason,
                amount, amount, defaultString(invoice.get("currency"), "ZAR"), effectiveActor(actor));
        jdbc.update("INSERT INTO " + qualified(tenant, "credit_note_line")
                        + "(id,credit_note_id,description,quantity,unit_price_cents,tax_cents,subtotal_cents,total_cents,created_at)"
                        + " VALUES(?,?,?,1,?,0,?,?,CURRENT_TIMESTAMP)",
                UUID.randomUUID().toString(), creditId,
                "Credit against " + defaultString(invoice.get("invoice_no"), invoiceId),
                amount, amount, amount);

        long credited = existingCredit + amount;
        long paid = number(invoice.get("paid_cents"));
        long balance = Math.max(0L, total - paid - credited);
        String status = balance == 0 ? "CREDITED" : "PARTIALLY_CREDITED";
        jdbc.update("UPDATE " + qualified(tenant, "invoice")
                        + " SET credited_cents=?,balance_cents=?,status=?,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                credited, balance, status, effectiveActor(actor), invoiceId);

        if (!isBlank(Objects.toString(invoice.get("xero_invoice_id"), null))
                || XERO_ACTIVE_STATUSES.contains(Objects.toString(invoice.get("integration_status"), "")
                .trim().toUpperCase(Locale.ROOT))) {
            queueXeroInvoice(tenant, invoiceId, Objects.toString(invoice.get("invoice_no"), invoiceId));
        }
    }

    private void queueXeroInvoice(String tenant, String invoiceId, String invoiceNo) {
        String queue = qualified(tenant, "message_queue");
        jdbc.update("INSERT INTO " + queue
                        + "(message_type,reference_id,reference_no,payload,processed,retry_count,next_attempt_at,created_at)"
                        + " VALUES('XERO-INVOICE',?,?,?,b'0',0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"
                        + " ON DUPLICATE KEY UPDATE reference_no=VALUES(reference_no),payload=VALUES(payload),"
                        + " processed=b'0',retry_count=0,next_attempt_at=CURRENT_TIMESTAMP",
                invoiceId, invoiceNo, invoiceId);
        jdbc.update("UPDATE " + qualified(tenant, "invoice")
                        + " SET integration_status=CASE WHEN xero_invoice_id IS NULL OR xero_invoice_id=''"
                        + " THEN 'QUEUED' ELSE 'UPDATE_QUEUED' END,integration_error=NULL WHERE id=?",
                invoiceId);
    }

    private String qualified(String tenant, String table) {
        if (tenant == null || !tenant.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new IllegalArgumentException("Invalid provider tenant");
        }
        if (table == null || !table.matches("[A-Za-z0-9_]{1,128}")) {
            throw new IllegalArgumentException("Invalid table name");
        }
        return "`" + tenant + "`.`" + table + "`";
    }

    private long number(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value == null || value.toString().isBlank()) return 0L;
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException ignored) { return 0L; }
    }

    private long value(Long value) { return value == null ? 0L : value; }
    private String effectiveActor(String actor) { return isBlank(actor) ? "SYSTEM" : actor.trim(); }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
    private String defaultString(Object value, String fallback) {
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }
}
