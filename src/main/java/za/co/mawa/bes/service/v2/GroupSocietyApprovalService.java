package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.dto.v2.group.GroupSocietyAdjustmentRequest;
import za.co.mawa.bes.dto.v2.group.GroupSocietyStatusChangeRequest;
import za.co.mawa.bes.entity.AttachmentEntity;
import za.co.mawa.bes.entity.v2.GroupSocietyAccountTxnEntity;
import za.co.mawa.bes.entity.v2.GroupSocietyEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.repository.AttachmentRepository;
import za.co.mawa.bes.repository.v2.GroupSocietyAccountTxnRepository;
import za.co.mawa.bes.repository.v2.GroupSocietyRepository;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GroupSocietyApprovalService {
    private final GroupSocietyRepository groupSocietyRepository;
    private final GroupSocietyAccountTxnRepository txnRepository;
    private final AttachmentRepository attachmentRepository;
    private final ApprovalService approvalService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public GroupSocietyEntity requestStatus(String groupSocietyId, String targetStatus,
                                            GroupSocietyStatusChangeRequest request) {
        String target = normaliseStatus(targetStatus);
        GroupSocietyEntity society = groupSocietyRepository.findByIdForUpdate(groupSocietyId)
                .orElseThrow(() -> new IllegalArgumentException("Group society not found: " + groupSocietyId));
        if (society.getPendingAction() != null && !society.getPendingAction().isBlank()) {
            throw new IllegalStateException("The group society already has an approval request in progress");
        }
        if (target.equalsIgnoreCase(society.getStatus())) {
            throw new IllegalArgumentException("The group society is already " + target);
        }
        if (Set.of("SUSPENDED", "CLOSED").contains(target)) {
            validateSupportingDocuments(groupSocietyId,
                    request == null ? null : request.getSupportingAttachmentIds());
        }
        String actor = actor(request == null ? null : request.getRequestedBy());
        String actionId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO group_society_approval_action(
                    id,group_society_id,action_type,previous_status,requested_status,
                    status,notes,requested_by,created_at
                ) VALUES(?,?,'STATUS_CHANGE',?,?,'PENDING_APPROVAL',?,?,CURRENT_TIMESTAMP)
                """, actionId, groupSocietyId, society.getStatus(), target,
                request == null ? null : request.getNotes(), actor);

        ApprovalSubmitRequest approval = new ApprovalSubmitRequest();
        approval.setApprovalType(ApprovalType.GROUP_SOCIETY_STATUS_CHANGE);
        approval.setReferenceId(actionId);
        approval.setReferenceNo(society.getGroupNo());
        approval.setTitle("Group society " + target.toLowerCase(Locale.ROOT) + " request");
        approval.setDescription("Approval required to change group society " + society.getGroupNo()
                + " from " + society.getStatus() + " to " + target);
        approval.setRequesterId(actor);
        approval.setPayloadJson("{\"groupSocietyId\":\"" + groupSocietyId
                + "\",\"requestedStatus\":\"" + target
                + "\",\"supportingAttachmentIds\":"
                + jsonStringArray(request == null ? null : request.getSupportingAttachmentIds()) + "}");
        var response = approvalService.submitForApproval(approval);

        jdbcTemplate.update("UPDATE group_society_approval_action SET approval_request_id=? WHERE id=?",
                response.getId(), actionId);
        society.setPreviousStatus(society.getStatus());
        society.setRequestedStatus(target);
        society.setPendingAction("STATUS_CHANGE");
        society.setApprovalRequestId(response.getId());
        society.setStatus("PENDING_" + switch (target) {
            case "CLOSED" -> "CLOSURE";
            case "SUSPENDED" -> "SUSPENSION";
            default -> target;
        });
        society.setUpdatedBy(actor);
        return groupSocietyRepository.save(society);
    }

    @Transactional
    public GroupSocietyAccountTxnEntity requestAdjustment(String groupSocietyId,
                                                          GroupSocietyAdjustmentRequest request) {
        if (request == null || request.getAmountCents() == null || request.getAmountCents() <= 0) {
            throw new IllegalArgumentException("amountCents must be greater than zero");
        }
        String direction = request.getDirection() == null ? "" : request.getDirection().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("CREDIT", "DEBIT").contains(direction)) {
            throw new IllegalArgumentException("direction must be CREDIT or DEBIT");
        }
        validateSupportingDocuments(groupSocietyId, request.getSupportingAttachmentIds());
        GroupSocietyEntity society = groupSocietyRepository.findByIdForUpdate(groupSocietyId)
                .orElseThrow(() -> new IllegalArgumentException("Group society not found: " + groupSocietyId));
        if (!"ACTIVE".equalsIgnoreCase(society.getStatus())) {
            throw new IllegalStateException("Balance adjustment can only be requested for an ACTIVE group society");
        }
        long before = value(society.getAvailableBalanceCents());
        if ("DEBIT".equals(direction) && before < request.getAmountCents()) {
            throw new IllegalArgumentException("Insufficient balance for debit adjustment");
        }
        long proposed = "CREDIT".equals(direction)
                ? before + request.getAmountCents()
                : before - request.getAmountCents();
        String actor = actor(request.getRequestedBy());

        GroupSocietyAccountTxnEntity txn = new GroupSocietyAccountTxnEntity();
        txn.setGroupSocietyId(groupSocietyId);
        txn.setTxnType("ADJUSTMENT_" + direction);
        txn.setDirection(direction);
        txn.setAmountCents(request.getAmountCents());
        txn.setBalanceBeforeCents(before);
        txn.setBalanceAfterCents(proposed);
        txn.setTxnDate(request.getAdjustmentDate() == null ? LocalDate.now() : request.getAdjustmentDate());
        txn.setReferenceType("MANUAL_ADJUSTMENT");
        txn.setReferenceNo(request.getReferenceNo());
        txn.setNotes(request.getNotes());
        txn.setStatus("PENDING_APPROVAL");
        txn.setRequestedBy(actor);
        txn.setCreatedBy(actor);
        txn = txnRepository.save(txn);

        ApprovalSubmitRequest approval = new ApprovalSubmitRequest();
        approval.setApprovalType(ApprovalType.GROUP_SOCIETY_BALANCE_ADJUSTMENT);
        approval.setReferenceId(txn.getId());
        approval.setReferenceNo(society.getGroupNo());
        approval.setTitle("Group society balance adjustment");
        approval.setDescription(direction + " adjustment of " + request.getAmountCents()
                + " cents for group society " + society.getGroupNo());
        approval.setRequesterId(actor);
        approval.setPayloadJson("{\"groupSocietyId\":\"" + groupSocietyId
                + "\",\"direction\":\"" + direction
                + "\",\"amountCents\":" + request.getAmountCents()
                + ",\"supportingAttachmentIds\":"
                + jsonStringArray(request.getSupportingAttachmentIds()) + "}");
        var response = approvalService.submitForApproval(approval);
        txn.setApprovalRequestId(response.getId());
        return txnRepository.save(txn);
    }

    @Transactional
    public void completeStatus(String actionId, boolean approved, String actor) {
        Map<String,Object> action = requireAction(actionId, "STATUS_CHANGE");
        if (!"PENDING_APPROVAL".equalsIgnoreCase(Objects.toString(action.get("status"), ""))) {
            return;
        }
        String societyId = Objects.toString(action.get("group_society_id"));
        GroupSocietyEntity society = groupSocietyRepository.findByIdForUpdate(societyId)
                .orElseThrow(() -> new IllegalArgumentException("Group society not found: " + societyId));
        String target = Objects.toString(action.get("requested_status"));
        String previous = Objects.toString(action.get("previous_status"));
        society.setStatus(approved ? target : previous);
        society.setPendingAction(null);
        society.setRequestedStatus(null);
        society.setPreviousStatus(null);
        society.setApprovalRequestId(null);
        society.setUpdatedBy(actor);
        groupSocietyRepository.save(society);
        jdbcTemplate.update("""
                UPDATE group_society_approval_action
                   SET status=?,completed_by=?,completed_at=CURRENT_TIMESTAMP
                 WHERE id=? AND status='PENDING_APPROVAL'
                """, approved ? "APPROVED" : "REJECTED", actor, actionId);
    }

    @Transactional
    public void completeAdjustment(String transactionId, boolean approved, String actor) {
        GroupSocietyAccountTxnEntity txn = txnRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Group society adjustment not found: " + transactionId));
        if (!"PENDING_APPROVAL".equalsIgnoreCase(txn.getStatus())) return;
        if (!approved) {
            txn.setStatus("REJECTED");
            txn.setNotes(append(txn.getNotes(), "Approval rejected by " + actor));
            txnRepository.save(txn);
            return;
        }
        GroupSocietyEntity society = groupSocietyRepository.findByIdForUpdate(txn.getGroupSocietyId())
                .orElseThrow(() -> new IllegalArgumentException("Group society not found: " + txn.getGroupSocietyId()));
        if (!"ACTIVE".equalsIgnoreCase(society.getStatus())) {
            throw new IllegalStateException("The group society must be ACTIVE before the adjustment can be approved");
        }
        long before = value(society.getAvailableBalanceCents());
        long amount = value(txn.getAmountCents());
        long after;
        if ("CREDIT".equalsIgnoreCase(txn.getDirection())) {
            after = before + amount;
        } else {
            if (before < amount) throw new IllegalStateException("Insufficient balance for the approved debit adjustment");
            after = before - amount;
        }
        society.setAvailableBalanceCents(after);
        society.setUpdatedBy(actor);
        groupSocietyRepository.save(society);
        txn.setBalanceBeforeCents(before);
        txn.setBalanceAfterCents(after);
        txn.setStatus("POSTED");
        txn.setNotes(append(txn.getNotes(), "Approved by " + actor));
        txnRepository.save(txn);
    }

    private void validateSupportingDocuments(String groupSocietyId, List<String> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            throw new IllegalArgumentException("At least one supporting document must be attached");
        }
        Set<String> supplied = new HashSet<>(attachmentIds);
        List<AttachmentEntity> attachments = attachmentRepository.findAllById(supplied);
        boolean valid = attachments.size() == supplied.size()
                && attachments.stream().allMatch(item -> groupSocietyId.equals(item.getObjectId()));
        if (!valid) {
            throw new IllegalArgumentException("Every supporting attachment must exist and belong to this group society");
        }
    }

    private Map<String,Object> requireAction(String id, String type) {
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM group_society_approval_action WHERE id=? AND action_type=?", id, type);
        if (rows.isEmpty()) throw new IllegalArgumentException("Group society approval action not found: " + id);
        return rows.get(0);
    }

    private String normaliseStatus(String value) {
        String status = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ACTIVE", "SUSPENDED", "CLOSED").contains(status)) {
            throw new IllegalArgumentException("Status must be ACTIVE, SUSPENDED or CLOSED");
        }
        return status;
    }

    private String jsonStringArray(List<String> values) {
        if (values == null || values.isEmpty()) return "[]";
        return values.stream()
                .map(value -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private String actor(String value) { return value == null || value.isBlank() ? "SYSTEM" : value.trim(); }
    private long value(Long value) { return value == null ? 0L : value; }
    private String append(String current, String value) {
        return current == null || current.isBlank() ? value : current + "\n" + value;
    }
}
