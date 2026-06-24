package za.co.mawa.bes.mapper.v2;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalActionEntity;
import za.co.mawa.bes.dto.v2.ApprovalActionCreateRequestDto;
import za.co.mawa.bes.dto.v2.ApprovalActionResponseDto;
import za.co.mawa.bes.dto.v2.ApprovalActionUpdateRequestDto;

@Component
public class ApprovalActionMapper {

    public ApprovalActionResponseDto toResponse(ApprovalActionEntity entity) {
        if (entity == null) {
            return null;
        }

        return ApprovalActionResponseDto.builder()
                .id(entity.getId())
                .approvalRequestId(entity.getApprovalRequestId())
                .stepNo(entity.getStepNo())
                .action(entity.getAction())
                .actionBy(entity.getActionBy())
                .actionAt(entity.getActionAt())
                .comments(entity.getComments())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    public ApprovalActionEntity toEntity(ApprovalActionCreateRequestDto request) {
        if (request == null) {
            return null;
        }
        return ApprovalActionEntity.builder()
                .approvalRequestId(request.getApprovalRequestId())
                .stepNo(request.getStepNo())
                .action(request.getAction())
                .actionBy(request.getActionBy())
                .actionAt(request.getActionAt())
                .comments(request.getComments())
                .build();
    }

    public void updateEntity(ApprovalActionEntity entity, ApprovalActionUpdateRequestDto request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setApprovalRequestId(request.getApprovalRequestId());
        entity.setStepNo(request.getStepNo());
        entity.setAction(request.getAction());
        entity.setActionBy(request.getActionBy());
        entity.setActionAt(request.getActionAt());
        entity.setComments(request.getComments());
    }
}
