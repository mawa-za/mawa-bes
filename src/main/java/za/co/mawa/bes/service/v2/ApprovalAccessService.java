package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.v2.AssignedApprovalTypeResponse;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowStepApproverEntity;
import za.co.mawa.bes.repository.v2.ApprovalWorkflowStepRepository;
import za.co.mawa.bes.repository.v2.ApprovalWorkflowStepApproverRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalAccessService {
    private final JdbcTemplate jdbcTemplate;
    private final ApprovalWorkflowStepRepository workflowStepRepository;
    private final ApprovalWorkflowStepApproverRepository approverRepository;
    private final ApprovalApproverScopeService approverScopeService;

    public List<AssignedApprovalTypeResponse> assignedTypes(String user) {
        if (user == null || user.isBlank()) return List.of();
        return jdbcTemplate.query("""
            SELECT aw.approval_type,
                   REPLACE(aw.approval_type, '_', ' ') label
              FROM approval_workflow aw
             WHERE aw.active = 1 AND COALESCE(aw.auto_approve, 0) = 0
               AND EXISTS (
                 SELECT 1
                   FROM approval_workflow_step aws
                   JOIN approval_workflow_step_approver a ON a.workflow_step_id = aws.id AND a.active = 1
                  WHERE aws.workflow_id = aw.id AND aws.active = 1
                    AND (
                      (a.approver_type = 'USER' AND EXISTS
                        (SELECT 1 FROM `user` u WHERE (u.id = ? OR u.username = ? OR u.email = ?)
                          AND (u.id = a.approver_value OR u.username = a.approver_value OR u.email = a.approver_value)
                          AND u.status = 'ACTIVE' AND (u.expires_at IS NULL OR u.expires_at > NOW())))
                      OR (a.approver_type = 'ROLE' AND EXISTS (
                        SELECT 1 FROM user_role ur JOIN `user` u ON u.id = ur.user JOIN role r ON r.id = ur.role
                         WHERE (u.id = ? OR u.username = ? OR u.email = ?)
                           AND (r.id = a.approver_value OR UPPER(r.description) = UPPER(a.approver_value))
                           AND u.status = 'ACTIVE' AND (u.expires_at IS NULL OR u.expires_at > NOW())
                           AND (ur.valid_from IS NULL OR ur.valid_from <= CURRENT_DATE)
                           AND (ur.valid_to IS NULL OR ur.valid_to >= CURRENT_DATE)))
                      OR (a.approver_type = 'GROUP' AND EXISTS (
                        SELECT 1 FROM approval_group_member gm JOIN `user` u ON u.id = gm.user_id
                         WHERE gm.group_code = a.approver_value AND gm.active = 1
                           AND (u.id = ? OR u.username = ? OR u.email = ?)
                           AND u.status = 'ACTIVE' AND (u.expires_at IS NULL OR u.expires_at > NOW())))
                      OR (a.approver_type = 'MANAGER' AND EXISTS (
                        SELECT 1 FROM approval_manager_assignment ma
                         WHERE ma.active = 1 AND ma.manager_user_id IN
                           (SELECT id FROM `user` WHERE id = ? OR username = ? OR email = ?)))
                    )
               )
             ORDER BY label
            """, (rs, rowNum) -> new AssignedApprovalTypeResponse(
                    rs.getString("approval_type"), rs.getString("label"), 0L),
                user, user, user, user, user, user, user, user, user, user, user, user);
    }

    public boolean canView(ApprovalRequestEntity request, String user) {
        if (request == null || user == null || user.isBlank() || request.getCurrentStepNo() == null) return false;
        var step = workflowStepRepository.findByWorkflowIdAndStepNoAndActiveTrue(
                request.getWorkflowId(), request.getCurrentStepNo()).orElse(null);
        if (step == null) return false;
        return approverRepository.findByWorkflowStepIdAndActiveTrue(step.getId()).stream()
                .filter(a -> a.getActive() == null || a.getActive())
                .filter(a -> approverScopeService.appliesToRequest(a, request))
                .anyMatch(a -> matches(a, user, request));
    }

    private boolean matches(ApprovalWorkflowStepApproverEntity rule, String user, ApprovalRequestEntity request) {
        if (rule.getApproverType() == null || rule.getApproverValue() == null) return false;
        return switch (rule.getApproverType()) {
            case USER -> identityMatches(user, rule.getApproverValue());
            case ROLE -> hasRole(user, rule.getApproverValue());
            case GROUP -> inGroup(user, rule.getApproverValue());
            case MANAGER -> manages(user, request.getRequesterId());
        };
    }

    private boolean identityMatches(String user, String configured) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
            SELECT COUNT(*) > 0 FROM `user`
             WHERE status='ACTIVE' AND (expires_at IS NULL OR expires_at > NOW())
               AND (id=? OR username=? OR email=?) AND (id=? OR username=? OR email=?)
            """, Boolean.class, user, user, user, configured, configured, configured));
    }

    private boolean hasRole(String user, String role) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
            SELECT COUNT(*) > 0 FROM user_role ur JOIN `user` u ON u.id=ur.user JOIN role r ON r.id=ur.role
             WHERE (u.id=? OR u.username=? OR u.email=?) AND (r.id=? OR UPPER(r.description)=UPPER(?))
               AND u.status='ACTIVE' AND (u.expires_at IS NULL OR u.expires_at > NOW())
               AND (ur.valid_from IS NULL OR ur.valid_from <= CURRENT_DATE)
               AND (ur.valid_to IS NULL OR ur.valid_to >= CURRENT_DATE)
            """, Boolean.class, user, user, user, role, role));
    }

    private boolean inGroup(String user, String group) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
            SELECT COUNT(*) > 0 FROM approval_group_member gm JOIN `user` u ON u.id=gm.user_id
             WHERE gm.group_code=? AND gm.active=1 AND (u.id=? OR u.username=? OR u.email=?)
               AND u.status='ACTIVE' AND (u.expires_at IS NULL OR u.expires_at > NOW())
            """, Boolean.class, group, user, user, user));
    }

    private boolean manages(String user, String requester) {
        if (requester == null) return false;
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
            SELECT COUNT(*) > 0 FROM approval_manager_assignment ma WHERE ma.active=1
              AND ma.manager_user_id IN (SELECT id FROM `user` WHERE id=? OR username=? OR email=?)
              AND ma.requester_user_id IN (SELECT id FROM `user` WHERE id=? OR username=? OR email=?)
            """, Boolean.class, user, user, user, requester, requester, requester));
    }
}
