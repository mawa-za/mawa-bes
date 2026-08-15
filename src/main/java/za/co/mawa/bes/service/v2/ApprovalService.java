package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final ApprovalSubmissionHandlerRegistry submissionHandlerRegistry;
    private final JdbcTemplate jdbcTemplate;
    private final UserInboxService userInboxService;
    private final MembershipClaimService membershipClaimService;

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
        if (request.getApprovalType() != null && request.getApprovalType().isMembershipClaimApproval()) {
            boolean existingClaimApproval = approvalRequestRepository.findByReferenceId(request.getReferenceId()).stream()
                    .anyMatch(existing -> existing.getApprovalType() != null
                            && existing.getApprovalType().isMembershipClaimApproval());
            if (existingClaimApproval) {
                throw new IllegalStateException("Approval request already exists for reference: " + request.getReferenceId());
            }
        } else {
            approvalRequestRepository
                    .findByApprovalTypeAndReferenceId(request.getApprovalType(), request.getReferenceId())
                    .ifPresent(existing -> {
                        throw new IllegalStateException("Approval request already exists for reference: " + request.getReferenceId());
                    });
        }

        ApprovalWorkflowEntity workflow = resolveWorkflow(request.getApprovalType())
                .orElseThrow(() -> new IllegalStateException(
                        "No approval workflow configured for type: " + request.getApprovalType()));

        if (!Boolean.TRUE.equals(workflow.getActive())) {
            return autoApproveWithoutWorkflow(request, workflow);
        }

        if (Boolean.TRUE.equals(workflow.getAutoApprove())) {
            return autoApproveThroughWorkflow(request, workflow);
        }

        List<ApprovalWorkflowStepEntity> steps =
                workflowStepRepository.findByWorkflowIdAndActiveTrueOrderByStepNoAsc(workflow.getId());

        if (steps.isEmpty()) {
            throw new IllegalStateException("Approval workflow has no active steps");
        }

        Integer firstStepNo = steps.get(0).getStepNo();

        ApprovalRequestEntity entity = createApprovalRequest(request, workflow, firstStepNo);
        entity = approvalRequestRepository.save(entity);

        recordAction(
                entity.getId(),
                firstStepNo,
                ApprovalActionType.SUBMITTED,
                request.getRequesterId(),
                "Submitted for approval"
        );
        submissionHandlerRegistry.handleSubmit(entity, request.getRequesterId());
        userInboxService.notifyApprovalRequired(entity);
        return toResponse(entity);
    }


    private ApprovalRequestResponse autoApproveThroughWorkflow(
            ApprovalSubmitRequest request,
            ApprovalWorkflowEntity workflow
    ) {
        ApprovalRequestEntity entity = createApprovalRequest(request, workflow, 0);
        entity = approvalRequestRepository.save(entity);

        recordAction(
                entity.getId(),
                0,
                ApprovalActionType.SUBMITTED,
                request.getRequesterId(),
                "Submitted to auto-approval workflow"
        );
        submissionHandlerRegistry.handleSubmit(entity, request.getRequesterId());

        entity.setStatus(ApprovalStatus.APPROVED);
        entity.setFinalActionBy(request.getRequesterId());
        entity.setFinalActionAt(new Date());
        entity.setUpdatedBy(request.getRequesterId());
        entity = approvalRequestRepository.save(entity);

        recordAction(
                entity.getId(),
                0,
                ApprovalActionType.APPROVED,
                request.getRequesterId(),
                "Automatically approved by workflow configuration"
        );
        completionHandlerRegistry.handleApproved(entity, request.getRequesterId());
        return toResponse(entity);
    }

    private ApprovalRequestResponse autoApproveWithoutWorkflow(
            ApprovalSubmitRequest request,
            ApprovalWorkflowEntity workflow
    ) {
        ApprovalRequestEntity entity = createApprovalRequest(request, workflow, 0);
        entity = approvalRequestRepository.save(entity);

        recordAction(
                entity.getId(),
                0,
                ApprovalActionType.SUBMITTED,
                request.getRequesterId(),
                "Submitted while approval workflow is inactive"
        );
        submissionHandlerRegistry.handleSubmit(entity, request.getRequesterId());

        entity.setStatus(ApprovalStatus.APPROVED);
        entity.setFinalActionBy(request.getRequesterId());
        entity.setFinalActionAt(new Date());
        entity.setUpdatedBy(request.getRequesterId());
        entity = approvalRequestRepository.save(entity);

        recordAction(
                entity.getId(),
                0,
                ApprovalActionType.APPROVED,
                request.getRequesterId(),
                "Auto-approved because the approval workflow is inactive"
        );
        completionHandlerRegistry.handleApproved(entity, request.getRequesterId());
        return toResponse(entity);
    }

    private ApprovalRequestEntity createApprovalRequest(
            ApprovalSubmitRequest request,
            ApprovalWorkflowEntity workflow,
            Integer currentStepNo
    ) {
        ApprovalRequestEntity entity = new ApprovalRequestEntity();
        entity.setApprovalType(request.getApprovalType());
        entity.setReferenceId(request.getReferenceId());
        entity.setReferenceNo(truncate(request.getReferenceNo(), 100));
        entity.setTitle(approvalTitle(request));
        entity.setDescription(request.getDescription());
        entity.setRequesterId(request.getRequesterId());
        entity.setWorkflowId(workflow.getId());
        entity.setCurrentStepNo(currentStepNo);
        entity.setStatus(ApprovalStatus.IN_PROGRESS);
        entity.setPayloadJson(request.getPayloadJson());
        entity.setCreatedBy(request.getRequesterId());
        return entity;
    }

    @Transactional
    public ApprovalRequestResponse approve(String approvalRequestId, ApprovalDecisionRequest request) {
        ApprovalRequestEntity approvalRequest = getApprovalRequestForUpdateOrThrow(approvalRequestId);
        String actionBy = canonicalActionUser(request.getActionBy());
        request.setActionBy(actionBy);

        validateCanAction(approvalRequest);

        ApprovalWorkflowStepEntity currentStep = workflowStepRepository
                .findByWorkflowIdAndStepNoAndActiveTrue(
                        approvalRequest.getWorkflowId(),
                        approvalRequest.getCurrentStepNo()
                ).orElseThrow(() -> new RuntimeException("Current approval step not found"));

        validateApprover(currentStep, request.getActionBy(), approvalRequest);

        boolean alreadyActioned = approvalActionRepository
                .existsByApprovalRequestIdAndStepNoAndActionByAndActionIn(
                        approvalRequestId, approvalRequest.getCurrentStepNo(), request.getActionBy(),
                        List.of(ApprovalActionType.APPROVED, ApprovalActionType.REJECTED));
        if (alreadyActioned) {
            throw new IllegalStateException("You have already actioned this approval step");
        }

        Integer actionedStepNo = approvalRequest.getCurrentStepNo();
        ApprovalActionEntity action = recordAction(
                approvalRequestId,
                actionedStepNo,
                ApprovalActionType.APPROVED,
                request.getActionBy(),
                request.getComments()
        );

        long approvedCount = approvalActionRepository
                .countByApprovalRequestIdAndStepNoAndAction(
                        approvalRequestId,
                        actionedStepNo,
                        ApprovalActionType.APPROVED
                );

        if (approvedCount >= currentStep.getRequiredApprovals()) {
            userInboxService.resolveApprovalStep(approvalRequestId, actionedStepNo);
            moveToNextStepOrComplete(approvalRequest, request);
        } else {
            userInboxService.resolveApprovalStepForUser(
                    approvalRequestId, actionedStepNo, request.getActionBy());
        }

        ApprovalRequestEntity saved = approvalRequestRepository.save(approvalRequest);
        userInboxService.notifyRequesterActioned(saved, action, request.getActionBy());
        if ((saved.getStatus() == ApprovalStatus.IN_PROGRESS || saved.getStatus() == ApprovalStatus.PENDING)
                && !actionedStepNo.equals(saved.getCurrentStepNo())) {
            userInboxService.notifyApprovalRequired(saved);
        }
        return toResponse(saved);
    }

    @Transactional
    public ApprovalRequestResponse reject(String approvalRequestId, ApprovalDecisionRequest request) {
        ApprovalRequestEntity approvalRequest = getApprovalRequestForUpdateOrThrow(approvalRequestId);
        String actionBy = canonicalActionUser(request.getActionBy());
        request.setActionBy(actionBy);

        validateCanAction(approvalRequest);

        ApprovalWorkflowStepEntity currentStep = workflowStepRepository
                .findByWorkflowIdAndStepNoAndActiveTrue(
                        approvalRequest.getWorkflowId(),
                        approvalRequest.getCurrentStepNo()
                )
                .orElseThrow(() -> new RuntimeException("Current approval step not found"));

        validateApprover(currentStep, request.getActionBy(), approvalRequest);

        boolean alreadyActioned = approvalActionRepository
                .existsByApprovalRequestIdAndStepNoAndActionByAndActionIn(
                        approvalRequestId, approvalRequest.getCurrentStepNo(), request.getActionBy(),
                        List.of(ApprovalActionType.APPROVED, ApprovalActionType.REJECTED));
        if (alreadyActioned) {
            throw new IllegalStateException("You have already actioned this approval step");
        }

        Integer actionedStepNo = approvalRequest.getCurrentStepNo();
        ApprovalActionEntity action = recordAction(
                approvalRequestId,
                actionedStepNo,
                ApprovalActionType.REJECTED,
                request.getActionBy(),
                request.getComments()
        );
        userInboxService.resolveApprovalStep(approvalRequestId, actionedStepNo);

        approvalRequest.setStatus(ApprovalStatus.REJECTED);
        approvalRequest.setFinalActionBy(request.getActionBy());
        approvalRequest.setFinalActionAt(new Date());
        approvalRequest.setUpdatedBy(request.getActionBy());

        ApprovalRequestEntity saved = approvalRequestRepository.save(approvalRequest);
        completionHandlerRegistry.handleRejected(saved, request.getActionBy());
        userInboxService.notifyRequesterActioned(saved, action, request.getActionBy());
        return toResponse(saved);
    }

    @Transactional
    public ApprovalRequestResponse cancel(String approvalRequestId, ApprovalDecisionRequest request) {
        ApprovalRequestEntity approvalRequest = getApprovalRequestForUpdateOrThrow(approvalRequestId);
        String actionBy = canonicalActionUser(request.getActionBy());
        request.setActionBy(actionBy);

        if (approvalRequest.getStatus() == ApprovalStatus.APPROVED ||
                approvalRequest.getStatus() == ApprovalStatus.REJECTED ||
                approvalRequest.getStatus() == ApprovalStatus.CANCELLED) {
            throw new RuntimeException("Approval request is already finalised");
        }

        Integer actionedStepNo = approvalRequest.getCurrentStepNo();
        ApprovalActionEntity action = recordAction(
                approvalRequestId,
                actionedStepNo,
                ApprovalActionType.CANCELLED,
                request.getActionBy(),
                request.getComments()
        );
        userInboxService.resolveApprovalStep(approvalRequestId, actionedStepNo);

        approvalRequest.setStatus(ApprovalStatus.CANCELLED);
        approvalRequest.setFinalActionBy(request.getActionBy());
        approvalRequest.setFinalActionAt(new Date());
        approvalRequest.setUpdatedBy(request.getActionBy());

        ApprovalRequestEntity saved = approvalRequestRepository.save(approvalRequest);
        completionHandlerRegistry.handleCancelled(saved, request.getActionBy());
        userInboxService.notifyRequesterActioned(saved, action, request.getActionBy());
        return toResponse(saved);
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

    public List<ApprovalRequestEntity> search(
            ApprovalStatus status,
            ApprovalType approvalType,
            String requesterId
    ) {
        if (status != null && approvalType != null) {
            return approvalRequestRepository
                    .findByStatusAndApprovalTypeOrderByCreatedAtDesc(status, approvalType);
        }
        if (status != null) return getByStatus(status);
        if (approvalType != null) return getByType(approvalType);
        if (requesterId != null && !requesterId.isBlank()) return getByRequester(requesterId);
        return approvalRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    private void moveToNextStepOrComplete(ApprovalRequestEntity approvalRequest, ApprovalDecisionRequest decision) {
        List<ApprovalWorkflowStepEntity> steps =
                workflowStepRepository.findByWorkflowIdOrderByStepNoAsc(
                        approvalRequest.getWorkflowId()
                );

        Integer currentStepNo = approvalRequest.getCurrentStepNo();
        String actionBy = decision.getActionBy();

        ApprovalWorkflowStepEntity nextStep = steps.stream()
                .filter(step -> step.getStepNo() > currentStepNo)
                .findFirst()
                .orElse(null);

        if (nextStep == null) {
            if (approvalRequest.getApprovalType() != null
                    && approvalRequest.getApprovalType().isMembershipClaimApproval()) {
                membershipClaimService.applyArrearsFineForApproval(
                        approvalRequest.getReferenceId(),
                        decision.getArrearsMonths(),
                        actionBy
                );
            }

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

    private java.util.Optional<ApprovalWorkflowEntity> resolveWorkflow(ApprovalType approvalType) {
        if (approvalType == null) return java.util.Optional.empty();
        java.util.Optional<ApprovalWorkflowEntity> exact = workflowRepository.findByApprovalType(approvalType);
        if (exact.isPresent()) return exact;
        if (approvalType != null && approvalType.isMembershipClaimApproval() && approvalType != ApprovalType.CLAIM) {
            return workflowRepository.findByApprovalType(ApprovalType.CLAIM);
        }
        return java.util.Optional.empty();
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
     * <p>
     * You can enhance this by checking your UserService:
     * - if approverType = USER, actionBy must equal approverValue
     * - if approverType = ROLE, user must have that role
     * - if approverType = GROUP, user must belong to group
     * - if approverType = MANAGER, user must be manager of requester
     */
    private void validateApprover(ApprovalWorkflowStepEntity step, String actionBy, ApprovalRequestEntity request) {
        if (step == null) {
            throw new RuntimeException("Approval step is required");
        }

        if (actionBy == null || actionBy.isBlank()) {
            throw new IllegalArgumentException("Action user is required");
        }

        if (step.getApprovers() == null || step.getApprovers().isEmpty()) {
            throw new IllegalStateException("No approvers configured for this approval step");
        }

        boolean allowed = step.getApprovers()
                .stream()
                .filter(approver -> approver.getActive() == null || approver.getActive())
                .anyMatch(approver -> isUserAllowedForApproverRule(approver, actionBy, request));

        if (!allowed) {
            throw new SecurityException("User is not allowed to approve this step");
        }
    }

    private boolean isUserAllowedForApproverRule(
            ApprovalWorkflowStepApproverEntity approver,
            String actionBy,
            ApprovalRequestEntity request
    ) {
        if (approver.getApproverType() == null) {
            return false;
        }

        if (approver.getApproverValue() == null || approver.getApproverValue().isBlank()) {
            return false;
        }

        switch (approver.getApproverType()) {
            case USER:
                return matchesUser(actionBy, approver.getApproverValue());
            case ROLE:
                return userHasRole(actionBy, approver.getApproverValue());

            case GROUP:
                return userBelongsToGroup(actionBy, approver.getApproverValue());

            case MANAGER:
                return isManagerOfRequester(actionBy, request == null ? null : request.getRequesterId());

            default:
                return false;
        }
    }

    private boolean matchesUser(String actionBy, String configuredUser) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM `user`
                 WHERE status = 'ACTIVE'
                   AND (expires_at IS NULL OR expires_at > NOW())
                   AND (id = ? OR username = ? OR email = ?)
                """, Integer.class, configuredUser, configuredUser, configuredUser);
        if (count == null || count == 0) return false;
        return actionBy.equals(configuredUser) || Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) > 0 FROM `user`
                 WHERE (id = ? OR username = ? OR email = ?)
                   AND (id = ? OR username = ? OR email = ?)
                """, Boolean.class, actionBy, actionBy, actionBy, configuredUser, configuredUser, configuredUser));
    }

    private boolean userHasRole(String userId, String roleCode) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM user_role ur
                  JOIN `user` u ON u.id = ur.user
                  JOIN role r ON r.id = ur.role
                 WHERE (u.id = ? OR u.username = ? OR u.email = ?)
                   AND (r.id = ? OR UPPER(r.description) = UPPER(?))
                   AND u.status = 'ACTIVE'
                   AND (u.expires_at IS NULL OR u.expires_at > NOW())
                   AND (ur.valid_from IS NULL OR ur.valid_from <= CURRENT_DATE)
                   AND (ur.valid_to IS NULL OR ur.valid_to >= CURRENT_DATE)
                """, Integer.class, userId, userId, userId, roleCode, roleCode);
        return count != null && count > 0;
    }

    private boolean userBelongsToGroup(String userId, String groupCode) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM approval_group_member gm
                  JOIN `user` u ON u.id = gm.user_id
                 WHERE gm.group_code = ? AND gm.active = 1
                   AND (u.id = ? OR u.username = ? OR u.email = ?)
                   AND u.status = 'ACTIVE'
                   AND (u.expires_at IS NULL OR u.expires_at > NOW())
                """, Integer.class, groupCode, userId, userId, userId);
        return count != null && count > 0;
    }

    private boolean isManagerOfRequester(String managerUser, String requesterUser) {
        if (requesterUser == null || requesterUser.isBlank()) return false;
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM approval_manager_assignment ma
                 WHERE ma.active = 1
                   AND ma.manager_user_id IN (SELECT id FROM `user` WHERE id = ? OR username = ? OR email = ?)
                   AND ma.requester_user_id IN (SELECT id FROM `user` WHERE id = ? OR username = ? OR email = ?)
                """, Integer.class, managerUser, managerUser, managerUser,
                requesterUser, requesterUser, requesterUser);
        return count != null && count > 0;
    }

    private ApprovalActionEntity recordAction(
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

        return approvalActionRepository.save(actionEntity);
    }

    private String canonicalActionUser(String identity) {
        String canonical = userInboxService.canonicalUserId(identity);
        if (canonical == null || canonical.isBlank()) {
            throw new IllegalArgumentException("Action user is required");
        }
        return canonical;
    }

    private ApprovalRequestEntity getApprovalRequestForUpdateOrThrow(String id) {
        return approvalRequestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found: " + id));
    }

    private ApprovalRequestEntity getApprovalRequestOrThrow(String id) {
        return approvalRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found: " + id));
    }

    private String approvalTitle(ApprovalSubmitRequest request) {
        String title = request.getTitle();
        if (title == null || title.isBlank()) {
            String type = request.getApprovalType() == null
                    ? "Approval"
                    : request.getApprovalType().name().replace('_', ' ');
            String reference = request.getReferenceNo();
            title = reference == null || reference.isBlank()
                    ? type + " approval request"
                    : type + " approval request - " + reference.trim();
        }
        return truncate(title.trim(), 255);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
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
