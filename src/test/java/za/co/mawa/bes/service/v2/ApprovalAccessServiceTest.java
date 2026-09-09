package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowStepApproverEntity;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowStepEntity;
import za.co.mawa.bes.enums.ApproverType;
import za.co.mawa.bes.repository.v2.ApprovalWorkflowStepApproverRepository;
import za.co.mawa.bes.repository.v2.ApprovalWorkflowStepRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApprovalAccessServiceTest {
    @Test
    void requestVisibilityUsesTheSameApproverRuleAsTheTileAssignment() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ApprovalWorkflowStepRepository steps = mock(ApprovalWorkflowStepRepository.class);
        ApprovalWorkflowStepApproverRepository approvers = mock(ApprovalWorkflowStepApproverRepository.class);
        ApprovalApproverScopeService scope = mock(ApprovalApproverScopeService.class);
        ApprovalWorkflowStepEntity step = new ApprovalWorkflowStepEntity();
        step.setId("STEP-1");
        ApprovalWorkflowStepApproverEntity rule = new ApprovalWorkflowStepApproverEntity();
        rule.setApproverType(ApproverType.USER);
        rule.setApproverValue("APPROVER");
        rule.setActive(true);
        ApprovalRequestEntity request = new ApprovalRequestEntity();
        request.setWorkflowId("WF-1");
        request.setCurrentStepNo(1);

        when(steps.findByWorkflowIdAndStepNoAndActiveTrue("WF-1", 1)).thenReturn(Optional.of(step));
        when(approvers.findByWorkflowStepIdAndActiveTrue("STEP-1")).thenReturn(List.of(rule));
        when(scope.appliesToRequest(rule, request)).thenReturn(true);
        when(jdbc.queryForObject(anyString(), any(Class.class), any(Object[].class))).thenReturn(true);

        ApprovalAccessService service = new ApprovalAccessService(jdbc, steps, approvers, scope);
        assertTrue(service.canView(request, "APPROVER"));
        assertFalse(service.canView(request, ""));
    }
}
