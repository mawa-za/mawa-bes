package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.EmploymentEntity;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowStepApproverEntity;
import za.co.mawa.bes.enums.ApprovalType;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ApprovalApproverScopeService {

    private final JdbcTemplate jdbcTemplate;

    public boolean appliesToRequest(ApprovalWorkflowStepApproverEntity approver, ApprovalRequestEntity request) {
        if (approver == null || request == null || request.getApprovalType() != ApprovalType.LEAVE) {
            return true;
        }
        String scopeType = scopeType(approver.getAssignmentScopeType());
        if ("ALL".equals(scopeType)) return true;
        String scopeValue = trim(approver.getAssignmentScopeValue());
        if (scopeValue == null) return false;

        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM leave_request lr
                  JOIN employment e ON e.id = lr.employment_id
                 WHERE lr.id = ?
                   AND (
                        (? = 'POSITION' AND UPPER(e.position) = UPPER(?))
                        OR
                        (? = 'EMPLOYEE' AND (e.id = ? OR e.partner_id = ?))
                   )
                """, Integer.class,
                request.getReferenceId(),
                scopeType, scopeValue,
                scopeType, scopeValue, scopeValue);
        return count != null && count > 0;
    }

    public boolean appliesToEmployment(ApprovalWorkflowStepApproverEntity approver, EmploymentEntity employment) {
        if (approver == null || employment == null) return false;
        String scopeType = scopeType(approver.getAssignmentScopeType());
        String value = trim(approver.getAssignmentScopeValue());
        return switch (scopeType) {
            case "ALL" -> true;
            case "POSITION" -> value != null && value.equalsIgnoreCase(trim(employment.getPosition()));
            case "EMPLOYEE" -> value != null
                    && (value.equals(employment.getId()) || value.equals(employment.getPartnerId()));
            default -> false;
        };
    }

    public String scopeType(String value) {
        if (value == null || value.isBlank()) return "ALL";
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
