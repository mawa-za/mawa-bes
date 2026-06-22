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
        return ApprovalWorkflowStepResponseDto.builder()
                .id(entity.getId())
                .workflow(entity.getWorkflow() != null ? entity.getWorkflow().getId() : null)
                .stepNoId(entity.getStepNo() != null ? entity.getStepNo().toString() : null)
                .stepNameId(entity.getStepName())
                .approvalModeId(entity.getApprovalMode() != null ? entity.getApprovalMode().name() : null)
                .requiredApprovals(entity.getRequiredApprovals())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .approvers(entity.getApprovers() != null ? entity.getApprovers().stream().map(za.co.mawa.bes.entity.v2.ApprovalWorkflowStepApproverEntity::getId).toList() : null)
                .build();
    }

    public ApprovalWorkflowStepEntity toEntity(ApprovalWorkflowStepCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return ApprovalWorkflowStepEntity.builder()
                .stepNo(request.getStepNoId() != null ? Integer.valueOf(request.getStepNoId()) : null)
                .stepName(request.getStepNameId())
                .approvalMode(request.getApprovalModeId() != null ? za.co.mawa.bes.enums.ApprovalMode.valueOf(request.getApprovalModeId()) : null)
                .requiredApprovals(request.getRequiredApprovals())
                .active(request.getActive())
                .build();
    }

    public void updateEntity(ApprovalWorkflowStepEntity entity, ApprovalWorkflowStepUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setStepNo(request.getStepNoId() != null ? Integer.valueOf(request.getStepNoId()) : null);
        entity.setStepName(request.getStepNameId());
        entity.setApprovalMode(request.getApprovalModeId() != null ? za.co.mawa.bes.enums.ApprovalMode.valueOf(request.getApprovalModeId()) : null);
        entity.setRequiredApprovals(request.getRequiredApprovals());
        entity.setActive(request.getActive());
    }
}
