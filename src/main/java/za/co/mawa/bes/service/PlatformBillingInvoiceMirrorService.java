package za.co.mawa.bes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.entity.PlatformBillingInvoiceMirrorEntity;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoiceLineEntity;
import za.co.mawa.bes.repository.PartnerRepository;
import java.time.LocalDate;
import za.co.mawa.bes.repository.PlatformBillingInvoiceMirrorRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A tenant-local, idempotent financial staging ledger. These are platform billing
 * source invoices, NOT user-issued ERP invoices. Payment and void/credit treatment
 * are explicit reconciliation states to prevent false Xero accounting entries.
 */
@Service
@RequiredArgsConstructor
public class PlatformBillingInvoiceMirrorService {
    private static final Set<String> ACCOUNTABLE = Set.of("ISSUED", "OVERDUE", "PAID", "VOID");
    private final PlatformBillingInvoiceMirrorRepository repository;
    private final TenantService tenantService;
    private final UserAccessService accessService;
    private final ObjectMapper mapper;
    @Value("${mawa.platform-operator.tenant-id:}")
    private String configuredOperatorTenantId;
    private final PartnerRepository partners;
    private final InvoiceService invoiceService;

    public void requireOperatorTenant() {
        String tenant = TenantContext.getCurrentTenant();
        if (!StringUtils.hasText(configuredOperatorTenantId)
                || !configuredOperatorTenantId.equals(tenant)
                || !"PLATFORM_OPERATOR".equals(
                    tenantService.getTenantProperties(tenant).getProperty("TENANT.TYPE"))) {
            throw new SecurityException("This endpoint is restricted to the platform operator tenant");
        }
    }

    @Transactional
    public Map<String, Object> accept(JsonNode invoice) {
        requireOperatorTenant();
        if (invoice == null || !invoice.isObject()) throw new IllegalArgumentException("Invoice payload is required");
        long id = invoice.path("id").asLong(-1);
        if (id <= 0) throw new IllegalArgumentException("Source invoice ID must be positive");
        String customer = requireText(invoice, "tenantId");
        String number = requireText(invoice, "invoiceNumber");
        String status = requireText(invoice, "status").toUpperCase(java.util.Locale.ROOT);
        if (!ACCOUNTABLE.contains(status)) throw new IllegalArgumentException("Only issued, overdue, paid or void invoices can be imported");
        String currency = requireText(invoice, "currency");
        if (currency.length() != 3) throw new IllegalArgumentException("Invalid currency");
        long subtotal = cents(invoice, "subtotal");
        long tax = cents(invoice, "taxAmount");
        long total = cents(invoice, "totalAmount");
        if (Math.addExact(subtotal, tax) != total || total < 0) {
            throw new IllegalArgumentException("Source invoice totals do not reconcile");
        }
        if (!invoice.path("lines").isArray() || invoice.path("lines").isEmpty()) {
            throw new IllegalArgumentException("Source invoice lines are required");
        }
        long lineNet = 0, lineTax = 0, lineTotal = 0;
        for (JsonNode line : invoice.path("lines")) {
            lineNet = Math.addExact(lineNet, cents(line, "netAmount"));
            lineTax = Math.addExact(lineTax, cents(line, "taxAmount"));
            lineTotal = Math.addExact(lineTotal, cents(line, "totalAmount"));
        }
        if (lineNet != subtotal || lineTax != tax || lineTotal != total) {
            throw new IllegalArgumentException("Source invoice lines do not reconcile with its totals");
        }
        String snapshot = invoice.toString();
        PlatformBillingInvoiceMirrorEntity mirror = repository.findById(id)
                .orElseGet(PlatformBillingInvoiceMirrorEntity::new);
        boolean previouslyPosted = StringUtils.hasText(mirror.getErpInvoiceId());
        boolean amountChanged = mirror.getSourceInvoiceId() != null
                && (mirror.getSubtotalCents() != subtotal || mirror.getTaxCents() != tax
                    || mirror.getTotalCents() != total || !currency.equals(mirror.getCurrency()));
        if (mirror.getSourceInvoiceId() != null) {
            if (!mirror.getSourceTenantId().equals(customer) || !mirror.getSourceInvoiceNumber().equals(number)) {
                throw new IllegalStateException("Source invoice identity conflict: " + id);
            }
            if (snapshot.equals(mirror.getSnapshotJson())) {
                return Map.of("sourceInvoiceId", id, "state", mirror.getAccountingState(), "changed", false);
            }
        }
        mirror.setSourceInvoiceId(id);
        mirror.setSourceTenantId(customer);
        mirror.setSourceInvoiceNumber(number);
        mirror.setSourceStatus(status);
        mirror.setCurrency(currency);
        mirror.setSubtotalCents(subtotal);
        mirror.setTaxCents(tax);
        mirror.setTotalCents(total);
        mirror.setSnapshotJson(snapshot);
        // Never manufacture customer payments or void/credit notes from a status flag.
        mirror.setAccountingState(switch (status) {
            case "PAID" -> "PAYMENT_RECONCILIATION_REQUIRED";
            case "VOID" -> "CREDIT_RECONCILIATION_REQUIRED";
            default -> amountChanged && previouslyPosted ? "ADJUSTMENT_RECONCILIATION_REQUIRED"
                    : previouslyPosted ? "ERP_INVOICE_POSTED" : "READY_FOR_ACCOUNTING";
        });
        mirror.setLastError(null);
        mirror.setSyncedAt(LocalDateTime.now());
        mirror.setUpdatedAt(LocalDateTime.now());
        repository.save(mirror);
        return Map.of("sourceInvoiceId", id, "state", mirror.getAccountingState(), "changed", true);
    }

