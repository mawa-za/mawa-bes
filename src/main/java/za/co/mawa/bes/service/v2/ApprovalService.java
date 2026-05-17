package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.*;
import za.co.mawa.bes.entity.v2.*;
import za.co.mawa.bes.enums.ApprovalActionType;
import za.co.mawa.bes.enums.ApprovalStatus;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.enums.ApproverType;
import za.co.mawa.bes.repository.v2.ApprovalActionRepository;
import za.co.mawa.bes.repository.v2.ApprovalRequestRepository;
import za.co.mawa.bes.repository.v2.ApprovalWorkflowRepository;
import za.co.mawa.bes.repository.v2.ApprovalWorkflowStepRepository;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowStepApproverEntity;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalWorkflowRepository workflowRepository;
    private final ApprovalWorkflowStepRepository workflowStepRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalActionRepository approvalActionRepository;
    private final ApprovalCompletionHandlerRegistry completionHandlerRegistry;

//    @Transactional
//    public ApprovalWorkflowEntity createWorkflow(ApprovalWorkflowCreateRequest request, String createdBy) {
//        if (request.getSteps() == null || request.getSteps().isEmpty()) {
//            throw new RuntimeException("At least one approval workflow step is required");
//        }
//
//        workflowRepository.findByApprovalTypeAndActiveTrue(request.getApprovalType())
//                .ifPresent(existing -> {
//                    throw new RuntimeException("Active workflow already exists for approval type: " + request.getApprovalType());
//                });
//
//        ApprovalWorkflowEntity workflow = new ApprovalWorkflowEntity();
//        workflow.setApprovalType(request.getApprovalType());
//        workflow.setName(request.getName());
//        workflow.setDescription(request.getDescription());
//        workflow.setActive(true);
//        workflow.setCreatedBy(createdBy);
//
//        workflow = workflowRepository.save(workflow);
//
//        for (ApprovalWorkflowStepCreateRequest stepRequest : request.getSteps()) {
//            ApprovalWorkflowStepEntity step = new ApprovalWorkflowStepEntity();
//            step.setWorkflowId(workflow.getId());
//            step.setStepNo(stepRequest.getStepNo());
//            step.setStepName(stepRequest.getStepName());
//            step.setApproverType(stepRequest.getApproverType());
//            step.setApproverValue(stepRequest.getApproverValue());
//            step.setRequiredApprovals(
//                    stepRequest.getRequiredApprovals() == null ? 1 : stepRequest.getRequiredApprovals()
//            );
//            step.setActive(true);
//            step.setCreatedBy(createdBy);
//
//            workflowStepRepository.save(step);
//        }
//
//        return workflow;
//    }

    @Transactional
    public ApprovalRequestResponse submitForApproval(ApprovalSubmitRequest request) {
        approvalRequestRepository
                .findByApprovalTypeAndReferenceId(request.getApprovalType(), request.getReferenceId())
                .ifPresent(existing -> {
                    throw new RuntimeException("Approval request already exists for reference: " + request.getReferenceId());
                });

        ApprovalWorkflowEntity workflow = workflowRepository
                .findByApprovalTypeAndActiveTrue(request.getApprovalType())
                .orElseThrow(() -> new RuntimeException("No active approval workflow found for type: " + request.getApprovalType()));

        List<ApprovalWorkflowStepEntity> steps =
                workflowStepRepository.findByWorkflowIdAndActiveTrueOrderByStepNoAsc(workflow.getId());

        if (steps.isEmpty()) {
            throw new RuntimeException("Approval workflow has no active steps");
        }

        Integer firstStepNo = steps.get(0).getStepNo();

        ApprovalRequestEntity entity = new ApprovalRequestEntity();
        entity.setApprovalType(request.getApprovalType());
        entity.setReferenceId(request.getReferenceId());
        entity.setReferenceNo(request.getReferenceNo());
        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setRequesterId(request.getRequesterId());
        entity.setWorkflowId(workflow.getId());
        entity.setCurrentStepNo(firstStepNo);
        entity.setStatus(ApprovalStatus.IN_PROGRESS);
        entity.setPayloadJson(request.getPayloadJson());
        entity.setCreatedBy(request.getRequesterId());

        entity = approvalRequestRepository.save(entity);

        recordAction(
                entity.getId(),
                firstStepNo,
                ApprovalActionType.SUBMITTED,
                request.getRequesterId(),
                "Submitted for approval"
        );

        return toResponse(entity);
    }

    @Transactional
    public ApprovalRequestResponse approve(String approvalRequestId, ApprovalDecisionRequest request) {
        ApprovalRequestEntity approvalRequest = getApprovalRequestOrThrow(approvalRequestId);

        validateCanAction(approvalRequest);

        ApprovalWorkflowStepEntity currentStep = workflowStepRepository
                .findByWorkflowIdAndStepNoAndActiveTrue(
                        approvalRequest.getWorkflowId(),
                        approvalRequest.getCurrentStepNo()
                ).orElseThrow(() -> new RuntimeException("Current approval step not found"));

        validateApprover(currentStep, request.getActionBy());

        boolean alreadyApproved = approvalActionRepository
                .existsByApprovalRequestIdAndStepNoAndActionByAndAction(
                        approvalRequestId,
                        approvalRequest.getCurrentStepNo(),
                        request.getActionBy(),
                        ApprovalActionType.APPROVED
                );

        if (alreadyApproved) {
            throw new RuntimeException("User has already approved this step");
        }

        recordAction(
                approvalRequestId,
                approvalRequest.getCurrentStepNo(),
                ApprovalActionType.APPROVED,
                request.getActionBy(),
                request.getComments()
        );

        long approvedCount = approvalActionRepository
                .countByApprovalRequestIdAndStepNoAndAction(
                        approvalRequestId,
                        approvalRequest.getCurrentStepNo(),
                        ApprovalActionType.APPROVED
                );

        if (approvedCount >= currentStep.getRequiredApprovals()) {
            moveToNextStepOrComplete(approvalRequest, request.getActionBy());
        }

        return toResponse(approvalRequestRepository.save(approvalRequest));
    }

    @Transactional
    public ApprovalRequestResponse reject(String approvalRequestId, ApprovalDecisionRequest request) {
        ApprovalRequestEntity approvalRequest = getApprovalRequestOrThrow(approvalRequestId);

        validateCanAction(approvalRequest);

        ApprovalWorkflowStepEntity currentStep = workflowStepRepository
                .findByWorkflowIdAndStepNoAndActiveTrue(
                        approvalRequest.getWorkflowId(),
                        approvalRequest.getCurrentStepNo()
                )
                .orElseThrow(() -> new RuntimeException("Current approval step not found"));

        validateApprover(currentStep, request.getActionBy());

        recordAction(
                approvalRequestId,
                approvalRequest.getCurrentStepNo(),
                ApprovalActionType.REJECTED,
                request.getActionBy(),
                request.getComments()
        );

        approvalRequest.setStatus(ApprovalStatus.REJECTED);
        approvalRequest.setFinalActionBy(request.getActionBy());
        approvalRequest.setFinalActionAt(new Date());
        approvalRequest.setUpdatedBy(request.getActionBy());

        return toResponse(approvalRequestRepository.save(approvalRequest));
    }

    @Transactional
    public ApprovalRequestResponse cancel(String approvalRequestId, ApprovalDecisionRequest request) {
        ApprovalRequestEntity approvalRequest = getApprovalRequestOrThrow(approvalRequestId);

        if (approvalRequest.getStatus() == ApprovalStatus.APPROVED ||
                approvalRequest.getStatus() == ApprovalStatus.REJECTED ||
                approvalRequest.getStatus() == ApprovalStatus.CANCELLED) {
            throw new RuntimeException("Approval request is already finalised");
        }

        recordAction(
                approvalRequestId,
                approvalRequest.getCurrentStepNo(),
                ApprovalActionType.CANCELLED,
                request.getActionBy(),
                request.getComments()
        );

        approvalRequest.setStatus(ApprovalStatus.CANCELLED);
        approvalRequest.setFinalActionBy(request.getActionBy());
        approvalRequest.setFinalActionAt(new Date());
        approvalRequest.setUpdatedBy(request.getActionBy());

        return toResponse(approvalRequestRepository.save(approvalRequest));
    }

    public ApprovalRequestResponse getById(String id) {
        return toResponse(getApprovalRequestOrThrow(id));
    }

    public List<ApprovalActionEntity> getAuditTrail(String approvalRequestId) {
        return approvalActionRepository.findByApprovalRequestIdOrderByActionAtAsc(approvalRequestId);
    }

    public List<ApprovalRequestEntity> getByStatus(ApprovalStatus status) {
        return approvalRequestRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<ApprovalRequestEntity> getByType(ApprovalType approvalType) {
        return approvalRequestRepository.findByApprovalTypeOrderByCreatedAtDesc(approvalType);
    }

    public List<ApprovalRequestEntity> getByRequester(String requesterId) {
        return approvalRequestRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId);
    }

    private void moveToNextStepOrComplete(ApprovalRequestEntity approvalRequest, String actionBy) {
        List<ApprovalWorkflowStepEntity> steps =
                workflowStepRepository.findByWorkflowIdOrderByStepNoAsc(
                        approvalRequest.getWorkflowId()
                );

        Integer currentStepNo = approvalRequest.getCurrentStepNo();

        ApprovalWorkflowStepEntity nextStep = steps.stream()
                .filter(step -> step.getStepNo() > currentStepNo)
                .findFirst()
                .orElse(null);

        if (nextStep == null) {
            approvalRequest.setStatus(ApprovalStatus.APPROVED);
            approvalRequest.setFinalActionBy(actionBy);
            approvalRequest.setFinalActionAt(new Date());

            completionHandlerRegistry.handleApproved(approvalRequest, actionBy);
        } else {
            approvalRequest.setCurrentStepNo(nextStep.getStepNo());
            approvalRequest.setStatus(ApprovalStatus.IN_PROGRESS);
        }

        approvalRequest.setUpdatedBy(actionBy);
    }

    private void validateCanAction(ApprovalRequestEntity approvalRequest) {
        if (approvalRequest.getStatus() == ApprovalStatus.APPROVED ||
                approvalRequest.getStatus() == ApprovalStatus.REJECTED ||
                approvalRequest.getStatus() == ApprovalStatus.CANCELLED) {
            throw new RuntimeException("Approval request is already finalised");
        }
    }

    /**
     * Basic validation.
     *
     * You can enhance this by checking your UserService:
     * - if approverType = USER, actionBy must equal approverValue
     * - if approverType = ROLE, user must have that role
     * - if approverType = GROUP, user must belong to group
     * - if approverType = MANAGER, user must be manager of requester
     */
    private void validateApprover(ApprovalWorkflowStepEntity step, String actionBy) {
        if (step == null) {
            throw new RuntimeException("Approval step is required");
        }

        if (actionBy == null || actionBy.isBlank()) {
            throw new RuntimeException("Action user is required");
        }

        if (step.getApprovers() == null || step.getApprovers().isEmpty()) {
            throw new RuntimeException("No approvers configured for this approval step");
        }

        boolean allowed = step.getApprovers()
                .stream()
                .filter(approver -> approver.getActive() == null || approver.getActive())
                .anyMatch(approver -> isUserAllowedForApproverRule(approver, actionBy));

        if (!allowed) {
            throw new RuntimeException("User is not allowed to approve this step");
        }
    }

    private boolean isUserAllowedForApproverRule(
            ApprovalWorkflowStepApproverEntity approver,
            String actionBy
    ) {
        if (approver.getApproverType() == null) {
            return false;
        }

        if (approver.getApproverValue() == null || approver.getApproverValue().isBlank()) {
            return false;
        }

        switch (approver.getApproverType()) {
            case USER:
                return approver.getApproverValue().equals(actionBy);

            case ROLE:
                return userHasRole(actionBy, approver.getApproverValue());

            case GROUP:
                return userBelongsToGroup(actionBy, approver.getApproverValue());

            case MANAGER:
                return isManager(actionBy);

            default:
                return false;
        }
    }
    private boolean userHasRole(String userId, String roleCode) {
        // TODO: Connect to your existing user/role table or security service.
        // Example:
        // return userRoleRepository.existsByUserIdAndRoleCode(userId, roleCode);

        return false;
    }

    private boolean userBelongsToGroup(String userId, String groupCode) {
        // TODO: Connect to your existing group/team table if you have one.
        // Example:
        // return userGroupRepository.existsByUserIdAndGroupCode(userId, groupCode);

        return false;
    }

    private boolean isManager(String userId) {
        // TODO: Connect to your employee/manager structure.
        // Example:
        // return employeeRepository.existsByUserIdAndIsManagerTrue(userId);

        return false;
    }
    private void recordAction(
            String approvalRequestId,
            Integer stepNo,
            ApprovalActionType action,
            String actionBy,
            String comments
    ) {
        ApprovalActionEntity actionEntity = new ApprovalActionEntity();
        actionEntity.setApprovalRequestId(approvalRequestId);
        actionEntity.setStepNo(stepNo);
        actionEntity.setAction(action);
        actionEntity.setActionBy(actionBy);
        actionEntity.setActionAt(new Date());
        actionEntity.setComments(comments);
        actionEntity.setCreatedBy(actionBy);

        approvalActionRepository.save(actionEntity);
    }

    private ApprovalRequestEntity getApprovalRequestOrThrow(String id) {
        return approvalRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Approval request not found: " + id));
    }

    private ApprovalRequestResponse toResponse(ApprovalRequestEntity entity) {
        return ApprovalRequestResponse.builder()
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
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
