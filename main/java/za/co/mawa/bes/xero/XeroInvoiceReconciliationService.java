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

    /** Read-only Xero ledger snapshot. Values are observational, not an approval to release. */
    public Map<String, Object> inspectInvoiceLedger(String invoiceId) throws IOException {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        if (!"RECONCILIATION_REQUIRED".equalsIgnoreCase(invoice.getIntegrationStatus()))
            throw new IllegalStateException("Invoice must remain in reconciliation quarantine");
        String externalId = invoice.getXeroInvoiceId();
        if (externalId == null || externalId.isBlank())
            throw new IllegalStateException("Xero invoice identifier is missing; run Check Xero first");
        String config = auth.checkXeroInfo();
        if (config == null || config.isBlank()) throw new IOException("Xero is not connected");
        String base = auth.getXeroProperty(config, "XERO-BASE-URL", DEFAULT_URL).replaceAll("/+$", "");
        JsonNode body = getXero(base + "/" + URLEncoder.encode(externalId, StandardCharsets.UTF_8));
        JsonNode invoices = body.path("Invoices");
        if (!invoices.isArray() || invoices.size() != 1 ||
                !externalId.equals(invoices.get(0).path("InvoiceID").asText(null)))
            throw new IllegalStateException("Xero did not return exactly the requested invoice");
        JsonNode remote = invoices.get(0);
        // Missing fields are reported, never treated as zero.
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("invoiceId", invoiceId);
        result.put("xeroInvoiceId", externalId);
        result.put("mawaInvoiceNumber", invoice.getInvoiceNo());
        result.put("xeroInvoiceNumber", remote.path("InvoiceNumber").asText(null));
        result.put("mawaCurrency", invoice.getCurrency());
        result.put("xeroCurrency", remote.path("CurrencyCode").asText(null));
        result.put("mawaTotalCents", invoice.getTotalCents());
        for (String field : java.util.List.of("Total", "AmountPaid", "AmountCredited", "AmountDue")) {
            result.put("xero" + field, remote.path(field).isNumber() ? remote.path(field).decimalValue() : null);
        }
        result.put("xeroStatus", remote.path("Status").asText(null));
        result.put("xeroBalanceArithmetic", verifyInvoiceBalanceArithmetic(remote));
        boolean identityMatches = java.util.Objects.equals(invoice.getInvoiceNo(), remote.path("InvoiceNumber").asText(null))
                && invoice.getCurrency() != null
                && invoice.getCurrency().equalsIgnoreCase(remote.path("CurrencyCode").asText(""))
                && invoice.getTotalCents() != null && remote.path("Total").isNumber()
                && BigDecimal.valueOf(invoice.getTotalCents(), 2).compareTo(remote.path("Total").decimalValue()) == 0;
        result.put("invoiceIdentityMatches", identityMatches);
        // Read-only, evidence-oriented comparison. Missing remote arrays are UNKNOWN,
        // not evidence that Xero contains no payments or credits.
        java.util.List<Map<String, Object>> localPayments = new java.util.ArrayList<>();
        java.util.List<InvoicePaymentEntity> payments = paymentRepository.findByInvoiceId(invoiceId);
        JsonNode remotePayments = remote.path("Payments");
        boolean paymentsObservable = remotePayments.isArray();
        for (InvoicePaymentEntity payment : payments) {
            Map<String, Object> detail = new java.util.LinkedHashMap<>();
            detail.put("mawaPaymentId", payment.getId());
            detail.put("xeroPaymentId", payment.getXeroPaymentId());
            detail.put("mawaAmountCents", payment.getAmountCents());
            detail.put("mawaStatus", payment.getStatus());
            detail.put("xeroSyncStatus", payment.getXeroSyncStatus());
            java.util.List<JsonNode> matches = new java.util.ArrayList<>();
            if (paymentsObservable && payment.getXeroPaymentId() != null && !payment.getXeroPaymentId().isBlank()) {
                for (JsonNode candidate : remotePayments) {
                    if (payment.getXeroPaymentId().equals(candidate.path("PaymentID").asText(null))) matches.add(candidate);
                }
            }
            detail.put("matchCount", paymentsObservable ? matches.size() : null);
            detail.put("xeroAmount", matches.size() == 1 && matches.get(0).path("Amount").isNumber()
                    ? matches.get(0).path("Amount").decimalValue() : null);
            detail.put("amountMatches", matches.size() == 1 && payment.getAmountCents() != null
                    && matches.get(0).path("Amount").isNumber()
                    && BigDecimal.valueOf(payment.getAmountCents(), 2).compareTo(matches.get(0).path("Amount").decimalValue()) == 0);
            localPayments.add(detail);
        }
        result.put("paymentDetailsAvailable", paymentsObservable);
        result.put("mawaPayments", localPayments);
        // Xero invoice details are not guaranteed to include complete credit-note
        // allocation evidence; do not interpret absent details as zero allocations.
        JsonNode credits = remote.path("CreditNotes");
        result.put("creditNoteDetailsAvailable", credits.isArray());
        result.put("xeroCreditNotes", credits.isArray() ? credits : null);
        // Independent reads: embedded invoice collections are not authoritative evidence.
        // Query only IDs already linked in MAWA. Unknown IDs remain unresolved and
        // must never be inferred from matching amounts or an empty collection.
        if (payments.size() > 100) throw new IllegalStateException("Payment verification exceeds the 100-record review limit");
        java.util.List<Map<String, Object>> paymentEvidence = new java.util.ArrayList<>();
        String paymentBase = auth.getXeroProperty(config, "XERO-PAYMENTS-URL",
                "https://api.xero.com/api.xro/2.0/Payments").replaceAll("/+$", "");
        for (InvoicePaymentEntity payment : payments) {
            Map<String, Object> evidence = new java.util.LinkedHashMap<>();
            evidence.put("mawaPaymentId", payment.getId());
            evidence.put("xeroPaymentId", payment.getXeroPaymentId());
            String paymentId = payment.getXeroPaymentId();
            if (paymentId == null || paymentId.isBlank()) {
                evidence.put("verification", "UNKNOWN_MISSING_XERO_ID");
            } else {
                JsonNode response = getXero(paymentBase + "/" + URLEncoder.encode(paymentId, StandardCharsets.UTF_8));
                evidence.put("verification", verifyIndependentPayment(response, paymentId,
                        externalId, payment.getAmountCents()));
                if ("CANCELLED".equalsIgnoreCase(payment.getStatus()) || payment.getReversedAt() != null) {
                    evidence.put("reversalEvidence", verifyPaymentReversalEvidence(response, paymentId, externalId));
                    evidence.put("reversalCertified", false);
                }
            }
            paymentEvidence.add(evidence);
        }
        result.put("independentPaymentEvidence", paymentEvidence);
        java.util.List<Map<String, Object>> noteEvidence = new java.util.ArrayList<>();
        java.util.List<Map<String, Object>> localNotes = jdbcTemplate.queryForList("""
                SELECT id, xero_credit_note_id, total_cents, currency, xero_allocated_at
                  FROM credit_note WHERE invoice_id=?
                """, invoiceId);
        if (localNotes.size() > 100) throw new IllegalStateException("Credit-note verification exceeds the 100-record review limit");
        for (Map<String, Object> note : localNotes) {
            Map<String, Object> evidence = new java.util.LinkedHashMap<>();
            evidence.put("mawaCreditNoteId", note.get("id"));
            String noteId = stringValue(note.get("xero_credit_note_id"));
            evidence.put("xeroCreditNoteId", noteId);
            if (noteId == null) {
                evidence.put("verification", "UNKNOWN_MISSING_XERO_ID");
            } else {
                JsonNode response = getXero(creditNotesBaseUrl() + "/" + URLEncoder.encode(noteId, StandardCharsets.UTF_8));
                Long cents = note.get("total_cents") instanceof Number amount ? amount.longValue() : null;
                evidence.put("verification", verifyIndependentCreditNote(response, noteId,
                        externalId, cents, stringValue(note.get("currency"))));
                evidence.put("allocationEvidence", inspectCreditNoteAllocations(response, noteId, externalId));
            }
            noteEvidence.add(evidence);
        }
        result.put("independentCreditNoteEvidence", noteEvidence);
        result.put("financialComparison", compareLocalFinancialEvidence(
                payments, paymentEvidence, localNotes, noteEvidence, remote));
        result.put("ledgerVerified", false);
        result.put("releaseAllowed", false);
        result.put("message", "Read-only evidence only; local-to-Xero allocation amounts, unmatched external postings and complete ledger balances are not yet certified");
        return result;
    }

    /**
     * Read-only comparison of MAWA's current postings with independently retrieved Xero evidence.
     * A locally absent external ID is UNKNOWN, never evidence that Xero did not post it.
     * Embedded invoice arrays can expose extra Xero records, but their absence is not completeness proof.
     */
    static Map<String, Object> compareLocalFinancialEvidence(
            java.util.List<InvoicePaymentEntity> payments,
            java.util.List<Map<String, Object>> paymentEvidence,
            java.util.List<Map<String, Object>> notes,
            java.util.List<Map<String, Object>> noteEvidence,
            JsonNode remoteInvoice) {
        Map<String, Object> comparison = new java.util.LinkedHashMap<>();
        java.util.List<String> blockers = new java.util.ArrayList<>();
        BigDecimal localPaymentTotal = BigDecimal.ZERO;
        BigDecimal verifiedPaymentTotal = BigDecimal.ZERO;
        java.util.Set<String> knownPaymentIds = new java.util.HashSet<>();
        for (int i = 0; i < payments.size(); i++) {
            InvoicePaymentEntity payment = payments.get(i);
            String status = payment.getStatus();
            if (!"POSTED".equalsIgnoreCase(status) || payment.getReversedAt() != null) {
                blockers.add("Payment " + payment.getId() + " needs reversal/status review");
                continue;
            }
            Long cents = payment.getAmountCents();
            if (cents == null || cents < 0) {
                blockers.add("Payment " + payment.getId() + " has invalid amount");
                continue;
            }
            BigDecimal amount = BigDecimal.valueOf(cents, 2);
            localPaymentTotal = localPaymentTotal.add(amount);
            String id = payment.getXeroPaymentId();
            if (id == null || id.isBlank() || !knownPaymentIds.add(id)) {
                blockers.add("Payment " + payment.getId() + " has a missing or duplicate Xero ID");
                continue;
            }
            if (i >= paymentEvidence.size() || !"EXACT_MATCH".equals(paymentEvidence.get(i).get("verification"))) {
                blockers.add("Payment " + payment.getId() + " is not independently verified");
                continue;
            }
            verifiedPaymentTotal = verifiedPaymentTotal.add(amount);
        }
        comparison.put("mawaPostedPaymentTotal", localPaymentTotal);
        comparison.put("independentlyVerifiedPaymentTotal", verifiedPaymentTotal);
        JsonNode xeroPaid = remoteInvoice.path("AmountPaid");
        comparison.put("xeroAmountPaid", xeroPaid.isNumber() ? xeroPaid.decimalValue() : null);
        if (!xeroPaid.isNumber() || localPaymentTotal.compareTo(xeroPaid.decimalValue()) != 0)
            blockers.add("MAWA posted payment total cannot be matched to Xero AmountPaid");
        if (localPaymentTotal.compareTo(verifiedPaymentTotal) != 0)
            blockers.add("Not all MAWA posted payments have independent Xero confirmation");

        BigDecimal verifiedInvoiceCredits = BigDecimal.ZERO;
        java.util.Set<String> knownNoteIds = new java.util.HashSet<>();
        for (int i = 0; i < notes.size(); i++) {
            Map<String, Object> note = notes.get(i);
            String noteId = stringValue(note.get("xero_credit_note_id"));
            if (noteId == null || !knownNoteIds.add(noteId)) {
                blockers.add("Credit note " + note.get("id") + " has a missing or duplicate Xero ID");
                continue;
            }
            if (i >= noteEvidence.size()) {
                blockers.add("Credit note " + note.get("id") + " has no Xero evidence");
                continue;
            }
            Map<String, Object> evidence = noteEvidence.get(i);
            if (!"EXACT_MATCH_ALLOCATION_PRESENT".equals(evidence.get("verification"))) {
                blockers.add("Credit note " + note.get("id") + " has unresolved identity/allocation");
                continue;
            }
            Object allocationObject = evidence.get("allocationEvidence");
            if (!(allocationObject instanceof Map<?, ?> allocation)
                    || !"XERO_ALLOCATION_ARITHMETIC_MATCH".equals(allocation.get("verification"))
                    || !(allocation.get("targetInvoiceAllocatedAmount") instanceof BigDecimal allocated)) {
                blockers.add("Credit note " + note.get("id") + " lacks verified allocation amounts");
                continue;
            }
            if (note.get("xero_allocated_at") == null)
                blockers.add("Credit note " + note.get("id") + " has no local allocation marker");
            verifiedInvoiceCredits = verifiedInvoiceCredits.add(allocated);
        }
        comparison.put("verifiedInvoiceCreditAllocations", verifiedInvoiceCredits);
        JsonNode xeroCredited = remoteInvoice.path("AmountCredited");
        comparison.put("xeroAmountCredited", xeroCredited.isNumber() ? xeroCredited.decimalValue() : null);
        if (!xeroCredited.isNumber() || verifiedInvoiceCredits.compareTo(xeroCredited.decimalValue()) != 0)
            blockers.add("Verified credit allocations do not match Xero AmountCredited");

        JsonNode embeddedPayments = remoteInvoice.path("Payments");
        if (embeddedPayments.isArray()) {
            for (JsonNode item : embeddedPayments) {
                String id = item.path("PaymentID").asText(null);
                if (id == null || !knownPaymentIds.contains(id)) {
                    blockers.add("Xero invoice contains an unlinked or unidentified payment");
                    break;
                }
            }
        } else blockers.add("Xero invoice response did not include complete payment identifiers");
        JsonNode embeddedCredits = remoteInvoice.path("CreditNotes");
        if (embeddedCredits.isArray()) {
            for (JsonNode item : embeddedCredits) {
                String id = item.path("CreditNoteID").asText(null);
                if (id == null || !knownNoteIds.contains(id)) {
                    blockers.add("Xero invoice contains an unlinked or unidentified credit note");
                    break;
                }
            }
        } else blockers.add("Xero invoice response did not include complete credit note identifiers");
        comparison.put("blockers", blockers);
        comparison.put("localEvidenceConsistent", blockers.isEmpty());
        comparison.put("ledgerCertified", false); // Completeness of remote collections not established.
        comparison.put("releaseAllowed", false);
        return comparison;
    }

    /** Xero arithmetic only; this is not proof of MAWA-to-Xero balance equality. */
    static String verifyInvoiceBalanceArithmetic(JsonNode invoice) {
        if (invoice == null || !invoice.path("Total").isNumber()
                || !invoice.path("AmountPaid").isNumber()
                || !invoice.path("AmountCredited").isNumber()
                || !invoice.path("AmountDue").isNumber()) return "UNKNOWN_INCOMPLETE_AMOUNTS";
        BigDecimal calculated = invoice.path("Total").decimalValue()
                .subtract(invoice.path("AmountPaid").decimalValue())
                .subtract(invoice.path("AmountCredited").decimalValue());
        return calculated.compareTo(invoice.path("AmountDue").decimalValue()) == 0
                ? "ARITHMETIC_MATCH" : "ARITHMETIC_CONFLICT";
    }

    /** Inspect all allocations on a unique credit note without assuming the full note was allocated. */
    static Map<String, Object> inspectCreditNoteAllocations(JsonNode body, String creditNoteId, String invoiceId) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("verification", "UNKNOWN_RESPONSE");
        if (body == null || !body.path("CreditNotes").isArray() || creditNoteId == null || invoiceId == null)
            return result;
        JsonNode matched = null;
        int count = 0;
        for (JsonNode note : body.path("CreditNotes")) {
            if (creditNoteId.equals(note.path("CreditNoteID").asText(null))) { matched = note; count++; }
        }
        if (count != 1) {
            result.put("verification", count == 0 ? "UNKNOWN_NOT_RETURNED" : "CONFLICT_DUPLICATE_CREDIT_NOTES");
            return result;
        }
        JsonNode allocations = matched.path("Allocations");
        if (!allocations.isArray()) {
            result.put("verification", "UNKNOWN_ALLOCATIONS_NOT_RETURNED");
            return result;
        }
        BigDecimal invoiceSum = BigDecimal.ZERO;
        BigDecimal allSum = BigDecimal.ZERO;
        int invoiceCount = 0;
        for (JsonNode allocation : allocations) {
            if (!allocation.path("Amount").isNumber()) {
                result.put("verification", "UNKNOWN_ALLOCATION_AMOUNT");
                return result;
            }
            BigDecimal amount = allocation.path("Amount").decimalValue();
            if (amount.signum() < 0) {
                result.put("verification", "CONFLICT_NEGATIVE_ALLOCATION");
                return result;
            }
            allSum = allSum.add(amount);
            if (invoiceId.equals(allocation.path("Invoice").path("InvoiceID").asText(null))) {
                invoiceSum = invoiceSum.add(amount);
                invoiceCount++;
            }
        }
        result.put("targetInvoiceAllocationCount", invoiceCount);
        result.put("targetInvoiceAllocatedAmount", invoiceSum);
        result.put("allInvoicesAllocatedAmount", allSum);
        if (!matched.path("Total").isNumber() || !matched.path("RemainingCredit").isNumber()) {
            result.put("verification", "UNKNOWN_CREDIT_BALANCE");
            return result;
        }
        BigDecimal total = matched.path("Total").decimalValue();
        BigDecimal remaining = matched.path("RemainingCredit").decimalValue();
        result.put("xeroRemainingCredit", remaining);
        result.put("verification", total.subtract(allSum).compareTo(remaining) == 0 && remaining.signum() >= 0
                ? "XERO_ALLOCATION_ARITHMETIC_MATCH" : "XERO_ALLOCATION_ARITHMETIC_CONFLICT");
        return result;
    }

    // Exact identity, parent invoice and amount are mandatory. We do not use
    // an empty response as evidence that a financial posting never happened.
    /**
     * Read-only evidence for a cancelled MAWA payment. Xero's explicit DELETED
     * status is evidence of reversal; a missing record / HTTP failure is not.
     * This method never certifies the ledger or changes a local sync status.
     */
    static String verifyPaymentReversalEvidence(JsonNode body, String paymentId, String invoiceId) {
        if (body == null || !body.path("Payments").isArray() || paymentId == null || invoiceId == null)
            return "UNKNOWN_RESPONSE";
        int matches = 0;
        String outcome = "UNKNOWN_NOT_RETURNED";
        for (JsonNode payment : body.path("Payments")) {
            if (!paymentId.equals(payment.path("PaymentID").asText(null))) continue;
            matches++;
            if (!invoiceId.equals(payment.path("Invoice").path("InvoiceID").asText(null))) {
                outcome = "CONFLICT_INVOICE";
            } else {
                String status = payment.path("Status").asText("").toUpperCase(java.util.Locale.ROOT);
                outcome = switch (status) {
                    case "DELETED" -> "REMOTE_DELETED_EVIDENCE";
                    case "AUTHORISED", "AUTHORIZED" -> "REMOTE_ACTIVE_PAYMENT";
                    default -> "UNKNOWN_REMOTE_STATUS";
                };
            }
        }
        return matches > 1 ? "CONFLICT_DUPLICATE_ID" : outcome;
    }

    static String verifyIndependentPayment(JsonNode body, String paymentId, String invoiceId, Long cents) {
        if (body == null || !body.path("Payments").isArray() || cents == null) return "UNKNOWN_RESPONSE";
        int matches = 0;
        boolean valid = false;
        for (JsonNode payment : body.path("Payments")) {
            if (!paymentId.equals(payment.path("PaymentID").asText(null))) continue;
            matches++;
            valid = invoiceId.equals(payment.path("Invoice").path("InvoiceID").asText(null))
                    && payment.path("Amount").isNumber()
                    && BigDecimal.valueOf(cents, 2).compareTo(payment.path("Amount").decimalValue()) == 0;
        }
        if (matches == 0) return "UNKNOWN_NOT_RETURNED";
        return matches == 1 && valid ? "EXACT_MATCH" : "CONFLICT";
    }

    static String verifyIndependentCreditNote(JsonNode body, String creditNoteId, String invoiceId,
                                               Long cents, String currency) {
        if (body == null || !body.path("CreditNotes").isArray() || cents == null || currency == null)
            return "UNKNOWN_RESPONSE";
        int matches = 0;
        boolean valid = false;
        for (JsonNode note : body.path("CreditNotes")) {
            if (!creditNoteId.equals(note.path("CreditNoteID").asText(null))) continue;
            matches++;
            boolean exact = note.path("Total").isNumber()
                    && BigDecimal.valueOf(cents, 2).compareTo(note.path("Total").decimalValue()) == 0
                    && currency.equalsIgnoreCase(note.path("CurrencyCode").asText(""));
            JsonNode allocations = note.path("Allocations");
            boolean allocatedToInvoice = false;
            if (allocations.isArray()) {
                for (JsonNode allocation : allocations) {
                    if (invoiceId.equals(allocation.path("Invoice").path("InvoiceID").asText(null))) {
                        allocatedToInvoice = true;
                        break;
                    }
                }
            }
            valid = exact && allocatedToInvoice;
        }
        if (matches == 0) return "UNKNOWN_NOT_RETURNED";
        return matches == 1 && valid ? "EXACT_MATCH_ALLOCATION_PRESENT" : "CONFLICT_OR_ALLOCATION_UNKNOWN";
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
