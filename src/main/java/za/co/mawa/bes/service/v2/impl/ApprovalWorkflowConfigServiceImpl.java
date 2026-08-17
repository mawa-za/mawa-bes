package za.co.mawa.bes.service.v2.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.approval.*;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowEntity;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowStepApproverEntity;
import za.co.mawa.bes.entity.v2.ApprovalWorkflowStepEntity;
import za.co.mawa.bes.enums.ApprovalMode;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.enums.ApproverType;
import za.co.mawa.bes.repository.v2.ApprovalWorkflowRepository;
import za.co.mawa.bes.service.v2.ApprovalWorkflowConfigService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalWorkflowConfigServiceImpl implements ApprovalWorkflowConfigService {

    private final ApprovalWorkflowRepository workflowRepository;

    @Override
    public ApprovalWorkflowResponseDto create(ApprovalWorkflowRequestDto request) {
        validateWorkflowRequest(request);

        if (workflowRepository.existsByApprovalType(request.getApprovalType())) {
            throw new RuntimeException("Approval workflow already exists for type: " + request.getApprovalType());
        }

        ApprovalWorkflowEntity entity = new ApprovalWorkflowEntity();
        applyWorkflowFields(entity, request);
        entity.setCreatedAt(LocalDateTime.now());

        applySteps(entity, request.getSteps());
        if (Boolean.TRUE.equals(entity.getActive())) {
            validateCanActivate(entity);
        }

        return toDto(workflowRepository.save(entity));
    }

    @Override
    public ApprovalWorkflowResponseDto update(String id, ApprovalWorkflowRequestDto request) {
        validateWorkflowRequest(request);

        ApprovalWorkflowEntity entity = getEntity(id);

        if (entity.getApprovalType() != request.getApprovalType()) {
            throw new RuntimeException("Approval type cannot be changed after a workflow is created");
        }

        workflowRepository.findByApprovalType(request.getApprovalType())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new RuntimeException("Approval workflow already exists for type: " + request.getApprovalType());
                    }
                });

        applyWorkflowFields(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());

        entity.getSteps().clear();
        // Flush orphan removals before recreating numbered steps. Without this flush,
        // Hibernate may INSERT step 1 before deleting the old step 1 and violate
        // uq_workflow_step_no (workflow_id, step_no).
        workflowRepository.flush();
        applySteps(entity, request.getSteps());
        if (Boolean.TRUE.equals(entity.getActive())) {
            validateCanActivate(entity);
        }

        return toDto(workflowRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalWorkflowResponseDto getById(String id) {
        return toDto(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalWorkflowResponseDto getByApprovalType(ApprovalType approvalType) {
        ApprovalWorkflowEntity entity = workflowRepository.findByApprovalType(approvalType)
                .orElseThrow(() -> new RuntimeException("Approval workflow not found for type: " + approvalType));

        return toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalWorkflowResponseDto getActiveByApprovalType(ApprovalType approvalType) {
        ApprovalWorkflowEntity entity = workflowRepository.findByApprovalTypeAndActiveTrue(approvalType)
                .orElseThrow(() -> new RuntimeException("Active approval workflow not found for type: " + approvalType));

        return toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalWorkflowResponseDto findApplicableWorkflow(ApprovalType approvalType, BigDecimal amount) {
        ApprovalWorkflowEntity entity = workflowRepository.findByApprovalTypeAndActiveTrue(approvalType)
                .orElseThrow(() -> new RuntimeException("Active approval workflow not found for type: " + approvalType));

        if (!isAmountApplicable(entity, amount)) {
            throw new RuntimeException("No applicable approval workflow found for type " + approvalType + " and amount " + amount);
        }

        return toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalWorkflowResponseDto> getAll() {
        return workflowRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalWorkflowResponseDto> getActive() {
        return workflowRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void activate(String id) {
        ApprovalWorkflowEntity entity = getEntity(id);
        validateCanActivate(entity);
        entity.setActive(true);
        entity.setUpdatedAt(LocalDateTime.now());
        workflowRepository.save(entity);
    }

    @Override
    public void deactivate(String id) {
        ApprovalWorkflowEntity entity = getEntity(id);
        entity.setActive(false);
        entity.setUpdatedAt(LocalDateTime.now());
        workflowRepository.save(entity);
    }

    @Override
    public void delete(String id) {
        getEntity(id);
        throw new IllegalStateException(
                "Approval workflows cannot be deleted. Deactivate the workflow to disable approval for new requests."
        );
    }

    private ApprovalWorkflowEntity getEntity(String id) {
        return workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Approval workflow not found: " + id));
    }

    private void applyWorkflowFields(ApprovalWorkflowEntity entity, ApprovalWorkflowRequestDto request) {
        entity.setApprovalType(request.getApprovalType());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setMinAmount(request.getMinAmount());
        entity.setMaxAmount(request.getMaxAmount());
        entity.setActive(request.getActive() == null || request.getActive());
        entity.setAutoApprove(Boolean.TRUE.equals(request.getAutoApprove()));
    }

    private void applySteps(ApprovalWorkflowEntity workflow, List<ApprovalWorkflowStepRequestDto> stepRequests) {
        if (stepRequests == null || stepRequests.isEmpty()) {
            return;
        }
        validateStepNumbers(stepRequests);

        for (ApprovalWorkflowStepRequestDto stepRequest : stepRequests) {
            ApprovalWorkflowStepEntity step = new ApprovalWorkflowStepEntity();

            step.setWorkflow(workflow);
            step.setStepNo(stepRequest.getStepNo());
            step.setStepName(stepRequest.getStepName());
            step.setApprovalMode(stepRequest.getApprovalMode() == null ? ApprovalMode.ANY_ONE : stepRequest.getApprovalMode());
            step.setRequiredApprovals(stepRequest.getRequiredApprovals() == null ? 1 : stepRequest.getRequiredApprovals());
            step.setActive(stepRequest.getActive() == null || stepRequest.getActive());
            step.setCreatedAt(LocalDateTime.now());

            validateStepRequest(stepRequest, workflow.getApprovalType());

            for (ApprovalWorkflowStepApproverRequestDto approverRequest : stepRequest.getApprovers()) {
                ApprovalWorkflowStepApproverEntity approver = new ApprovalWorkflowStepApproverEntity();

                approver.setWorkflowStep(step);
                approver.setApproverType(approverRequest.getApproverType());
                approver.setApproverValue(approverRequest.getApproverValue());
                approver.setApproverName(approverRequest.getApproverName());
                approver.setAssignmentScopeType(normaliseScopeType(approverRequest.getAssignmentScopeType()));
                approver.setAssignmentScopeValue(normaliseScopeValue(
                        approver.getAssignmentScopeType(), approverRequest.getAssignmentScopeValue()));
                approver.setActive(approverRequest.getActive() == null || approverRequest.getActive());
                approver.setCreatedAt(LocalDateTime.now());

                step.getApprovers().add(approver);
            }

            workflow.getSteps().add(step);
        }
    }

    private void validateWorkflowRequest(ApprovalWorkflowRequestDto request) {
        if (request == null) {
            throw new RuntimeException("Request is required");
        }

        if (request.getApprovalType() == null) {
            throw new RuntimeException("Approval type is required");
        }

        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Workflow name is required");
        }

        if (request.getMinAmount() != null && request.getMaxAmount() != null) {
            if (request.getMinAmount().compareTo(request.getMaxAmount()) > 0) {
                throw new RuntimeException("Minimum amount cannot be greater than maximum amount");
            }
        }

        if (!Boolean.TRUE.equals(request.getAutoApprove())
                && (request.getSteps() == null || request.getSteps().isEmpty())) {
            throw new RuntimeException("At least one workflow step is required unless auto approval is enabled");
        }
    }

    private void validateStepRequest(ApprovalWorkflowStepRequestDto stepRequest, ApprovalType approvalType) {
        if (stepRequest.getStepNo() == null) {
            throw new RuntimeException("Step number is required");
        }

        if (stepRequest.getStepName() == null || stepRequest.getStepName().isBlank()) {
            throw new RuntimeException("Step name is required");
        }

        if (stepRequest.getRequiredApprovals() != null && stepRequest.getRequiredApprovals() < 1) {
            throw new RuntimeException("Required approvals must be at least 1");
        }

        if (stepRequest.getApprovers() == null || stepRequest.getApprovers().isEmpty()) {
            throw new RuntimeException("Step " + stepRequest.getStepNo() + " requires at least one approver");
        }

        for (ApprovalWorkflowStepApproverRequestDto approver : stepRequest.getApprovers()) {
            if (approver.getApproverType() == null) {
                throw new RuntimeException("Approver type is required for step " + stepRequest.getStepNo());
            }

            if (approver.getApproverValue() == null || approver.getApproverValue().isBlank()) {
                throw new RuntimeException("Approver value is required for step " + stepRequest.getStepNo());
            }

            if (approvalType == ApprovalType.LEAVE) {
                if (!Set.of(ApproverType.ROLE, ApproverType.USER).contains(approver.getApproverType())) {
                    throw new RuntimeException("Leave approvers must be configured by role or specific employee");
                }

                String scopeType = normaliseScopeType(approver.getAssignmentScopeType());
                if (!Set.of("ALL", "POSITION", "EMPLOYEE").contains(scopeType)) {
                    throw new RuntimeException("Leave approver assignment scope must be ALL, POSITION or EMPLOYEE");
                }
                if (!"ALL".equals(scopeType)
                        && (approver.getAssignmentScopeValue() == null || approver.getAssignmentScopeValue().isBlank())) {
                    throw new RuntimeException("Assignment scope value is required for " + scopeType.toLowerCase(Locale.ROOT));
                }
            }
        }
    }

    private void validateStepNumbers(List<ApprovalWorkflowStepRequestDto> steps) {
        Set<Integer> stepNos = new HashSet<>();

        for (ApprovalWorkflowStepRequestDto step : steps) {
            if (step.getStepNo() == null) {
                throw new RuntimeException("Step number is required");
            }

            if (!stepNos.add(step.getStepNo())) {
                throw new RuntimeException("Duplicate step number found: " + step.getStepNo());
            }
        }
    }

    private void validateCanActivate(ApprovalWorkflowEntity workflow) {
        if (Boolean.TRUE.equals(workflow.getAutoApprove())) {
            return;
        }

        List<ApprovalWorkflowStepEntity> activeSteps = workflow.getSteps() == null
                ? List.of()
                : workflow.getSteps().stream()
                .filter(step -> Boolean.TRUE.equals(step.getActive()))
                .sorted(Comparator.comparing(ApprovalWorkflowStepEntity::getStepNo))
                .toList();

        if (activeSteps.isEmpty()) {
            throw new RuntimeException("An active workflow requires at least one active approval step");
        }

        for (ApprovalWorkflowStepEntity step : activeSteps) {
            long activeApprovers = step.getApprovers() == null
                    ? 0
                    : step.getApprovers().stream()
                    .filter(approver -> Boolean.TRUE.equals(approver.getActive()))
                    .count();
            int requiredApprovals = step.getRequiredApprovals() == null ? 1 : step.getRequiredApprovals();
            if (activeApprovers < requiredApprovals) {
                throw new RuntimeException(
                        "Step " + step.getStepNo() + " requires " + requiredApprovals
                                + " active approver(s), but only " + activeApprovers + " are configured"
                );
            }
        }
    }

    private boolean isAmountApplicable(ApprovalWorkflowEntity workflow, BigDecimal amount) {
        if (amount == null) {
            return true;
        }

        boolean minOk = workflow.getMinAmount() == null || amount.compareTo(workflow.getMinAmount()) >= 0;
        boolean maxOk = workflow.getMaxAmount() == null || amount.compareTo(workflow.getMaxAmount()) <= 0;

        return minOk && maxOk;
    }

    private ApprovalWorkflowResponseDto toDto(ApprovalWorkflowEntity entity) {
        ApprovalWorkflowResponseDto dto = new ApprovalWorkflowResponseDto();

        dto.setId(entity.getId());
        dto.setApprovalType(entity.getApprovalType());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setMinAmount(entity.getMinAmount());
        dto.setMaxAmount(entity.getMaxAmount());
        dto.setActive(entity.getActive());
        dto.setAutoApprove(Boolean.TRUE.equals(entity.getAutoApprove()));
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        dto.setSteps(
                entity.getSteps() == null
                        ? new ArrayList<>()
                        : entity.getSteps()
                        .stream()
                        .sorted(Comparator.comparing(ApprovalWorkflowStepEntity::getStepNo))
                        .map(this::toStepDto)
                        .collect(Collectors.toList())
        );

        return dto;
    }

    private ApprovalWorkflowStepResponseDto toStepDto(ApprovalWorkflowStepEntity entity) {
        ApprovalWorkflowStepResponseDto dto = new ApprovalWorkflowStepResponseDto();

        dto.setId(entity.getId());
        dto.setStepNo(entity.getStepNo());
        dto.setStepName(entity.getStepName());
        dto.setApprovalMode(entity.getApprovalMode());
        dto.setRequiredApprovals(entity.getRequiredApprovals());
        dto.setActive(entity.getActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        dto.setApprovers(
                entity.getApprovers() == null
                        ? new ArrayList<>()
                        : entity.getApprovers()
                        .stream()
                        .map(this::toApproverDto)
                        .collect(Collectors.toList())
        );

        return dto;
    }

    private String normaliseScopeType(String value) {
        if (value == null || value.isBlank()) return "ALL";
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normaliseScopeValue(String scopeType, String value) {
        if ("ALL".equals(scopeType)) return null;
        return value == null ? null : value.trim();
    }

    private ApprovalWorkflowStepApproverResponseDto toApproverDto(ApprovalWorkflowStepApproverEntity entity) {
        ApprovalWorkflowStepApproverResponseDto dto = new ApprovalWorkflowStepApproverResponseDto();

        dto.setId(entity.getId());
        dto.setApproverType(entity.getApproverType());
        dto.setApproverValue(entity.getApproverValue());
        dto.setApproverName(entity.getApproverName());
        dto.setAssignmentScopeType(normaliseScopeType(entity.getAssignmentScopeType()));
        dto.setAssignmentScopeValue(entity.getAssignmentScopeValue());
        dto.setActive(entity.getActive());

        return dto;
    }
}