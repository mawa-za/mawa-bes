package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowStepApproverEntity;
import za.co.mawa.bes.dto.v2.ApprovalWorkflowStepApproverCreateRequestDto;
import za.co.mawa.bes.dto.v2.ApprovalWorkflowStepApproverResponseDto;
import za.co.mawa.bes.dto.v2.ApprovalWorkflowStepApproverUpdateRequestDto;

@Component
public class ApprovalWorkflowStepApproverMapper {

    public ApprovalWorkflowStepApproverResponseDto toResponse(ApprovalWorkflowStepApproverEntity entity) {
        if (entity == null) {
            return null;
        }
        // TODO: map relation field `approverType` to `approverTypeId` once the related entity id getter is confirmed.
        // TODO: map relation field `approverValue` to `approverValueId` once the related entity id getter is confirmed.
        // TODO: map relation field `approverName` to `approverNameId` once the related entity id getter is confirmed.
        return ApprovalWorkflowStepApproverResponseDto.builder()
                .id(entity.getId())
                .workflowStep(entity.getWorkflowStep())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public ApprovalWorkflowStepApproverEntity toEntity(ApprovalWorkflowStepApproverCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return ApprovalWorkflowStepApproverEntity.builder()
                .workflowStep(request.getWorkflowStep())
                .active(request.getActive())
                .build();
    }

    public void updateEntity(ApprovalWorkflowStepApproverEntity entity, ApprovalWorkflowStepApproverUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setWorkflowStep(request.getWorkflowStep());
        entity.setActive(request.getActive());
    }
}