    /**
     * Explicit finance-authorised posting into the standard ERP invoice/Xero pipeline.
     * Source data stays immutable in the mirror. Never book source PAID/VOID here.
     * The posting is serialised per source invoice and may only be performed once.
     */
    @Transactional
    public Map<String, Object> postToErp(long sourceId, String partnerId, String xeroTaxType) {
        requireOperatorTenant();
        accessService.assertPermission("platform-billing-revenue-post", null);
        accessService.assertExternalTransactionAllowed("PLATFORM-BILLING-INVOICE");
        PlatformBillingInvoiceMirrorEntity row = repository.lockById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Billing invoice has not been synchronised"));
        if (StringUtils.hasText(row.getErpInvoiceId())) {
            return Map.of("sourceInvoiceId", sourceId, "erpInvoiceId", row.getErpInvoiceId(), "created", false);
        }
        if ("VOID".equals(row.getSourceStatus())) {
            throw new IllegalStateException("Voided billing invoice must not be newly posted to ERP");
        }
        if (!StringUtils.hasText(partnerId) || !partners.existsById(partnerId)) {
            throw new IllegalArgumentException("A valid operator-ERP customer partner must be selected");
        }
        try {
            JsonNode source = mapper.readTree(row.getSnapshotJson());
            if (row.getTaxCents() > 0 && !StringUtils.hasText(xeroTaxType)) {
                throw new IllegalArgumentException("A reviewed Xero output tax type is required for taxable billing invoices");
            }
            InvoiceEntity invoice = new InvoiceEntity();
            invoice.setSourceType("PLATFORM_BILLING");
            invoice.setSourceId(Long.toString(sourceId));
            invoice.setExternalRef(row.getSourceInvoiceNumber());
            invoice.setPartnerId(partnerId);
            String dateText = source.hasNonNull("issuedAt")
                    ? source.get("issuedAt").asText() : source.path("periodEnd").asText();
            if (dateText.length() < 10) throw new IllegalStateException("Source invoice has no posting date");
            invoice.setInvoiceDate(LocalDate.parse(dateText.substring(0, 10)));
            if (source.hasNonNull("dueDate")) invoice.setDueDate(LocalDate.parse(source.get("dueDate").asText().substring(0, 10)));
            invoice.setStatus("ISSUED");
            invoice.setCurrency(row.getCurrency());
            invoice.setSubtotalCents(row.getSubtotalCents());
            invoice.setTaxCents(row.getTaxCents());
            invoice.setTotalCents(row.getTotalCents());
            invoice.setPaidCents(0L); // Never infer a payment without bank verification.
            invoice.setCreditedCents(0L);
            invoice.setBalanceCents(row.getTotalCents());
            invoice.setVatPricingMethodSnapshot("EXCLUSIVE");
            invoice.setVatRegisteredSnapshot(row.getTaxCents() > 0);
            invoice.setVatPolicyCapturedAt(LocalDateTime.now());
            invoice.setNotes("MAWA SaaS billing invoice " + row.getSourceInvoiceNumber()
                    + "; customer tenant " + row.getSourceTenantId()
                    + "; accounting source invoice ID " + sourceId
                    + ". Source is authoritative; reconcile payments/credits separately.");
            java.util.List<InvoiceLineEntity> lines = new java.util.ArrayList<>();
            for (JsonNode sourceLine : source.path("lines")) {
                InvoiceLineEntity line = new InvoiceLineEntity();
                String originalQty = sourceLine.path("quantity").asText("1");
                line.setDescription(sourceLine.path("description").asText("Subscription")
                        + " [billing qty " + originalQty + "]");
                // Usage-unit rates can contain fractions of a cent. Preserve the exact
                // billed line amount, not a rounded per-unit rate.
                line.setQuantity(1.0);
                line.setShowAmount(true);
                line.setUnitPriceCents(cents(sourceLine, "netAmount"));
                line.setSubtotalCents(cents(sourceLine, "netAmount"));
                line.setTaxCents(cents(sourceLine, "taxAmount"));
                line.setTotalCents(cents(sourceLine, "totalAmount"));
                line.setDiscountCents(0L);
                line.setVatRatePercentSnapshot(sourceLine.path("taxRate").decimalValue());
                line.setXeroTaxTypeSnapshot(StringUtils.hasText(xeroTaxType) ? xeroTaxType.trim() : "NONE");
                lines.add(line);
            }
            invoice.setLines(lines);
            InvoiceEntity saved = invoiceService.createInvoice(invoice);
            row.setErpInvoiceId(saved.getId());
            row.setAccountingState("PAID".equals(row.getSourceStatus())
                    ? "PAYMENT_RECONCILIATION_REQUIRED" : "ERP_INVOICE_POSTED");
            row.setLastError(null);
            row.setUpdatedAt(LocalDateTime.now());
            repository.save(row);
            return Map.of("sourceInvoiceId", sourceId, "erpInvoiceId", saved.getId(), "created", true);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Billing source payload could not be decoded", ex);
        }
    }

    public List<PlatformBillingInvoiceMirrorEntity> revenue() {
        requireOperatorTenant();
        if (!accessService.hasWorkcentreAccess("platform-billing-revenue", null)) {
            throw new SecurityException("Platform billing revenue access denied");
        }
        return repository.findTop500ByOrderByUpdatedAtDesc();
    }

    private static String requireText(JsonNode node, String field) {
        String value = node.path(field).asText("").trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Missing invoice " + field);
        return value;
    }

    private static long cents(JsonNode node, String field) {
        if (!node.hasNonNull(field)) throw new IllegalArgumentException("Missing invoice " + field);
        return node.get(field).decimalValue().movePointRight(2).longValueExact();
    }
}
