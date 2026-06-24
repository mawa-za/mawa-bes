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
        return ApprovalWorkflowStepApproverResponseDto.builder()
                .id(entity.getId())
                .workflowStep(entity.getWorkflowStep() != null ? entity.getWorkflowStep().getId() : null)
                .approverTypeId(entity.getApproverType() != null ? entity.getApproverType().name() : null)
                .approverValueId(entity.getApproverValue())
                .approverNameId(entity.getApproverName())
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
                .approverValue(request.getApproverValueId())
                .approverName(request.getApproverNameId())
                .active(request.getActive())
                .build();
    }

    public void updateEntity(ApprovalWorkflowStepApproverEntity entity, ApprovalWorkflowStepApproverUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setApproverValue(request.getApproverValueId());
        entity.setApproverName(request.getApproverNameId());
        entity.setActive(request.getActive());
    }
}
