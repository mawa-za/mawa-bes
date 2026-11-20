package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.v2.funeral.FuneralClaimFundingReversalRequestDto;
import za.co.mawa.bes.service.UserAccessService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * MWE-117: Compensating claims rather than deleting/editing approved and paid claims.
 * A paid claim remains PAID; a replacement is only possible after a separately
 * imported bank recovery is allocated in the claim-owning tenant.
 * Does not initiate a bank refund, cancel an in-flight FNB instruction or amend
 * an original payout. Every state change is written to the tenant audit ledger.
 */
@Service
@RequiredArgsConstructor
public class FuneralClaimFundingReversalService {
    private static final Set<String> PRIMARY_TYPES = Set.of("FUNERAL", "COMBINATION", "STILLBORN");
    private static final Set<String> UNSENT_REQUESTS = Set.of("CANCELLED", "REJECTED", "FAILED");
    private final JdbcTemplate jdbc;
    private final UserAccessService access;

    @Transactional
    public Map<String, Object> reconcile(String funeralServiceId, String claimId,
                                          FuneralClaimFundingReversalRequestDto request) {
        access.assertPermission("funeral-funding-reconcile", null);
        if (request == null || !StringUtils.hasText(request.getReason())
                || request.getReason().trim().length() < 10 || request.getReason().length() > 800) {
            throw new IllegalArgumentException("Finance reconciliation reason (10-800 characters) is required");
        }
        if (!StringUtils.hasText(request.getReference()) || request.getReference().trim().length() < 4
                || request.getReference().length() > 255) {
            throw new IllegalArgumentException("An auditable finance reconciliation reference is required");
        }
        // Always lock the provider arrangement first. This is the same lock
        // order as reopenFunding and initiateClaims (protects concurrent retries).
        List<String> locked = jdbc.query("SELECT id FROM funeral_service WHERE id=? FOR UPDATE",
                (rs, i) -> rs.getString(1), funeralServiceId);
        if (locked.isEmpty()) throw new IllegalArgumentException("Funeral service request not found");
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM funeral_service_claim WHERE funeral_service_id=? AND membership_claim_id=? FOR UPDATE",
                funeralServiceId, claimId);
        if (rows.size() != 1) throw new IllegalArgumentException("Claim is not linked to this funeral service");
        Map<String, Object> link = rows.get(0);
        String scope = Objects.toString(link.get("claim_storage_scope"), "LOCAL");
        String owner = "EXTERNAL".equalsIgnoreCase(scope)
                ? Objects.toString(link.get("claim_owner_tenant_id"), "") : TenantContext.getCurrentTenant();
        if ("EXTERNAL".equalsIgnoreCase(scope)) {
            Long trusted = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM tenant_trust_relationship"
                    + " WHERE requester_tenant_id=? AND provider_tenant_id=?"
                    + " AND integration_type='FUNERAL_MEMBERSHIP_CLAIMS' AND status='APPROVED'"
                    + " AND allow_claim_creation=1 AND allow_settlement=1",
                    Long.class, TenantContext.getCurrentTenant(), owner);
            if (trusted == null || trusted == 0) {
                throw new SecurityException("Funeral settlement trust for the claim-owning tenant is not approved");
            }
        }
        String claimTable = qualified(owner, "membership_claim");
        String reverseTable = qualified(owner, "funeral_claim_funding_reversal");
        String ownerPaymentTable = qualified(owner, "payment_request");

        List<Map<String, Object>> priorReversals = jdbc.queryForList(
                "SELECT * FROM " + reverseTable + " WHERE original_claim_id=?", claimId);
        if (!priorReversals.isEmpty()) return priorReversals.get(0); // idempotent; do not allocate a second deposit
        if (link.get("superseded_at") != null) {
            throw new IllegalStateException("Claim was superseded without a completed source-tenant reconciliation");
        }
        List<Map<String, Object>> claims = jdbc.queryForList(
                "SELECT id,claim_no,claim_type,status,claim_amount_cents,approved_amount_cents,"
                        + "funeral_service_id,funeral_provider_tenant_id,payment_request_id "
                        + "FROM " + claimTable + " WHERE id=? FOR UPDATE", claimId);
        if (claims.size() != 1) throw new IllegalArgumentException("Source claim not found in its owning tenant");
        Map<String, Object> claim = claims.get(0);
        String type = Objects.toString(claim.get("claim_type"), "").toUpperCase();
        String status = Objects.toString(claim.get("status"), "").toUpperCase();
        if (!PRIMARY_TYPES.contains(type)) throw new IllegalStateException("Only funeral cover claims can be reconciled");
        if (!funeralServiceId.equals(Objects.toString(claim.get("funeral_service_id"), ""))
                || !TenantContext.getCurrentTenant().equals(
                        Objects.toString(claim.get("funeral_provider_tenant_id"), ""))) {
            throw new SecurityException("The source claim is not owned by this funeral-provider arrangement");
        }
        if ("DRAFT".equals(status) || "CANCELLED".equals(status)) {
            throw new IllegalStateException("Draft or cancelled claims should use the normal amendment flow");
        }
        Long outstandingApprovals = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + qualified(owner, "approval_request")
                        + " WHERE reference_id=? AND status IN ('PENDING','IN_PROGRESS')",
                Long.class, claimId);
        if (outstandingApprovals != null && outstandingApprovals > 0) {
            throw new IllegalStateException("Withdraw the pending claim approval in the owning tenant before reconciliation");
        }

        List<Map<String, Object>> payments = jdbc.queryForList(
                "SELECT id,status,amount,paid_date,fnb_instruction_id FROM " + ownerPaymentTable
                        + " WHERE (source_type='MEMBERSHIP_CLAIM' AND source_id=?)"
                        + " OR id=? ORDER BY created_at FOR UPDATE",
                claimId, Objects.toString(link.get("service_payment_request_id"), ""));
        long paidAmountCents = 0;
        String paidRequestId = null;
        for (Map<String, Object> payment : payments) {
            String paymentStatus = Objects.toString(payment.get("status"), "").toUpperCase();
            String instruction = Objects.toString(payment.get("fnb_instruction_id"), "");
            if ("PAID".equals(paymentStatus)) {
                paidRequestId = Objects.toString(payment.get("id"), null);
                paidAmountCents += cents(payment.get("amount"));
            } else if (!UNSENT_REQUESTS.contains(paymentStatus) || StringUtils.hasText(instruction)) {
                // APPROVED, QUEUED_FOR_PAYMENT and PROCESSED can have an in-flight bank transfer.
                throw new IllegalStateException("Payment request " + payment.get("id") + " is "
                        + paymentStatus + "; confirm bank cancellation or settlement before reversal");
            }
        }
        if ("PAID".equals(status) && paidAmountCents == 0) {
            throw new IllegalStateException("Claim is marked PAID but no completed payment request was found; reconcile manually");
        }
        if (paidAmountCents > 0 && request.getRecoveryBankTransactionId() == null) {
            throw new IllegalStateException("Paid claim requires an imported recovery deposit in the cover-owning tenant");
        }
        if (paidAmountCents == 0 && request.getRecoveryBankTransactionId() != null) {
            throw new IllegalArgumentException("Recovery bank transaction is only applicable to paid claims");
        }
        if (paidAmountCents > 0) {
            List<Map<String, Object>> recoveries = jdbc.queryForList(
                    "SELECT id,amount_cents,transaction_date FROM " + qualified(owner, "bank_transaction")
                            + " WHERE id=? FOR UPDATE", request.getRecoveryBankTransactionId());
            if (recoveries.size() != 1 || number(recoveries.get(0).get("amount_cents")) <= 0) {
                throw new IllegalStateException("Recovery deposit must be a positive, imported bank transaction");
            }
            // A deposit cannot be allocated to multiple funeral reversals or cashups beyond its face value.
            Long alreadyAllocated = jdbc.queryForObject(
                    "SELECT COALESCE(SUM(amount_cents),0) FROM " + qualified(owner, "bank_transaction_allocation")
                            + " WHERE bank_transaction_id=? AND released_at IS NULL",
                    Long.class, request.getRecoveryBankTransactionId());
            if (number(recoveries.get(0).get("amount_cents")) - (alreadyAllocated == null ? 0 : alreadyAllocated)
                    < paidAmountCents) {
                throw new IllegalStateException("Unallocated imported deposit does not cover the original payout");
            }
            Object bankDate = recoveries.get(0).get("transaction_date");
            LocalDate recoveredAt = bankDate instanceof LocalDate date ? date
                    : bankDate instanceof java.sql.Date date ? date.toLocalDate()
                    : LocalDate.parse(bankDate.toString());
            for (Map<String, Object> payment : payments) {
                if ("PAID".equalsIgnoreCase(Objects.toString(payment.get("status"), ""))
                        && payment.get("paid_date") != null
                        && recoveredAt.isBefore(payment.get("paid_date") instanceof LocalDate paidOn
                            ? paidOn : payment.get("paid_date") instanceof java.sql.Date date
                            ? date.toLocalDate() : LocalDate.parse(payment.get("paid_date").toString()))) {
                    throw new IllegalStateException("Recovery bank transaction predates the original payout");
                }
            }
        }
        String actor = StringUtils.hasText(UserContext.getCurrentUserId())
                ? UserContext.getCurrentUserId() : UserContext.getCurrentUser();
        if (!StringUtils.hasText(actor)) throw new SecurityException("An authenticated finance user is required");
        String reversalId = UUID.randomUUID().toString();
        if (paidAmountCents > 0) {
            jdbc.update("INSERT INTO " + qualified(owner, "bank_transaction_allocation")
                            + " (bank_transaction_id,allocation_type,object_id,amount_cents,created_by,created_at)"
                            + " VALUES (?,'FUNERAL_CLAIM_RECOVERY',?,?,?,CURRENT_TIMESTAMP)",
                    request.getRecoveryBankTransactionId(), claimId, paidAmountCents, actor);
        }
        jdbc.update("INSERT INTO " + reverseTable
                        + " (id,original_claim_id,funeral_service_id,funeral_provider_tenant_id,original_claim_status,"
                        + "original_claim_type,original_claim_amount_cents,payment_request_id,paid_amount_cents,"
                        + "recovery_bank_transaction_id,finance_reference,reason,reconciled_by,reconciled_at)"
                        + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)",
                reversalId, claimId, funeralServiceId, TenantContext.getCurrentTenant(), status, type,
                number(claim.get("claim_amount_cents")), paidRequestId, paidAmountCents,
                request.getRecoveryBankTransactionId(), request.getReference().trim(),
                request.getReason().trim(), actor);
        if (paidAmountCents == 0) {
            // A completed approval is retained; the original claim itself is cancelled.
            // Never change a historical PAID status, payment request or bank instruction.
            jdbc.update("UPDATE " + claimTable
                    + " SET status='CANCELLED',notes=CONCAT(COALESCE(notes,''),?),"
                    + " updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                    "\nMWE-117: superseded by funeral funding amendment " + reversalId,
                    actor, claimId);
        } else if (!"PAID".equals(status)) {
            // If the bank has completed a payment but its claim callback was missed,
            // repair that stale status so the settlement scheduler cannot pay it again.
            jdbc.update("UPDATE " + claimTable
                    + " SET status='PAID',updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                    actor, claimId);
        }
        jdbc.update("UPDATE funeral_service_claim SET superseded_at=CURRENT_TIMESTAMP WHERE id=?",
                link.get("id"));
        return jdbc.queryForMap("SELECT * FROM " + reverseTable + " WHERE original_claim_id=?", claimId);
    }

    public List<Map<String, Object>> audit(String funeralServiceId) {
        access.assertPermission("funeral-funding-reconcile", null);
        // The originating tenant owns all claim history links; external source ledgers
        // are read by explicit verified owner identity, never by client-supplied schema.
        List<Map<String, Object>> links = jdbc.queryForList(
                "SELECT membership_claim_id,claim_storage_scope,claim_owner_tenant_id FROM funeral_service_claim"
                        + " WHERE funeral_service_id=? AND superseded_at IS NOT NULL", funeralServiceId);
        java.util.ArrayList<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Map<String, Object> link : links) {
            String owner = "EXTERNAL".equalsIgnoreCase(Objects.toString(link.get("claim_storage_scope"), ""))
                    ? Objects.toString(link.get("claim_owner_tenant_id"), "") : TenantContext.getCurrentTenant();
            result.addAll(jdbc.queryForList("SELECT original_claim_id,original_claim_status,original_claim_type,"
                            + "original_claim_amount_cents,paid_amount_cents,recovery_bank_transaction_id,"
                            + "finance_reference,reason,reconciled_by,reconciled_at FROM "
                            + qualified(owner, "funeral_claim_funding_reversal")
                            + " WHERE original_claim_id=? AND funeral_service_id=?",
                    link.get("membership_claim_id"), funeralServiceId));
        }
        return result;
    }

    private static long cents(Object amount) {
        if (amount == null) return 0;
        return new BigDecimal(amount.toString()).movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY).longValueExact();
    }
    private static long number(Object amount) { return amount instanceof Number n ? n.longValue() : 0L; }
    private static String qualified(String tenant, String table) {
        if (tenant == null || !tenant.matches("[A-Za-z0-9_-]{1,128}"))
            throw new SecurityException("Invalid source tenant");
        return "`" + tenant + "`.`" + table + "`";
    }
}
