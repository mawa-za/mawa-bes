package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.dto.v2.ApprovalRequestCreateRequestDto;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponseDto;
import za.co.mawa.bes.dto.v2.ApprovalRequestUpdateRequestDto;

@Component
public class ApprovalRequestMapper {

    public ApprovalRequestResponseDto toResponse(ApprovalRequestEntity entity) {
        if (entity == null) {
            return null;
        }

        return ApprovalRequestResponseDto.builder()
                .id(entity.getId())
                .approvalType(entity.getApprovalType())
                .referenceId(entity.getReferenceId())
                .referenceNo(entity.getReferenceNo())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .requesterId(entity.getRequesterId())
                .workflowId(entity.getWorkflowId())
                .currentStepNo(entity.getCurrentStepNo())
                .status(entity.getStatus())
                .payloadJson(entity.getPayloadJson())
                .finalActionBy(entity.getFinalActionBy())
                .finalActionAt(entity.getFinalActionAt())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public ApprovalRequestEntity toEntity(ApprovalRequestCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return ApprovalRequestEntity.builder()
                .approvalType(request.getApprovalType())
                .referenceId(request.getReferenceId())
                .referenceNo(request.getReferenceNo())
                .title(request.getTitle())
                .description(request.getDescription())
                .requesterId(request.getRequesterId())
                .workflowId(request.getWorkflowId())
                .currentStepNo(request.getCurrentStepNo())
                .status(request.getStatus())
                .payloadJson(request.getPayloadJson())
                .finalActionBy(request.getFinalActionBy())
                .finalActionAt(request.getFinalActionAt())
                .build();
    }

    public void updateEntity(ApprovalRequestEntity entity, ApprovalRequestUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setApprovalType(request.getApprovalType());
        entity.setReferenceId(request.getReferenceId());
        entity.setReferenceNo(request.getReferenceNo());
        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setRequesterId(request.getRequesterId());
        entity.setWorkflowId(request.getWorkflowId());
        entity.setCurrentStepNo(request.getCurrentStepNo());
        entity.setStatus(request.getStatus());
        entity.setPayloadJson(request.getPayloadJson());
        entity.setFinalActionBy(request.getFinalActionBy());
        entity.setFinalActionAt(request.getFinalActionAt());
    }
}
