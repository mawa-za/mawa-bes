package za.co.mawa.bes.service.v2;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.entity.v2.CashupDepositEntity;
import za.co.mawa.bes.entity.v2.PaymentBatchEntity;
import za.co.mawa.bes.enums.PaymentBatchStatus;
import za.co.mawa.bes.repository.v2.CashupDepositRepository;
import za.co.mawa.bes.repository.v2.PaymentBatchRepository;
import java.time.LocalDate;

/** Links historic, already-posted payments to bank credits without changing postings or approvals. */
@Service @RequiredArgsConstructor
public class BankDepositReconciliationService {
    private final PaymentBatchRepository batches;
    private final CashupDepositRepository deposits;
    private final BankTransactionService transactions;
    private final EntityManager em;
    private final JdbcTemplate jdbc;

    public record PendingItem(String type, String objectId, String documentNo, LocalDate date,
                              long amountCents, String reference, String status, String verificationNote) { }
    public record ReconcileRequest(String type, String objectId, String depositId) { }
    public record ReconcileResult(String type, String objectId, String depositId, String status) { }

    /** Keep the last mismatch visible even though the failed allocation rolled back. */
    @Transactional
    public void recordException(ReconcileRequest request, RuntimeException failure) {
        if (request == null || !validType(request.type()) || request.objectId() == null
                || request.objectId().isBlank() || request.objectId().length() > 255) return;
        String actor = UserContext.getCurrentUserId();
        if (actor == null || actor.isBlank()) return;
        String message = failure.getMessage() == null ? "Verification could not be completed" : failure.getMessage();
        jdbc.update("""
                INSERT INTO bank_deposit_reconciliation_exception
                  (allocation_type, object_id, proposed_deposit_id, reason, recorded_by)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE proposed_deposit_id=VALUES(proposed_deposit_id),
                  reason=VALUES(reason), recorded_by=VALUES(recorded_by), recorded_at=CURRENT_TIMESTAMP(6)
                """, request.type(), request.objectId().trim(),
                request.depositId() == null ? null : request.depositId().substring(0, Math.min(40, request.depositId().length())),
                message.substring(0, Math.min(500, message.length())), actor);
    }

    private boolean validType(String type) {
        return "PAYMENT_BATCH".equals(type) || "CASHUP_DEPOSIT".equals(type);
    }

    private record VerificationState(String status, String note) { }

    private VerificationState verificationState(String type, String id) {
        var reasons = jdbc.query("SELECT reason FROM bank_deposit_reconciliation_exception "
                        + "WHERE allocation_type = ? AND object_id = ?",
                (rs, rowNum) -> rs.getString(1), type, id);
        return reasons.isEmpty() ? new VerificationState("PENDING_BANK_VERIFICATION", null)
                : new VerificationState("VERIFICATION_EXCEPTION", reasons.get(0));
    }

    @Transactional(readOnly = true)
    public Page<PendingItem> pending(String type, String query, Pageable pageable) {
        String normalized = query == null ? "" : query.trim();
        if ("PAYMENT_BATCH".equals(type)) {
            return batches.pendingBankVerification(normalized, PaymentBatchStatus.POSTED, pageable).map(b -> {
                VerificationState state = verificationState(type, b.getId());
                return new PendingItem(type, b.getId(), b.getPaymentBatchNo(),
                        b.getPaymentDate() == null ? null : b.getPaymentDate().toLocalDate(),
                        b.getTotalAmountCents() == null ? 0 : b.getTotalAmountCents(),
                        b.getMembershipId(), state.status(), state.note());
            });
        }
        if ("CASHUP_DEPOSIT".equals(type)) {
            return deposits.pendingBankVerification(normalized, pageable).map(d -> {
                VerificationState state = verificationState(type, d.getId());
                return new PendingItem(type, d.getId(),
                        d.getCashup() == null ? "" : String.valueOf(d.getCashup().getCashupNo()),
                        d.getDepositDate(), d.getAmountCents() == null ? 0 : d.getAmountCents(),
                        d.getReferenceNo(), state.status(), state.note());
            });
        }
        throw new IllegalArgumentException("Only PAYMENT_BATCH and CASHUP_DEPOSIT can be reconciled");
    }

    @Transactional
    public ReconcileResult reconcile(ReconcileRequest request) {
        if (request == null || request.objectId() == null || request.objectId().isBlank())
            throw new IllegalArgumentException("An existing payment or deposit must be selected");
        String actor = UserContext.getCurrentUserId();
        if (actor == null || actor.isBlank() || UserContext.isBackgroundSession())
            throw new SecurityException("An authenticated ERP user is required to reconcile deposits");
        String type = request.type();
        String id = request.objectId().trim();
        Long bankId;
        if ("PAYMENT_BATCH".equals(type)) {
            PaymentBatchEntity batch = em.find(PaymentBatchEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (batch == null || !"EFT".equalsIgnoreCase(batch.getPaymentMethod())
                    || batch.getStatus() != PaymentBatchStatus.POSTED)
                throw new IllegalArgumentException("Only posted EFT payments can be reconciled");
            if (batch.getBankDepositId() != null)
                throw new IllegalStateException("The EFT payment is already bank verified");
            if (batch.getTotalAmountCents() == null || batch.getTotalAmountCents() <= 0)
                throw new IllegalArgumentException("EFT payment has an invalid amount");
            bankId = transactions.allocate(request.depositId(), batch.getTotalAmountCents(),
                    "PAYMENT_BATCH", id, actor);
            batch.setBankDepositId(bankId);
            batch.setUpdatedBy(actor);
            // Intentionally leave the posted amount, receipt and accounting records unchanged.
            em.flush();
        } else if ("CASHUP_DEPOSIT".equals(type)) {
            CashupDepositEntity deposit = em.find(CashupDepositEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (deposit == null) throw new IllegalArgumentException("Cashup deposit was not found");
            if (deposit.getBankDepositId() != null)
                throw new IllegalStateException("This cashup deposit is already bank verified");
            if (deposit.getAmountCents() == null || deposit.getAmountCents() <= 0)
                throw new IllegalArgumentException("Cashup deposit has an invalid amount");
            bankId = transactions.allocate(request.depositId(), deposit.getAmountCents(),
                    "CASHUP_DEPOSIT", id, actor);
            deposit.setBankDepositId(bankId);
            deposit.setUpdatedBy(actor);
            // Already approved cashups remain approved. Only bank reconciliation is updated.
            em.flush();
        } else {
            throw new IllegalArgumentException("Only PAYMENT_BATCH and CASHUP_DEPOSIT can be reconciled");
        }
        jdbc.update("DELETE FROM bank_deposit_reconciliation_exception WHERE allocation_type = ? AND object_id = ?", type, id);
        return new ReconcileResult(type, id, BankTransactionService.formatId(bankId), "BANK_VERIFIED");
    }
}
