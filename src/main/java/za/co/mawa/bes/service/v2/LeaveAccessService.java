package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.entity.EmploymentEntity;
import za.co.mawa.bes.repository.EmploymentRepository;
import za.co.mawa.bes.utils.Status;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class LeaveAccessService {

    private static final List<String> LEAVE_EMPLOYMENT_STATUSES = List.of(Status.ACTIVE, Status.SUSPENDED);

    private final EmploymentRepository employmentRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public EmploymentEntity currentEmployment(LocalDate onDate) {
        String partnerId = currentPartnerId();
        LocalDate date = onDate == null ? LocalDate.now() : onDate;
        List<EmploymentEntity> matches = employmentRepository.findApplicableEmployment(
                partnerId, Date.valueOf(date), LEAVE_EMPLOYMENT_STATUSES);
        if (!matches.isEmpty()) return matches.get(0);
        return employmentRepository.findFirstByPartnerIdAndStatusInOrderByStartDateDesc(partnerId, LEAVE_EMPLOYMENT_STATUSES)
                .orElseThrow(() -> new NoSuchElementException("No active employment record is linked to the logged in user"));
    }

    public String currentPartnerId() {
        String partner = trim(UserContext.getCurrentUserPartner());
        if (partner == null) {
            throw new SecurityException("The logged in user is not linked to an employee partner record");
        }
        return partner;
    }

    public void assertOwnEmployment(String employmentId) {
        if (employmentId == null || employmentId.isBlank()) {
            throw new SecurityException("Leave access is limited to the logged in employee");
        }
        EmploymentEntity employment = employmentRepository.findById(employmentId)
                .orElseThrow(() -> new NoSuchElementException("Employment record not found: " + employmentId));
        if (!currentPartnerId().equals(employment.getPartnerId())) {
            throw new SecurityException("You can only access your own leave information");
        }
    }

    public boolean ownsEmployment(String employmentId) {
        if (employmentId == null || employmentId.isBlank()) return false;
        String partner = trim(UserContext.getCurrentUserPartner());
        if (partner == null) return false;
        return employmentRepository.findById(employmentId)
                .map(e -> partner.equals(e.getPartnerId()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<String> approvableEmploymentIds() {
        String userId = currentUserId();
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT e.id
                  FROM employment e
                  JOIN approval_workflow w
                    ON w.approval_type = 'LEAVE'
                   AND w.active = 1
                  JOIN approval_workflow_step s
                    ON s.workflow_id = w.id
                   AND s.active = 1
                  JOIN approval_workflow_step_approver a
                    ON a.workflow_step_id = s.id
                   AND a.active = 1
                  JOIN `user` approver_user
                    ON approver_user.id = ?
                 WHERE e.status IN ('ACTIVE', 'SUSPENDED')
                   AND approver_user.status = 'ACTIVE'
                   AND (approver_user.expires_at IS NULL OR approver_user.expires_at > NOW())
                   AND (
                        (a.approver_type = 'USER' AND (
                             a.approver_value = approver_user.id OR
                             a.approver_value = approver_user.username OR
                             a.approver_value = approver_user.email OR
                             a.approver_value = approver_user.partner
                        ))
                        OR
                        (a.approver_type = 'ROLE' AND EXISTS (
                             SELECT 1
                               FROM user_role ur
                               JOIN role r ON r.id = ur.role
                              WHERE ur.user = approver_user.id
                                AND (r.id = a.approver_value OR UPPER(r.description) = UPPER(a.approver_value))
                                AND (ur.valid_from IS NULL OR ur.valid_from <= CURRENT_DATE)
                                AND (ur.valid_to IS NULL OR ur.valid_to >= CURRENT_DATE)
                        ))
                        OR
                        (a.approver_type = 'GROUP' AND EXISTS (
                             SELECT 1
                               FROM approval_group_member gm
                              WHERE gm.user_id = approver_user.id
                                AND gm.group_code = a.approver_value
                                AND gm.active = 1
                        ))
                        OR
                        (a.approver_type = 'MANAGER' AND EXISTS (
                             SELECT 1
                               FROM approval_manager_assignment ma
                               JOIN `user` requester_user ON requester_user.id = ma.requester_user_id
                              WHERE ma.manager_user_id = approver_user.id
                                AND ma.active = 1
                                AND requester_user.partner = e.partner_id
                        ))
                   )
                   AND (
                        a.assignment_scope_type IS NULL
                        OR a.assignment_scope_type = ''
                        OR UPPER(a.assignment_scope_type) = 'ALL'
                        OR (UPPER(a.assignment_scope_type) = 'POSITION' AND UPPER(a.assignment_scope_value) = UPPER(e.position))
                        OR (UPPER(a.assignment_scope_type) = 'EMPLOYEE'
                            AND (a.assignment_scope_value = e.id OR a.assignment_scope_value = e.partner_id))
                   )
                ORDER BY e.id
                """, String.class, userId);
    }

    public boolean canApproveEmployment(String employmentId) {
        return employmentId != null && approvableEmploymentIds().contains(employmentId);
    }

    public void assertCanApproveEmployment(String employmentId) {
        if (!canApproveEmployment(employmentId)) {
            throw new SecurityException("You are not configured as a leave approver for this employee");
        }
    }

    public boolean isLeaveApprover() {
        return !approvableEmploymentIds().isEmpty();
    }

    private String currentUserId() {
        String identity = firstNonBlank(
                UserContext.getCurrentUserId(),
                UserContext.getCurrentUser(),
                UserContext.getCurrentUserPartner());
        if (identity == null) throw new SecurityException("Current user could not be determined");

        List<String> ids = jdbcTemplate.queryForList("""
                SELECT id
                  FROM `user`
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
        if (ids.isEmpty()) throw new SecurityException("Current user could not be resolved");
        return ids.get(0);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String trimmed = trim(value);
            if (trimmed != null) return trimmed;
        }
        return null;
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
