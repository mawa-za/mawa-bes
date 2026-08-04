package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowEntity;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowStepApproverEntity;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowStepEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.repository.v2.ApprovalWorkflowRepository;
import za.co.mawa.bes.service.v2.impl.ApprovalWorkflowConfigServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalWorkflowConfigServiceImplTest {

    @Mock
    private ApprovalWorkflowRepository workflowRepository;

    @Test
    void activationRequiresEnoughActiveApprovers() {
        ApprovalWorkflowStepApproverEntity inactiveApprover = new ApprovalWorkflowStepApproverEntity();
        inactiveApprover.setActive(false);

        ApprovalWorkflowStepEntity step = new ApprovalWorkflowStepEntity();
        step.setStepNo(1);
        step.setStepName("Approve leave");
        step.setActive(true);
        step.setRequiredApprovals(1);
        step.getApprovers().add(inactiveApprover);

        ApprovalWorkflowEntity workflow = ApprovalWorkflowEntity.builder()
                .id("workflow-leave")
                .approvalType(ApprovalType.LEAVE)
                .name("Leave approval")
                .active(false)
                .steps(List.of(step))
                .build();

        when(workflowRepository.findById("workflow-leave"))
                .thenReturn(Optional.of(workflow));

        ApprovalWorkflowConfigServiceImpl service =
                new ApprovalWorkflowConfigServiceImpl(workflowRepository);

        assertThrows(RuntimeException.class, () -> service.activate("workflow-leave"));
        verify(workflowRepository, never()).save(workflow);
    }

    @Test
    void workflowCannotBeDeletedAndMustBeDeactivated() {
        ApprovalWorkflowEntity workflow = ApprovalWorkflowEntity.builder()
                .id("workflow-leave")
                .approvalType(ApprovalType.LEAVE)
                .name("Leave approval")
                .active(false)
                .build();
        when(workflowRepository.findById("workflow-leave"))
                .thenReturn(Optional.of(workflow));

        ApprovalWorkflowConfigServiceImpl service =
                new ApprovalWorkflowConfigServiceImpl(workflowRepository);

        assertThrows(
                IllegalStateException.class,
                () -> service.delete("workflow-leave")
        );
        verify(workflowRepository, never()).delete(workflow);
    }
}
