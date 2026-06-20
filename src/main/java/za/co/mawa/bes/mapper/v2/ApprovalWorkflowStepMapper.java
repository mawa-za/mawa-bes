package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowStepEntity;
import za.co.mawa.bes.dto.v2.ApprovalWorkflowStepCreateRequestDto;
import za.co.mawa.bes.dto.v2.ApprovalWorkflowStepResponseDto;
import za.co.mawa.bes.dto.v2.ApprovalWorkflowStepUpdateRequestDto;

@Component
public class ApprovalWorkflowStepMapper {

    public ApprovalWorkflowStepResponseDto toResponse(ApprovalWorkflowStepEntity entity) {
        if (entity == null) {
            return null;
        }
        // TODO: map relation field `stepNo` to `stepNoId` once the related entity id getter is confirmed.
        // TODO: map relation field `stepName` to `stepNameId` once the related entity id getter is confirmed.
        // TODO: map relation field `approvalMode` to `approvalModeId` once the related entity id getter is confirmed.
        return ApprovalWorkflowStepResponseDto.builder()
                .id(entity.getId())
                .workflow(entity.getWorkflow())
                .requiredApprovals(entity.getRequiredApprovals())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .approvers(entity.getApprovers())
                .build();
    }

    public ApprovalWorkflowStepEntity toEntity(ApprovalWorkflowStepCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return ApprovalWorkflowStepEntity.builder()
                .workflow(request.getWorkflow())
                .requiredApprovals(request.getRequiredApprovals())
                .active(request.getActive())
                .approvers(request.getApprovers())
                .build();
    }

    public void updateEntity(ApprovalWorkflowStepEntity entity, ApprovalWorkflowStepUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setWorkflow(request.getWorkflow());
        entity.setRequiredApprovals(request.getRequiredApprovals());
        entity.setActive(request.getActive());
        entity.setApprovers(request.getApprovers());
    }
}
