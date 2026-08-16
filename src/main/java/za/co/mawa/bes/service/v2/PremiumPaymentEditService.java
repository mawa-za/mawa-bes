package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.dto.v2.PremiumPaymentEditRequest;
import za.co.mawa.bes.entity.v2.ManualPremiumReceiptEntity;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.entity.v2.MembershipPremiumEntity;
import za.co.mawa.bes.entity.v2.PaymentBatchEntity;
import za.co.mawa.bes.entity.v2.ReceiptAllocationEntity;
import za.co.mawa.bes.entity.v2.ReceiptEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.enums.PaymentBatchStatus;
import za.co.mawa.bes.enums.ReceiptAllocationType;
import za.co.mawa.bes.enums.ReceiptSourceType;
import za.co.mawa.bes.enums.ReceiptStatus;
import za.co.mawa.bes.repository.v2.ManualPremiumReceiptRepository;
import za.co.mawa.bes.repository.v2.MembershipPremiumRepository;
import za.co.mawa.bes.repository.v2.PaymentBatchRepository;
import za.co.mawa.bes.repository.v2.ReceiptAllocationRepository;
import za.co.mawa.bes.repository.v2.ReceiptRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PremiumPaymentEditService {
    private final PaymentBatchRepository paymentBatchRepository;
    private final ReceiptRepository receiptRepository;
    private final ReceiptAllocationRepository receiptAllocationRepository;
    private final MembershipPremiumRepository membershipPremiumRepository;
    private final MembershipPremiumService membershipPremiumService;
    private final ManualPremiumReceiptRepository manualPremiumReceiptRepository;
    private final MembershipService membershipService;
    private final OnlineCashupService onlineCashupService;
    private final ApprovalService approvalService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public ApprovalRequestResponse requestEdit(String paymentBatchId, PremiumPaymentEditRequest request) {
        validateRequest(request);
        PaymentContext context = paymentContext(paymentBatchId, request.getReceiptId());
        long proposedAmount = request.getAmountCents();
        String proposedPeriod = request.getPeriodYYYYMM().trim();
        long currentAmount = value(context.allocation().getAmountCents());
        String currentPeriod = clean(context.allocation().getPeriodYYYYMM());
        if (currentPeriod == null) {
            throw new IllegalStateException("The existing premium allocation does not have a valid payment period");
        }

        if (currentAmount == proposedAmount && Objects.equals(currentPeriod, proposedPeriod)) {
            throw new IllegalArgumentException("Change the payment amount, payment period, or both before submitting");
        }

        Integer pending = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM premium_payment_edit_request
                 WHERE receipt_id=? AND status='PENDING_APPROVAL'
                """, Integer.class, context.receipt().getId());
        if (pending != null && pending > 0) {
            throw new IllegalStateException("This payment already has an edit request awaiting approval");
        }

        validateTargetCapacity(context, proposedPeriod, proposedAmount, currentAmount);

        String actionId = UUID.randomUUID().toString();
        String actor = request.getRequestedBy().trim();
        String reason = request.getReason().trim();
        jdbcTemplate.update("""
                INSERT INTO premium_payment_edit_request(
                    id,payment_batch_id,receipt_id,membership_id,
                    previous_amount_cents,requested_amount_cents,
                    previous_period_yyyymm,requested_period_yyyymm,
                    status,reason,requested_by,created_at
                ) VALUES(?,?,?,?,?,?,?,?,'PENDING_APPROVAL',?,?,CURRENT_TIMESTAMP)
                """,
                actionId,
                context.batch().getId(),
                context.receipt().getId(),
                context.batch().getMembershipId(),
                currentAmount,
                proposedAmount,
                currentPeriod,
                proposedPeriod,
                reason,
                actor);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentBatchId", context.batch().getId());
        payload.put("paymentBatchNumber", context.batch().getPaymentBatchNo());
        payload.put("receiptId", context.receipt().getId());
        payload.put("receiptNumber", context.receipt().getReceiptNo());
        payload.put("membershipId", context.batch().getMembershipId());
        payload.put("currentValues", Map.of(
                "amountCents", currentAmount,
                "periodYYYYMM", currentPeriod));
        payload.put("proposedValues", Map.of(
                "amountCents", proposedAmount,
                "periodYYYYMM", proposedPeriod));
        payload.put("reason", reason);
        payload.put("attachmentObjectIds", clean(context.batch().getMembershipId()) == null
                ? List.of()
                : List.of(context.batch().getMembershipId()));

        ApprovalSubmitRequest approval = new ApprovalSubmitRequest();
        approval.setApprovalType(ApprovalType.PREMIUM_PAYMENT_EDIT);
        approval.setReferenceId(actionId);
        approval.setReferenceNo(context.batch().getPaymentBatchNo());
        approval.setTitle("Edit premium payment " + context.batch().getPaymentBatchNo());
        approval.setDescription("Review the current and proposed premium payment amount and period before approval.");
        approval.setRequesterId(actor);
        approval.setPayloadJson(toJson(payload));
        ApprovalRequestResponse response = approvalService.submitForApproval(approval);
        jdbcTemplate.update(
                "UPDATE premium_payment_edit_request SET approval_request_id=? WHERE id=?",
                response.getId(), actionId);
        return response;
    }

    @Transactional
    public void complete(String actionId, boolean approved, String actor, String completionStatus) {
        Map<String, Object> edit = requireEdit(actionId);
        if (!"PENDING_APPROVAL".equalsIgnoreCase(Objects.toString(edit.get("status"), ""))) {
            return;
        }
        String completedBy = clean(actor) == null ? "SYSTEM" : actor.trim();
        if (!approved) {
            markComplete(actionId, completionStatus, completedBy);
            return;
        }

        String batchId = Objects.toString(edit.get("payment_batch_id"), "");
        String receiptId = Objects.toString(edit.get("receipt_id"), "");
        PaymentContext context = paymentContext(batchId, receiptId);
        long previousAmount = longValue(edit.get("previous_amount_cents"));
        long requestedAmount = longValue(edit.get("requested_amount_cents"));
        String previousPeriod = Objects.toString(edit.get("previous_period_yyyymm"), "");
        String requestedPeriod = Objects.toString(edit.get("requested_period_yyyymm"), "");

        long currentAmount = value(context.allocation().getAmountCents());
        String currentPeriod = clean(context.allocation().getPeriodYYYYMM());
        if (currentAmount != previousAmount || !Objects.equals(currentPeriod, previousPeriod)) {
            throw new IllegalStateException(
                    "The premium payment changed after this edit request was submitted. Submit a new edit request.");
        }

        MembershipPremiumEntity sourcePremium = membershipPremiumService.getById(context.allocation().getReferenceId());
        membershipPremiumService.reversePayment(sourcePremium, currentAmount, completedBy);

        MembershipEntity membership = membershipService.resolveMembership(context.batch().getMembershipId());
        long monthlyPremiumCents = value(membership.getPremiumCents());
        MembershipPremiumEntity targetPremium = membershipPremiumService.findOrCreatePremium(
                membership.getId(), requestedPeriod, monthlyPremiumCents, completedBy);
        long targetBalance = value(targetPremium.getBalanceCents());
        if (requestedAmount > targetBalance) {
            // Restore the original ledger state before failing the approval completion.
            membershipPremiumService.applyPayment(sourcePremium, currentAmount, completedBy);
            throw new IllegalStateException(
                    "Requested amount exceeds the outstanding balance for period " + requestedPeriod);
        }
        targetPremium = membershipPremiumService.applyPayment(targetPremium, requestedAmount, completedBy);

        ReceiptAllocationEntity allocation = context.allocation();
        allocation.setReferenceId(targetPremium.getId());
        allocation.setReferenceNo(membership.getId() + "-" + requestedPeriod);
        allocation.setPeriodYYYYMM(requestedPeriod);
        allocation.setMembershipId(membership.getId());
        allocation.setAmountCents(requestedAmount);
        allocation.setUpdatedAt(LocalDateTime.now());
        allocation.setUpdatedBy(completedBy);
        receiptAllocationRepository.save(allocation);

        ReceiptEntity receipt = context.receipt();
        receipt.setMembershipId(membership.getId());
        receipt.setTotalAmountCents(requestedAmount);
        receipt.setPrinted(false);
        receipt.setNotes(append(receipt.getNotes(), auditNote(previousAmount, requestedAmount, previousPeriod, requestedPeriod, completedBy)));
        receipt.setUpdatedAt(LocalDateTime.now());
        receipt.setUpdatedBy(completedBy);
        receiptRepository.save(receipt);

        PaymentBatchEntity batch = context.batch();
        long revisedBatchTotal = value(batch.getTotalAmountCents()) - previousAmount + requestedAmount;
        if (revisedBatchTotal <= 0) {
            throw new IllegalStateException("The revised payment batch total must be greater than zero");
        }
        batch.setTotalAmountCents(revisedBatchTotal);
        batch.setMembershipId(membership.getId());
        batch.setNotes(append(batch.getNotes(), auditNote(previousAmount, requestedAmount, previousPeriod, requestedPeriod, completedBy)));
        batch.setUpdatedAt(LocalDateTime.now());
        batch.setUpdatedBy(completedBy);
        paymentBatchRepository.save(batch);

        ManualPremiumReceiptEntity manualReceipt = manualPremiumReceiptRepository.findByPaymentBatchId(batch.getId()).orElse(null);
        if (manualReceipt != null) {
            manualReceipt.setMembershipId(membership.getId());
            manualReceipt.setAmountCents(requestedAmount);
            manualReceipt.setNotes(append(manualReceipt.getNotes(), auditNote(previousAmount, requestedAmount, previousPeriod, requestedPeriod, completedBy)));
            manualPremiumReceiptRepository.save(manualReceipt);
        }

        onlineCashupService.refreshReceiptAmount(
                receipt,
                manualReceipt == null ? null : manualReceipt.getId(),
                completedBy);
        membershipService.recalculatePaidUpToPeriod(membership.getId());
        markComplete(actionId, completionStatus, completedBy);
    }

    private PaymentContext paymentContext(String paymentBatchId, String receiptId) {
        PaymentBatchEntity batch = paymentBatchRepository.findById(paymentBatchId)
                .orElseThrow(() -> new IllegalArgumentException("Payment batch not found: " + paymentBatchId));
        if (batch.getSourceType() != ReceiptSourceType.MEMBERSHIP_PREMIUM) {
            throw new IllegalArgumentException("Only membership premium payments can be edited");
        }
        if (batch.getStatus() != PaymentBatchStatus.POSTED) {
            throw new IllegalStateException("Only POSTED premium payments can be edited");
        }

        ReceiptEntity receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found: " + receiptId));
        if (!paymentBatchId.equals(receipt.getPaymentBatchId())) {
            throw new IllegalArgumentException("Receipt does not belong to payment batch " + paymentBatchId);
        }
        if (receipt.getStatus() != ReceiptStatus.POSTED) {
            throw new IllegalStateException("Only POSTED receipts can be edited");
        }

        List<ReceiptAllocationEntity> allocations = receiptAllocationRepository.findByReceiptId(receiptId).stream()
                .filter(item -> item.getStatus() == ReceiptStatus.POSTED)
                .filter(item -> item.getAllocationType() == ReceiptAllocationType.MEMBERSHIP_PREMIUM)
                .toList();
        if (allocations.size() != 1) {
            throw new IllegalStateException(
                    "This receipt does not have exactly one posted premium allocation and cannot be edited safely");
        }
        ReceiptAllocationEntity allocation = allocations.get(0);
        if (clean(allocation.getReferenceId()) == null) {
            throw new IllegalStateException("The premium allocation does not reference a premium row");
        }
        return new PaymentContext(batch, receipt, allocation);
    }

    private void validateTargetCapacity(
            PaymentContext context,
            String proposedPeriod,
            long proposedAmount,
            long currentAmount
    ) {
        MembershipEntity membership = membershipService.resolveMembership(context.batch().getMembershipId());
        long monthlyPremium = value(membership.getPremiumCents());
        if (monthlyPremium <= 0) {
            throw new IllegalStateException("Membership does not have a valid premium amount");
        }

        MembershipPremiumEntity currentPremium = membershipPremiumService.getById(context.allocation().getReferenceId());
        List<String> identifiers = membershipService.membershipIdentifiers(membership.getId());
        MembershipPremiumEntity targetPremium = membershipPremiumRepository
                .findByMembershipIdInAndPeriodYYYYMMOrderByMembershipIdAsc(identifiers, proposedPeriod)
                .stream()
                .findFirst()
                .orElse(null);
        long capacity = targetPremium == null ? monthlyPremium : value(targetPremium.getBalanceCents());
        if (targetPremium != null && targetPremium.getId().equals(currentPremium.getId())) {
            capacity += currentAmount;
        }
        if (proposedAmount > capacity) {
            throw new IllegalArgumentException(
                    "The requested amount exceeds the outstanding balance for period " + proposedPeriod);
        }
    }

    private void validateRequest(PremiumPaymentEditRequest request) {
        if (request == null) throw new IllegalArgumentException("Edit request is required");
        if (clean(request.getReceiptId()) == null) throw new IllegalArgumentException("receiptId is required");
        if (request.getAmountCents() == null || request.getAmountCents() <= 0) {
            throw new IllegalArgumentException("amountCents must be greater than zero");
        }
        if (!PeriodUtil.isValidPeriod(request.getPeriodYYYYMM())) {
            throw new IllegalArgumentException("periodYYYYMM is required and must use YYYYMM format");
        }
        if (clean(request.getRequestedBy()) == null) throw new IllegalArgumentException("requestedBy is required");
        if (clean(request.getReason()) == null) throw new IllegalArgumentException("A reason is required");
    }

    private Map<String, Object> requireEdit(String id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM premium_payment_edit_request WHERE id=?", id);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Premium payment edit request not found: " + id);
        }
        return rows.get(0);
    }

    private void markComplete(String id, String status, String actor) {
        jdbcTemplate.update("""
                UPDATE premium_payment_edit_request
                   SET status=?,completed_by=?,completed_at=CURRENT_TIMESTAMP
                 WHERE id=? AND status='PENDING_APPROVAL'
                """, status, actor, id);
    }

    private String auditNote(long oldAmount, long newAmount, String oldPeriod, String newPeriod, String actor) {
        return "Approved premium payment edit by " + actor
                + ": amount R " + String.format(java.util.Locale.ROOT, "%.2f", oldAmount / 100.0)
                + " -> R " + String.format(java.util.Locale.ROOT, "%.2f", newAmount / 100.0)
                + ", period " + oldPeriod + " -> " + newPeriod;
    }

    private String append(String existing, String note) {
        return clean(existing) == null ? note : existing.trim() + "\n" + note;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to build premium payment edit approval details", e);
        }
    }

    private long longValue(Object value) {
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(Objects.toString(value, "0"));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private record PaymentContext(
            PaymentBatchEntity batch,
            ReceiptEntity receipt,
            ReceiptAllocationEntity allocation
    ) {}
}
