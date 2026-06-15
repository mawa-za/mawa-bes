package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowEntity;
import za.co.mawa.bes.dto.v2.ApprovalWorkflowCreateRequestDto;
import za.co.mawa.bes.dto.v2.ApprovalWorkflowResponseDto;
import za.co.mawa.bes.dto.v2.ApprovalWorkflowUpdateRequestDto;

@Component
public class ApprovalWorkflowMapper {

    public ApprovalWorkflowResponseDto toResponse(ApprovalWorkflowEntity entity) {
        if (entity == null) {
            return null;
        }

        return ApprovalWorkflowResponseDto.builder()
                .id(entity.getId())
                .approvalType(entity.getApprovalType())
                .name(entity.getName())
                .description(entity.getDescription())
                .minAmount(entity.getMinAmount())
                .maxAmount(entity.getMaxAmount())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .steps(entity.getSteps())
                .build();
    }

    public ApprovalWorkflowEntity toEntity(ApprovalWorkflowCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return ApprovalWorkflowEntity.builder()
                .approvalType(request.getApprovalType())
                .name(request.getName())
                .description(request.getDescription())
                .minAmount(request.getMinAmount())
                .maxAmount(request.getMaxAmount())
                .active(request.getActive())
                .steps(request.getSteps())
                .build();
    }

    public void updateEntity(ApprovalWorkflowEntity entity, ApprovalWorkflowUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setApprovalType(request.getApprovalType());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setMinAmount(request.getMinAmount());
        entity.setMaxAmount(request.getMaxAmount());
        entity.setActive(request.getActive());
        entity.setSteps(request.getSteps());
    }
}
