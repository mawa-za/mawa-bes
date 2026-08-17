package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.dto.v2.MembershipStatusChangeRequest;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.repository.v2.MembershipRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MembershipStatusChangeService {
    private static final Set<String> ACTIONS = Set.of("REACTIVATE", "DEACTIVATE", "SUSPEND", "CANCEL");

    private final MembershipRepository membershipRepository;
    private final ApprovalService approvalService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public Map<String, Object> pending(String membershipId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT action_type,requested_status,reason,requested_by,created_at,approval_request_id
                  FROM membership_status_change_request
                 WHERE membership_id=? AND status='PENDING_APPROVAL'
                 ORDER BY created_at DESC
                 LIMIT 1
                """, membershipId);
        if (rows.isEmpty()) {
            return Map.of("pending", false);
        }
        Map<String, Object> row = rows.get(0);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("pending", true);
        response.put("action", Objects.toString(row.get("action_type"), ""));
        response.put("requestedStatus", Objects.toString(row.get("requested_status"), ""));
        response.put("reason", Objects.toString(row.get("reason"), ""));
        response.put("requestedBy", Objects.toString(row.get("requested_by"), ""));
        response.put("createdAt", row.get("created_at"));
        response.put("approvalRequestId", row.get("approval_request_id"));
        return response;
    }

    @Transactional
    public ApprovalRequestResponse request(
            String membershipId,
            String actionValue,
            MembershipStatusChangeRequest request,
            String fallbackActor
    ) {
        String action = normalizeAction(actionValue);
        MembershipEntity membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + membershipId));
        String currentStatus = normalizeStatus(membership.getStatus());
        String targetStatus = targetStatus(action);
        validateTransition(currentStatus, action, targetStatus);

        Integer pending = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM membership_status_change_request
                 WHERE membership_id=? AND status='PENDING_APPROVAL'
                """, Integer.class, membershipId);
        if (pending != null && pending > 0) {
            throw new IllegalStateException("This membership already has a status change awaiting approval");
        }

        String reason = request == null ? null : clean(request.getReason());
        if (reason == null) {
            throw new IllegalArgumentException("A reason is required");
        }
        String actor = clean(request == null ? null : request.getRequestedBy());
        if (actor == null) actor = clean(fallbackActor);
        if (actor == null) actor = "SYSTEM";

        String actionId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO membership_status_change_request(
                    id,membership_id,membership_no,action_type,previous_status,requested_status,
                    status,reason,requested_by,created_at
                ) VALUES(?,?,?,?,?,?,'PENDING_APPROVAL',?,?,CURRENT_TIMESTAMP)
                """,
                actionId,
                membership.getId(),
                membership.getMembershipNo(),
                action,
                currentStatus,
                targetStatus,
                reason,
                actor);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("membershipId", membership.getId());
        payload.put("membershipNumber", membership.getMembershipNo());
        payload.put("memberId", membership.getMemberId());
        payload.put("action", action);
        payload.put("currentValues", Map.of("status", currentStatus));
        payload.put("proposedValues", Map.of("status", targetStatus));
        payload.put("reason", reason);
        List<String> attachmentObjectIds = new java.util.ArrayList<>();
        if (clean(membership.getId()) != null) attachmentObjectIds.add(membership.getId());
        if (clean(membership.getMemberId()) != null) attachmentObjectIds.add(membership.getMemberId());
        payload.put("attachmentObjectIds", attachmentObjectIds);

        ApprovalSubmitRequest approval = new ApprovalSubmitRequest();
        approval.setApprovalType(ApprovalType.MEMBERSHIP_STATUS_CHANGE);
        approval.setReferenceId(actionId);
        approval.setReferenceNo(membership.getMembershipNo());
        approval.setTitle(actionLabel(action) + " membership - " + membership.getMembershipNo());
        approval.setDescription("Review the current and requested membership status before approval.");
        approval.setRequesterId(actor);
        approval.setPayloadJson(toJson(payload));
        ApprovalRequestResponse response = approvalService.submitForApproval(approval);

        jdbcTemplate.update(
                "UPDATE membership_status_change_request SET approval_request_id=? WHERE id=?",
                response.getId(), actionId);
        return response;
    }

    @Transactional
    public void complete(String actionId, boolean approved, String actor, String completionStatus) {
        Map<String, Object> action = requireAction(actionId);
        if (!"PENDING_APPROVAL".equalsIgnoreCase(Objects.toString(action.get("status"), ""))) {
            return;
        }

        String membershipId = Objects.toString(action.get("membership_id"), "");
        MembershipEntity membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + membershipId));
        String previous = normalizeStatus(Objects.toString(action.get("previous_status"), ""));
        String requested = normalizeStatus(Objects.toString(action.get("requested_status"), ""));

        if (approved) {
            String current = normalizeStatus(membership.getStatus());
            if (!current.equals(previous)) {
                throw new IllegalStateException(
                        "Membership status changed after this request was submitted. Current status is " + current);
            }
            membership.setStatus(requested);
            membership.setUpdatedAt(LocalDateTime.now());
            membership.setUpdatedBy(clean(actor) == null ? "SYSTEM" : actor.trim());
            membershipRepository.save(membership);
        }

        jdbcTemplate.update("""
                UPDATE membership_status_change_request
                   SET status=?,completed_by=?,completed_at=CURRENT_TIMESTAMP
                 WHERE id=? AND status='PENDING_APPROVAL'
                """,
                completionStatus,
                clean(actor) == null ? "SYSTEM" : actor.trim(),
                actionId);
    }

    private Map<String, Object> requireAction(String id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM membership_status_change_request WHERE id=?", id);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Membership status change request not found: " + id);
        }
        return rows.get(0);
    }

    private String normalizeAction(String value) {
        String action = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!ACTIONS.contains(action)) {
            throw new IllegalArgumentException("Action must be REACTIVATE, DEACTIVATE, SUSPEND or CANCEL");
        }
        return action;
    }

    private String normalizeStatus(String value) {
        String status = value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (status.isEmpty()) return "INACTIVE";
        if ("CANCELED".equals(status)) return "CANCELLED";
        return status;
    }

    private String targetStatus(String action) {
        return switch (action) {
            case "REACTIVATE" -> "ACTIVE";
            case "DEACTIVATE" -> "INACTIVE";
            case "SUSPEND" -> "SUSPENDED";
            case "CANCEL" -> "CANCELLED";
            default -> throw new IllegalArgumentException("Unsupported membership action: " + action);
        };
    }

    private void validateTransition(String current, String action, String target) {
        if (current.equals(target)) {
            throw new IllegalArgumentException("Membership is already " + target);
        }
        if (current.startsWith("PENDING_")) {
            throw new IllegalStateException("Membership status cannot be changed while another membership approval is pending");
        }
        if ("LAPSED".equals(current) && !"REACTIVATE".equals(action)) {
            throw new IllegalStateException("A lapsed membership can only be reactivated");
        }
        boolean allowed = switch (action) {
            case "REACTIVATE" -> Set.of("INACTIVE", "SUSPENDED", "CANCELLED", "LAPSED").contains(current);
            case "DEACTIVATE" -> Set.of("ACTIVE", "SUSPENDED").contains(current);
            case "SUSPEND" -> Set.of("ACTIVE", "INACTIVE").contains(current);
            case "CANCEL" -> !"CANCELLED".equals(current);
            default -> false;
        };
        if (!allowed) {
            throw new IllegalStateException("Cannot " + action.toLowerCase(Locale.ROOT)
                    + " a membership with status " + current);
        }
    }

    private String actionLabel(String action) {
        return switch (action) {
            case "REACTIVATE" -> "Reactivate";
            case "DEACTIVATE" -> "Deactivate";
            case "SUSPEND" -> "Suspend";
            case "CANCEL" -> "Cancel";
            default -> action;
        };
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to build membership status approval details", e);
        }
    }

    private String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
