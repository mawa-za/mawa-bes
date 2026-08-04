package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.entity.v2.ApprovalActionEntity;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowEntity;
import za.co.mawa.bes.enums.ApprovalStatus;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.repository.v2.ApprovalActionRepository;
import za.co.mawa.bes.repository.v2.ApprovalRequestRepository;
import za.co.mawa.bes.repository.v2.ApprovalWorkflowRepository;
import za.co.mawa.bes.repository.v2.ApprovalWorkflowStepRepository;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock private ApprovalWorkflowRepository workflowRepository;
    @Mock private ApprovalWorkflowStepRepository workflowStepRepository;
    @Mock private ApprovalRequestRepository approvalRequestRepository;
    @Mock private ApprovalActionRepository approvalActionRepository;
    @Mock private ApprovalCompletionHandlerRegistry completionHandlerRegistry;
    @Mock private ApprovalSubmissionHandlerRegistry submissionHandlerRegistry;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private UserInboxService userInboxService;

    @InjectMocks
    private ApprovalService service;

    @Test
    void inactiveWorkflowAutoApprovesNewRequestAndKeepsAuditTrail() {
        ApprovalWorkflowEntity workflow = ApprovalWorkflowEntity.builder()
                .id("workflow-leave")
                .approvalType(ApprovalType.LEAVE)
                .name("Leave approval")
                .active(false)
                .build();

        ApprovalSubmitRequest request = new ApprovalSubmitRequest();
        request.setApprovalType(ApprovalType.LEAVE);
        request.setReferenceId("leave-1");
        request.setReferenceNo("LV-0001");
        request.setRequesterId("user-1");
        request.setTitle("Leave request");

        when(approvalRequestRepository.findByApprovalTypeAndReferenceId(
                ApprovalType.LEAVE, "leave-1"))
                .thenReturn(Optional.empty());
        when(workflowRepository.findByApprovalType(ApprovalType.LEAVE))
                .thenReturn(Optional.of(workflow));

        AtomicInteger saves = new AtomicInteger();
        when(approvalRequestRepository.save(any(ApprovalRequestEntity.class)))
                .thenAnswer(invocation -> {
                    ApprovalRequestEntity entity = invocation.getArgument(0);
                    if (entity.getId() == null) entity.setId("approval-1");
                    saves.incrementAndGet();
                    return entity;
                });
        when(approvalActionRepository.save(any(ApprovalActionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApprovalRequestResponse response = service.submitForApproval(request);

        assertEquals(ApprovalStatus.APPROVED, response.getStatus());
        assertEquals(0, response.getCurrentStepNo());
        assertEquals("user-1", response.getFinalActionBy());
        assertEquals(2, saves.get());
        verify(submissionHandlerRegistry).handleSubmit(any(ApprovalRequestEntity.class), org.mockito.ArgumentMatchers.eq("user-1"));
        verify(completionHandlerRegistry).handleApproved(any(ApprovalRequestEntity.class), org.mockito.ArgumentMatchers.eq("user-1"));
        verify(approvalActionRepository, org.mockito.Mockito.times(2)).save(any(ApprovalActionEntity.class));
        verify(userInboxService, never()).notifyApprovalRequired(any(ApprovalRequestEntity.class));
        verify(workflowStepRepository, never())
                .findByWorkflowIdAndActiveTrueOrderByStepNoAsc(any());
    }
}
