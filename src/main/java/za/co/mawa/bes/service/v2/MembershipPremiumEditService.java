package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.dto.v2.MembershipPremiumEditRequest;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.entity.v2.MembershipPremiumEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.enums.PremiumStatus;
import za.co.mawa.bes.repository.v2.MembershipPremiumRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MembershipPremiumEditService {
    private final MembershipPremiumRepository premiumRepository;
    private final MembershipService membershipService;
    private final MembershipActionGuardService membershipActionGuardService;
    private final ApprovalService approvalService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public ApprovalRequestResponse requestEdit(
            String membershipId,
            String premiumId,
            MembershipPremiumEditRequest request,
            String actor) {
        validateRequest(request);
        MembershipEntity membership = membershipService.resolveMembership(membershipId);
        membershipActionGuardService.requireActionable(membership);
        MembershipPremiumEntity premium = requirePremium(membershipId, premiumId);
        requireEditable(premium);

        long currentAmount = value(premium.getAmountCents());
        long proposedAmount = request.getAmountCents();
        long paidAmount = value(premium.getPaidAmountCents());
        if (currentAmount == proposedAmount) {
            throw new IllegalArgumentException("Enter an amount different from the generated premium amount");
        }
        if (proposedAmount < paidAmount) {
            throw new IllegalArgumentException("Premium amount cannot be less than the amount already paid");
        }
        Integer pending = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM membership_premium_edit_request
                 WHERE premium_id=? AND status='PENDING_APPROVAL'
                """, Integer.class, premium.getId());
        if (pending != null && pending > 0) {
            throw new IllegalStateException("This premium already has an edit awaiting approval");
        }

        String id = UUID.randomUUID().toString();
        String actionBy = clean(actor) == null ? "SYSTEM" : actor.trim();
        jdbcTemplate.update("""
                INSERT INTO membership_premium_edit_request(
                    id,membership_id,premium_id,period_yyyymm,
                    previous_amount_cents,requested_amount_cents,paid_amount_cents,
                    status,reason,requested_by,created_at
                ) VALUES(?,?,?,?,?,?,?,'PENDING_APPROVAL',?,?,CURRENT_TIMESTAMP)
                """, id, membership.getId(), premium.getId(), premium.getPeriodYYYYMM(),
                currentAmount, proposedAmount, paidAmount, request.getReason().trim(), actionBy);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("membershipId", membership.getId());
        payload.put("membershipNumber", membership.getMembershipNo());
        payload.put("premiumId", premium.getId());
        payload.put("periodYYYYMM", premium.getPeriodYYYYMM());
        payload.put("currentValues", Map.of(
                "amountCents", currentAmount,
                "paidAmountCents", paidAmount,
                "balanceCents", value(premium.getBalanceCents()),
                "status", premium.getStatus().name()));
        payload.put("proposedValues", Map.of(
                "amountCents", proposedAmount,
                "balanceCents", proposedAmount - paidAmount,
                "status", proposedAmount == paidAmount ? "PAID" : paidAmount > 0 ? "PARTIALLY_PAID" : "UNPAID"));
        payload.put("reason", request.getReason().trim());
        payload.put("attachmentObjectIds", List.of(membership.getId()));

        ApprovalSubmitRequest approval = new ApprovalSubmitRequest();
        approval.setApprovalType(ApprovalType.MEMBERSHIP_PREMIUM_EDIT);
        approval.setReferenceId(id);
        approval.setReferenceNo(membership.getMembershipNo() + "-" + premium.getPeriodYYYYMM());
        approval.setTitle("Edit generated premium " + membership.getMembershipNo() + " - " + premium.getPeriodYYYYMM());
        approval.setDescription("Review the current and proposed amount for this generated membership premium period.");
        approval.setRequesterId(actionBy);
        approval.setPayloadJson(toJson(payload));
        ApprovalRequestResponse response = approvalService.submitForApproval(approval);
        jdbcTemplate.update("UPDATE membership_premium_edit_request SET approval_request_id=? WHERE id=?", response.getId(), id);
        return response;
    }

    @Transactional
    public void complete(String requestId, boolean approved, String actor, String completionStatus) {
        Map<String, Object> edit = requireEdit(requestId);
        if (!"PENDING_APPROVAL".equalsIgnoreCase(Objects.toString(edit.get("status"), ""))) return;
        String actionBy = clean(actor) == null ? "SYSTEM" : actor.trim();
        if (!approved) {
            markComplete(requestId, completionStatus, actionBy);
            return;
        }

        String membershipId = Objects.toString(edit.get("membership_id"), "");
        MembershipEntity membership = membershipService.resolveMembership(membershipId);
        membershipActionGuardService.requireActionable(membership);
        MembershipPremiumEntity premium = requirePremium(membershipId, Objects.toString(edit.get("premium_id"), ""));
        requireEditable(premium);
        long previousAmount = number(edit.get("previous_amount_cents"));
        long requestedAmount = number(edit.get("requested_amount_cents"));
        long currentPaid = value(premium.getPaidAmountCents());
        if (value(premium.getAmountCents()) != previousAmount) {
            throw new IllegalStateException("The premium amount changed after this request was submitted. Submit a new request.");
        }
        if (requestedAmount < currentPaid) {
            throw new IllegalStateException("Approved premium amount cannot be less than the amount already paid");
        }

        long balance = requestedAmount - currentPaid;
        premium.setAmountCents(requestedAmount);
        premium.setBalanceCents(balance);
        premium.setStatus(balance == 0 ? PremiumStatus.PAID : currentPaid > 0 ? PremiumStatus.PARTIALLY_PAID : PremiumStatus.UNPAID);
        premium.setUpdatedAt(LocalDateTime.now());
        premium.setUpdatedBy(actionBy);
        premiumRepository.saveAndFlush(premium);
        membershipService.recalculatePaidUpToPeriod(membership.getId());
        markComplete(requestId, completionStatus, actionBy);
    }

    private MembershipPremiumEntity requirePremium(String membershipId, String premiumId) {
        MembershipPremiumEntity premium = premiumRepository.findById(premiumId)
                .orElseThrow(() -> new IllegalArgumentException("Membership premium not found: " + premiumId));
        if (!membershipService.membershipIdentifiers(membershipId).contains(premium.getMembershipId())) {
            throw new IllegalArgumentException("Premium does not belong to membership: " + membershipId);
        }
        return premium;
    }

    private void requireEditable(MembershipPremiumEntity premium) {
        if (premium.getStatus() == PremiumStatus.CANCELLED || premium.getStatus() == PremiumStatus.REVERSED) {
            throw new IllegalStateException("Cancelled or reversed premiums cannot be edited");
        }
    }

    private void validateRequest(MembershipPremiumEditRequest request) {
        if (request == null || request.getAmountCents() == null || request.getAmountCents() <= 0) {
            throw new IllegalArgumentException("Premium amount must be greater than zero");
        }
        if (clean(request.getReason()) == null) throw new IllegalArgumentException("A reason is required");
    }

    private Map<String, Object> requireEdit(String id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM membership_premium_edit_request WHERE id=?", id);
        if (rows.isEmpty()) throw new IllegalArgumentException("Membership premium edit request not found: " + id);
        return rows.get(0);
    }

    private void markComplete(String id, String status, String actor) {
        jdbcTemplate.update("""
                UPDATE membership_premium_edit_request
                   SET status=?,completed_by=?,completed_at=CURRENT_TIMESTAMP
                 WHERE id=? AND status='PENDING_APPROVAL'
                """, status, actor, id);
    }

    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception e) { throw new IllegalStateException("Unable to build membership premium edit approval details", e); }
    }

    private long number(Object value) {
        if (value instanceof Number number) return number.longValue();
        try { return Long.parseLong(Objects.toString(value, "0")); }
        catch (NumberFormatException e) { return 0L; }
    }

    private long value(Long value) { return value == null ? 0L : value; }
    private String clean(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
}
