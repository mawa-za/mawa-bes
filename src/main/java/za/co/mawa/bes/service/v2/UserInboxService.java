package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.inbox.InboxCountsResponse;
import za.co.mawa.bes.dto.v2.inbox.UserInboxResponse;
import za.co.mawa.bes.dto.v2.inbox.UserNotificationResponse;
import za.co.mawa.bes.entity.v2.ApprovalActionEntity;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowStepApproverEntity;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowStepEntity;
import za.co.mawa.bes.enums.ApprovalStatus;
import za.co.mawa.bes.enums.UserNotificationType;
import za.co.mawa.bes.repository.v2.ApprovalRequestRepository;
import za.co.mawa.bes.repository.v2.ApprovalWorkflowStepRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserInboxService {

    private final JdbcTemplate jdbcTemplate;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalWorkflowStepRepository workflowStepRepository;
    private final ApprovalApproverScopeService approverScopeService;

    @Transactional
    public void notifyApprovalRequired(ApprovalRequestEntity request) {
        ApprovalWorkflowStepEntity step = currentStep(request);
        for (String userId : resolveApproverUserIds(step, request)) {
            createNotification(
                    userId,
                    request.getId() + ":STEP:" + step.getStepNo(),
                    UserNotificationType.APPROVAL_REQUIRED,
                    "Approval required",
                    requiredMessage(request, step),
                    request,
                    step.getStepNo(),
                    null,
                    "/inbox"
            );
        }
    }

    @Transactional
    public void notifyRequesterActioned(
            ApprovalRequestEntity request,
            ApprovalActionEntity action,
            String actor
    ) {
        String requester = canonicalUserId(request.getRequesterId());
        String canonicalActor = canonicalUserId(actor);
        if (requester == null || requester.isBlank() || requester.equals(canonicalActor)) {
            return;
        }

        String actorName = displayName(canonicalActor);
        String actionLabel = action.getAction().name().toLowerCase(Locale.ROOT);
        String status = request.getStatus().name().replace('_', ' ').toLowerCase(Locale.ROOT);
        String message;
        if (request.getStatus() == ApprovalStatus.IN_PROGRESS || request.getStatus() == ApprovalStatus.PENDING) {
            message = "%s was %s by %s and is still in progress at step %d."
                    .formatted(referenceLabel(request), actionLabel, actorName, request.getCurrentStepNo());
        } else {
            message = "%s was %s by %s. Final status: %s."
                    .formatted(referenceLabel(request), actionLabel, actorName, status);
        }
        if (action.getComments() != null && !action.getComments().isBlank()) {
            message += " Comment: " + action.getComments().trim();
        }

        createNotification(
                requester,
                request.getId() + ":ACTION:" + action.getId(),
                UserNotificationType.APPROVAL_ACTIONED,
                "Approval request actioned",
                message,
                request,
                action.getStepNo(),
                canonicalActor,
                "/approvals?request=" + request.getId()
        );
    }

    @Transactional
    public void resolveApprovalStep(String approvalRequestId, Integer stepNo) {
        jdbcTemplate.update("""
                UPDATE user_notification
                   SET resolved_at = COALESCE(resolved_at, CURRENT_TIMESTAMP(6)),
                       read_at = COALESCE(read_at, CURRENT_TIMESTAMP(6))
                 WHERE approval_request_id = ?
                   AND approval_step_no = ?
                   AND notification_type = 'APPROVAL_REQUIRED'
                   AND resolved_at IS NULL
                """, approvalRequestId, stepNo);
    }

    @Transactional
    public void resolveApprovalStepForUser(String approvalRequestId, Integer stepNo, String userId) {
        String canonical = canonicalUserId(userId);
        jdbcTemplate.update("""
                UPDATE user_notification
                   SET resolved_at = COALESCE(resolved_at, CURRENT_TIMESTAMP(6)),
                       read_at = COALESCE(read_at, CURRENT_TIMESTAMP(6))
                 WHERE approval_request_id = ?
                   AND approval_step_no = ?
                   AND user_id = ?
                   AND notification_type = 'APPROVAL_REQUIRED'
                   AND resolved_at IS NULL
                """, approvalRequestId, stepNo, canonical);
    }

    @Transactional
    public UserInboxResponse getInbox(String userIdentity, int limit) {
        String userId = requireCanonicalUser(userIdentity);
        List<ApprovalRequestResponse> pending = assignedApprovals(userId);
        reconcileRequiredNotifications(userId, pending);
        List<UserNotificationResponse> notifications = queryNotifications(userId, normaliseLimit(limit), false);
        long unread = unreadCount(userId);
        return UserInboxResponse.builder()
                .userId(userId)
                .unreadCount(unread)
                .pendingApprovalCount(pending.size())
                .pendingApprovals(pending)
                .notifications(notifications)
                .build();
    }

    @Transactional(readOnly = true)
    public InboxCountsResponse getCounts(String userIdentity) {
        String userId = requireCanonicalUser(userIdentity);
        return InboxCountsResponse.builder()
                .unreadCount(unreadCount(userId))
                .pendingApprovalCount(assignedApprovalCount(userId))
                .build();
    }

    @Transactional
    public void markRead(String userIdentity, String notificationId) {
        String userId = requireCanonicalUser(userIdentity);
        int updated = jdbcTemplate.update("""
                UPDATE user_notification
                   SET read_at = COALESCE(read_at, CURRENT_TIMESTAMP(6))
                 WHERE id = ? AND user_id = ?
                """, notificationId, userId);
        if (updated == 0) {
            throw new RuntimeException("Notification not found");
        }
    }

    @Transactional
    public void markAllRead(String userIdentity) {
        String userId = requireCanonicalUser(userIdentity);
        jdbcTemplate.update("""
                UPDATE user_notification
                   SET read_at = COALESCE(read_at, CURRENT_TIMESTAMP(6))
                 WHERE user_id = ? AND read_at IS NULL
                """, userId);
    }

    private void reconcileRequiredNotifications(String userId, List<ApprovalRequestResponse> pending) {
        Set<String> pendingIds = new LinkedHashSet<>();
        Set<String> activeEventKeys = new LinkedHashSet<>(jdbcTemplate.queryForList("""
                SELECT event_key
                  FROM user_notification
                 WHERE user_id = ?
                   AND notification_type = 'APPROVAL_REQUIRED'
                   AND resolved_at IS NULL
                """, String.class, userId));

        for (ApprovalRequestResponse response : pending) {
            pendingIds.add(response.getId());
            String eventKey = response.getId() + ":STEP:" + response.getCurrentStepNo();
            if (!activeEventKeys.contains(eventKey)) {
                approvalRequestRepository.findById(response.getId())
                        .ifPresent(request -> notifyApprovalRequiredForUser(request, userId));
            }
        }

        List<String> unresolvedApprovalIds = jdbcTemplate.queryForList("""
                SELECT DISTINCT approval_request_id
                  FROM user_notification
                 WHERE user_id = ?
                   AND notification_type = 'APPROVAL_REQUIRED'
                   AND resolved_at IS NULL
                   AND approval_request_id IS NOT NULL
                """, String.class, userId);
        for (String approvalId : unresolvedApprovalIds) {
            if (!pendingIds.contains(approvalId)) {
                jdbcTemplate.update("""
                        UPDATE user_notification
                           SET resolved_at = COALESCE(resolved_at, CURRENT_TIMESTAMP(6)),
                               read_at = COALESCE(read_at, CURRENT_TIMESTAMP(6))
                         WHERE user_id = ? AND approval_request_id = ?
                           AND notification_type = 'APPROVAL_REQUIRED'
                           AND resolved_at IS NULL
                        """, userId, approvalId);
            }
        }
    }

    private List<ApprovalRequestResponse> assignedApprovals(String userId) {
        List<String> ids = jdbcTemplate.queryForList("""
                SELECT ar.id
                  FROM approval_request ar
                  JOIN approval_workflow_step step
                    ON step.workflow_id = ar.workflow_id
                   AND step.step_no = ar.current_step_no
                   AND step.active = 1
                  JOIN approval_workflow_step_approver approver
                    ON approver.workflow_step_id = step.id
                   AND approver.active = 1
                  JOIN `user` inbox_user ON inbox_user.id = ?
                 WHERE ar.status IN ('PENDING', 'IN_PROGRESS')
                   AND inbox_user.status = 'ACTIVE'
                   AND (inbox_user.expires_at IS NULL OR inbox_user.expires_at > NOW())
                   AND (
                        ar.approval_type <> 'LEAVE'
                        OR approver.assignment_scope_type IS NULL
                        OR approver.assignment_scope_type = ''
                        OR UPPER(approver.assignment_scope_type) = 'ALL'
                        OR (UPPER(approver.assignment_scope_type) = 'POSITION' AND EXISTS (
                            SELECT 1 FROM leave_request scoped_lr
                            JOIN employment scoped_e ON scoped_e.id = scoped_lr.employment_id
                            WHERE scoped_lr.id = ar.reference_id
                              AND UPPER(scoped_e.position) = UPPER(approver.assignment_scope_value)
                        ))
                        OR (UPPER(approver.assignment_scope_type) = 'EMPLOYEE' AND EXISTS (
                            SELECT 1 FROM leave_request scoped_lr
                            JOIN employment scoped_e ON scoped_e.id = scoped_lr.employment_id
                            WHERE scoped_lr.id = ar.reference_id
                              AND (scoped_e.id = approver.assignment_scope_value
                                   OR scoped_e.partner_id = approver.assignment_scope_value)
                        ))
                   )
                   AND (
                        (approver.approver_type = 'USER' AND (
                            approver.approver_value = inbox_user.id OR
                            approver.approver_value = inbox_user.username OR
                            approver.approver_value = inbox_user.email
                        ))
                        OR
                        (approver.approver_type = 'ROLE' AND EXISTS (
                            SELECT 1
                              FROM user_role ur
                              JOIN role r ON r.id = ur.role
                             WHERE ur.user = inbox_user.id
                               AND (r.id = approver.approver_value OR
                                    UPPER(r.description) = UPPER(approver.approver_value))
                               AND (ur.valid_from IS NULL OR ur.valid_from <= CURRENT_DATE)
                               AND (ur.valid_to IS NULL OR ur.valid_to >= CURRENT_DATE)
                        ))
                        OR
                        (approver.approver_type = 'GROUP' AND EXISTS (
                            SELECT 1
                              FROM approval_group_member gm
                             WHERE gm.user_id = inbox_user.id
                               AND gm.group_code = approver.approver_value
                               AND gm.active = 1
                        ))
                        OR
                        (approver.approver_type = 'MANAGER' AND EXISTS (
                            SELECT 1
                              FROM approval_manager_assignment ma
                              LEFT JOIN `user` requester_user ON requester_user.id = ma.requester_user_id
                             WHERE ma.manager_user_id = inbox_user.id
                               AND ma.active = 1
                               AND (
                                    ma.requester_user_id = ar.requester_id OR
                                    requester_user.username = ar.requester_id OR
                                    requester_user.email = ar.requester_id OR
                                    requester_user.partner = ar.requester_id
                               )
                        ))
                   )
                   AND NOT EXISTS (
                        SELECT 1
                          FROM approval_action actioned
                         WHERE actioned.approval_request_id = ar.id
                           AND actioned.step_no = ar.current_step_no
                           AND actioned.action IN ('APPROVED', 'REJECTED')
                           AND (
                                actioned.action_by = inbox_user.id OR
                                actioned.action_by = inbox_user.username OR
                                actioned.action_by = inbox_user.email OR
                                actioned.action_by = inbox_user.partner
                           )
                   )
                 GROUP BY ar.id, ar.created_at
                 ORDER BY ar.created_at DESC
                """, String.class, userId);

        if (ids.isEmpty()) return List.of();
        var byId = approvalRequestRepository.findAllById(ids).stream()
                .collect(java.util.stream.Collectors.toMap(ApprovalRequestEntity::getId, entity -> entity));
        return ids.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .map(this::toResponse)
                .toList();
    }

    private long assignedApprovalCount(String userId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT ar.id)
                  FROM approval_request ar
                  JOIN approval_workflow_step step
                    ON step.workflow_id = ar.workflow_id
                   AND step.step_no = ar.current_step_no
                   AND step.active = 1
                  JOIN approval_workflow_step_approver approver
                    ON approver.workflow_step_id = step.id
                   AND approver.active = 1
                  JOIN `user` inbox_user ON inbox_user.id = ?
                 WHERE ar.status IN ('PENDING', 'IN_PROGRESS')
                   AND inbox_user.status = 'ACTIVE'
                   AND (inbox_user.expires_at IS NULL OR inbox_user.expires_at > NOW())
                   AND (
                        ar.approval_type <> 'LEAVE'
                        OR approver.assignment_scope_type IS NULL
                        OR approver.assignment_scope_type = ''
                        OR UPPER(approver.assignment_scope_type) = 'ALL'
                        OR (UPPER(approver.assignment_scope_type) = 'POSITION' AND EXISTS (
                            SELECT 1 FROM leave_request scoped_lr
                            JOIN employment scoped_e ON scoped_e.id = scoped_lr.employment_id
                            WHERE scoped_lr.id = ar.reference_id
                              AND UPPER(scoped_e.position) = UPPER(approver.assignment_scope_value)
                        ))
                        OR (UPPER(approver.assignment_scope_type) = 'EMPLOYEE' AND EXISTS (
                            SELECT 1 FROM leave_request scoped_lr
                            JOIN employment scoped_e ON scoped_e.id = scoped_lr.employment_id
                            WHERE scoped_lr.id = ar.reference_id
                              AND (scoped_e.id = approver.assignment_scope_value
                                   OR scoped_e.partner_id = approver.assignment_scope_value)
                        ))
                   )
                   AND (
                        (approver.approver_type = 'USER' AND (
                            approver.approver_value = inbox_user.id OR
                            approver.approver_value = inbox_user.username OR
                            approver.approver_value = inbox_user.email
                        ))
                        OR
                        (approver.approver_type = 'ROLE' AND EXISTS (
                            SELECT 1
                              FROM user_role ur
                              JOIN role r ON r.id = ur.role
                             WHERE ur.user = inbox_user.id
                               AND (r.id = approver.approver_value OR
                                    UPPER(r.description) = UPPER(approver.approver_value))
                               AND (ur.valid_from IS NULL OR ur.valid_from <= CURRENT_DATE)
                               AND (ur.valid_to IS NULL OR ur.valid_to >= CURRENT_DATE)
                        ))
                        OR
                        (approver.approver_type = 'GROUP' AND EXISTS (
                            SELECT 1
                              FROM approval_group_member gm
                             WHERE gm.user_id = inbox_user.id
                               AND gm.group_code = approver.approver_value
                               AND gm.active = 1
                        ))
                        OR
                        (approver.approver_type = 'MANAGER' AND EXISTS (
                            SELECT 1
                              FROM approval_manager_assignment ma
                              LEFT JOIN `user` requester_user ON requester_user.id = ma.requester_user_id
                             WHERE ma.manager_user_id = inbox_user.id
                               AND ma.active = 1
                               AND (
                                    ma.requester_user_id = ar.requester_id OR
                                    requester_user.username = ar.requester_id OR
                                    requester_user.email = ar.requester_id OR
                                    requester_user.partner = ar.requester_id
                               )
                        ))
                   )
                   AND NOT EXISTS (
                        SELECT 1
                          FROM approval_action actioned
                         WHERE actioned.approval_request_id = ar.id
                           AND actioned.step_no = ar.current_step_no
                           AND actioned.action IN ('APPROVED', 'REJECTED')
                           AND (
                                actioned.action_by = inbox_user.id OR
                                actioned.action_by = inbox_user.username OR
                                actioned.action_by = inbox_user.email OR
                                actioned.action_by = inbox_user.partner
                           )
                   )
                """, Long.class, userId);
        return count == null ? 0 : count;
    }

    private void notifyApprovalRequiredForUser(ApprovalRequestEntity request, String userId) {
        ApprovalWorkflowStepEntity step = currentStep(request);
        createNotification(
                userId,
                request.getId() + ":STEP:" + step.getStepNo(),
                UserNotificationType.APPROVAL_REQUIRED,
                "Approval required",
                requiredMessage(request, step),
                request,
                step.getStepNo(),
                null,
                "/inbox"
        );
    }

    @Transactional(readOnly = true)
    public List<UserNotificationResponse> getNotifications(String userIdentity, int limit, boolean unreadOnly) {
        String userId = requireCanonicalUser(userIdentity);
        return queryNotifications(userId, normaliseLimit(limit), unreadOnly);
    }

    private List<UserNotificationResponse> queryNotifications(String userId, int limit, boolean unreadOnly) {
        return jdbcTemplate.query("""
                SELECT n.id, n.notification_type, n.title, n.message,
                       n.approval_request_id, n.approval_step_no, n.approval_type,
                       n.approval_status, n.reference_id, n.reference_no, n.action_by,
                       n.route, n.read_at, n.resolved_at, n.created_at,
                       COALESCE(u.username, n.action_by) AS action_by_display_name
                  FROM user_notification n
                  LEFT JOIN `user` u ON u.id = n.action_by
                 WHERE n.user_id = ?
                   AND (? = 0 OR (
                         n.read_at IS NULL
                         AND (n.notification_type <> 'APPROVAL_REQUIRED' OR n.resolved_at IS NULL)
                       ))
                 ORDER BY n.created_at DESC
                 LIMIT ?
                """, (rs, rowNum) -> UserNotificationResponse.builder()
                .id(rs.getString("id"))
                .notificationType(UserNotificationType.valueOf(rs.getString("notification_type")))
                .title(rs.getString("title"))
                .message(rs.getString("message"))
                .approvalRequestId(rs.getString("approval_request_id"))
                .approvalStepNo((Integer) rs.getObject("approval_step_no"))
                .approvalType(rs.getString("approval_type"))
                .approvalStatus(rs.getString("approval_status"))
                .referenceId(rs.getString("reference_id"))
                .referenceNo(rs.getString("reference_no"))
                .actionBy(rs.getString("action_by"))
                .actionByDisplayName(rs.getString("action_by_display_name"))
                .route(rs.getString("route"))
                .readAt(toLocalDateTime(rs.getTimestamp("read_at")))
                .resolvedAt(toLocalDateTime(rs.getTimestamp("resolved_at")))
                .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                .build(), userId, unreadOnly ? 1 : 0, limit);
    }

    private long unreadCount(String userId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM user_notification
                 WHERE user_id = ?
                   AND read_at IS NULL
                   AND (notification_type <> 'APPROVAL_REQUIRED' OR resolved_at IS NULL)
                """, Long.class, userId);
        return count == null ? 0 : count;
    }

    private void createNotification(
            String userId,
            String eventKey,
            UserNotificationType type,
            String title,
            String message,
            ApprovalRequestEntity request,
            Integer stepNo,
            String actionBy,
            String route
    ) {
        String canonical = canonicalUserId(userId);
        if (canonical == null || canonical.isBlank()) return;
        String duplicateHandling = type == UserNotificationType.APPROVAL_REQUIRED
                ? """
                  ON DUPLICATE KEY UPDATE
                      read_at = IF(user_notification.resolved_at IS NOT NULL, NULL, user_notification.read_at),
                      resolved_at = NULL
                  """
                : "ON DUPLICATE KEY UPDATE id = id";
        jdbcTemplate.update("""
                INSERT INTO user_notification(
                    id, user_id, event_key, notification_type, title, message,
                    approval_request_id, approval_step_no, approval_type, approval_status,
                    reference_id, reference_no, action_by, route, created_by
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """ + duplicateHandling,
                UUID.randomUUID().toString(), canonical, eventKey, type.name(), title, message,
                request.getId(), stepNo,
                request.getApprovalType() == null ? null : request.getApprovalType().name(),
                request.getStatus() == null ? null : request.getStatus().name(),
                request.getReferenceId(), request.getReferenceNo(), actionBy, route,
                actionBy == null ? request.getRequesterId() : actionBy
        );
    }

    private ApprovalWorkflowStepEntity currentStep(ApprovalRequestEntity request) {
        return workflowStepRepository.findByWorkflowIdAndStepNoAndActiveTrue(
                        request.getWorkflowId(), request.getCurrentStepNo())
                .orElseThrow(() -> new RuntimeException("Current approval step not found"));
    }

    private Set<String> resolveApproverUserIds(
            ApprovalWorkflowStepEntity step,
            ApprovalRequestEntity request
    ) {
        Set<String> users = new LinkedHashSet<>();
        if (step.getApprovers() == null) return users;
        for (ApprovalWorkflowStepApproverEntity approver : step.getApprovers()) {
            if (Boolean.FALSE.equals(approver.getActive()) || approver.getApproverType() == null) continue;
            if (!approverScopeService.appliesToRequest(approver, request)) continue;
            String value = approver.getApproverValue();
            if (value == null || value.isBlank()) continue;
            switch (approver.getApproverType()) {
                case USER -> users.addAll(userIdsForConfiguredUser(value));
                case ROLE -> users.addAll(userIdsForRole(value));
                case GROUP -> users.addAll(userIdsForGroup(value));
                case MANAGER -> users.addAll(managerIdsForRequester(request.getRequesterId()));
            }
        }
        users.removeIf(id -> id == null || id.isBlank());
        return users;
    }

    private List<String> userIdsForConfiguredUser(String configured) {
        return jdbcTemplate.queryForList("""
                SELECT id FROM `user`
                 WHERE status = 'ACTIVE'
                   AND (expires_at IS NULL OR expires_at > NOW())
                   AND (id = ? OR username = ? OR email = ?)
                """, String.class, configured, configured, configured);
    }

    private List<String> userIdsForRole(String role) {
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT u.id
                  FROM user_role ur
                  JOIN `user` u ON u.id = ur.user
                  JOIN role r ON r.id = ur.role
                 WHERE (r.id = ? OR UPPER(r.description) = UPPER(?))
                   AND u.status = 'ACTIVE'
                   AND (u.expires_at IS NULL OR u.expires_at > NOW())
                   AND (ur.valid_from IS NULL OR ur.valid_from <= CURRENT_DATE)
                   AND (ur.valid_to IS NULL OR ur.valid_to >= CURRENT_DATE)
                """, String.class, role, role);
    }

    private List<String> userIdsForGroup(String group) {
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT u.id
                  FROM approval_group_member gm
                  JOIN `user` u ON u.id = gm.user_id
                 WHERE gm.group_code = ? AND gm.active = 1
                   AND u.status = 'ACTIVE'
                   AND (u.expires_at IS NULL OR u.expires_at > NOW())
                """, String.class, group);
    }

    private List<String> managerIdsForRequester(String requester) {
        String requesterId = canonicalUserId(requester);
        if (requesterId == null) return List.of();
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT u.id
                  FROM approval_manager_assignment ma
                  JOIN `user` u ON u.id = ma.manager_user_id
                 WHERE ma.active = 1
                   AND ma.requester_user_id = ?
                   AND u.status = 'ACTIVE'
                   AND (u.expires_at IS NULL OR u.expires_at > NOW())
                """, String.class, requesterId);
    }

    public String canonicalUserId(String identity) {
        if (identity == null || identity.isBlank()) return identity;
        List<String> ids = jdbcTemplate.queryForList("""
                SELECT id FROM `user`
                 WHERE id = ? OR username = ? OR email = ? OR partner = ?
                 ORDER BY CASE
                            WHEN id = ? THEN 0
                            WHEN username = ? THEN 1
                            WHEN email = ? THEN 2
                            ELSE 3
                          END
                 LIMIT 1
                """, String.class,
                identity, identity, identity, identity,
                identity, identity, identity);
        return ids.isEmpty() ? identity : ids.get(0);
    }

    private String requireCanonicalUser(String identity) {
        String authenticatedUserId = UserContext.getCurrentUserId();
        if (authenticatedUserId != null
                && !authenticatedUserId.isBlank()
                && authenticatedUserId.equals(identity)) {
            return authenticatedUserId;
        }
        String userId = canonicalUserId(identity);
        if (userId == null || userId.isBlank()) {
            throw new RuntimeException("Current user could not be determined");
        }
        return userId;
    }

    private String displayName(String userId) {
        if (userId == null || userId.isBlank()) return "an approver";
        List<String> names = jdbcTemplate.queryForList(
                "SELECT username FROM `user` WHERE id = ? LIMIT 1", String.class, userId);
        return names.isEmpty() ? userId : names.get(0);
    }

    private String requiredMessage(ApprovalRequestEntity request, ApprovalWorkflowStepEntity step) {
        return "%s is waiting for your approval at step %d%s."
                .formatted(
                        referenceLabel(request),
                        step.getStepNo(),
                        step.getStepName() == null || step.getStepName().isBlank()
                                ? ""
                                : " (" + step.getStepName() + ")"
                );
    }

    private String referenceLabel(ApprovalRequestEntity request) {
        if (request.getReferenceNo() != null && !request.getReferenceNo().isBlank()) {
            return request.getTitle() + " [" + request.getReferenceNo() + "]";
        }
        return request.getTitle();
    }

    private int normaliseLimit(int limit) {
        if (limit < 1) return 50;
        return Math.min(limit, 200);
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private ApprovalRequestResponse toResponse(ApprovalRequestEntity entity) {
        return ApprovalRequestResponse.builder()
                .id(entity.getId())
                .approvalType(entity.getApprovalType())
                .referenceId(entity.getReferenceId())
                .referenceNo(entity.getReferenceNo())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .requesterId(entity.getRequesterId())
                .workflowId(entity.getWorkflowId())
                .currentStepNo(entity.getCurrentStepNo())
                .status(entity.getStatus())
                .payloadJson(entity.getPayloadJson())
                .finalActionBy(entity.getFinalActionBy())
                .finalActionAt(entity.getFinalActionAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
