package za.co.mawa.bes.service.v2;

import za.co.mawa.bes.dto.v2.approval.ApprovalWorkflowRequestDto;
import za.co.mawa.bes.dto.v2.approval.ApprovalWorkflowResponseDto;
import za.co.mawa.bes.enums.ApprovalType;

import java.math.BigDecimal;
import java.util.List;

public interface ApprovalWorkflowConfigService {

    ApprovalWorkflowResponseDto create(ApprovalWorkflowRequestDto request);

    ApprovalWorkflowResponseDto update(String id, ApprovalWorkflowRequestDto request);

    ApprovalWorkflowResponseDto getById(String id);

    ApprovalWorkflowResponseDto getByApprovalType(ApprovalType approvalType);

    ApprovalWorkflowResponseDto getActiveByApprovalType(ApprovalType approvalType);

    ApprovalWorkflowResponseDto findApplicableWorkflow(ApprovalType approvalType, BigDecimal amount);

    List<ApprovalWorkflowResponseDto> getAll();

    List<ApprovalWorkflowResponseDto> getActive();

    void activate(String id);

    void deactivate(String id);

    void delete(String id);
}