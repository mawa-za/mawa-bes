package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.EmploymentDto;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.v2.*;
import za.co.mawa.bes.entity.EmploymentEntity;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.entity.v2.*;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.repository.AttachmentRepository;
import za.co.mawa.bes.repository.EmploymentRepository;
import za.co.mawa.bes.repository.UserRepository;
import za.co.mawa.bes.repository.v2.EmployeeNumberAssignmentRepository;
import za.co.mawa.bes.repository.v2.EmploymentActionRequestRepository;
import za.co.mawa.bes.repository.v2.EmploymentStatusHistoryRepository;
import za.co.mawa.bes.service.FieldOptionService;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.utils.*;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class EmploymentLifecycleService {
    private static final String AWAITING_APPROVAL = "AWAITING-APPROVAL";
    private static final List<String> OPEN_STATUSES = List.of(AWAITING_APPROVAL, "PENDING", "IN_PROGRESS");
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final EmploymentActionRequestRepository actionRepository;
    private final EmploymentStatusHistoryRepository historyRepository;
    private final AttachmentRepository attachmentRepository;
    private final EmploymentRepository employmentRepository;
    private final EmployeeNumberAssignmentRepository employeeNumberRepository;
    private final NumberAllocationService numberAllocationService;
    private final ApprovalService approvalService;
    private final PartnerService partnerService;
    private final FieldOptionService fieldOptionService;
    private final LeaveConfigurationService leaveConfigurationService;
    private final LeaveBalanceService leaveBalanceService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public EmploymentLifecycleService(
            EmploymentActionRequestRepository actionRepository,
            EmploymentStatusHistoryRepository historyRepository,
            AttachmentRepository attachmentRepository,
            EmploymentRepository employmentRepository,
            EmployeeNumberAssignmentRepository employeeNumberRepository,
            NumberAllocationService numberAllocationService,
            ApprovalService approvalService,
            PartnerService partnerService,
            FieldOptionService fieldOptionService,
            LeaveConfigurationService leaveConfigurationService,
            LeaveBalanceService leaveBalanceService,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.actionRepository = actionRepository;
        this.historyRepository = historyRepository;
        this.attachmentRepository = attachmentRepository;
        this.employmentRepository = employmentRepository;
        this.employeeNumberRepository = employeeNumberRepository;
        this.numberAllocationService = numberAllocationService;
        this.approvalService = approvalService;
        this.partnerService = partnerService;
        this.fieldOptionService = fieldOptionService;
        this.leaveConfigurationService = leaveConfigurationService;
        this.leaveBalanceService = leaveBalanceService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EmploymentActionResponseDto requestHire(EmploymentActionRequestDto request) {
        validateCommon(request, "HIRE");
        String partnerId = request.getPartnerId().trim();
        validatePartner(partnerId);
        if (employmentRepository.existsByPartnerIdAndStatusIn(partnerId, List.of(Status.ACTIVE, Status.SUSPENDED))) {
            throw new IllegalStateException("The selected person already has an active or suspended employment record");
        }
        if (actionRepository.existsByPartnerIdAndStatusIn(partnerId, OPEN_STATUSES)) {
            throw new IllegalStateException("An employment action is already awaiting approval for this person");
        }
        LocalDate start = request.getStartDate() != null ? request.getStartDate()
                : request.getEffectiveDate() != null ? request.getEffectiveDate() : LocalDate.now();
        String type = validateOption(Field.EMPLOYMENT_TYPE, request.getType(), "Employment type", true);
        String position = validateOption(Field.EMPLOYMENT_POSITION, request.getPosition(), "Position", true);
        String branch = validateOption(Field.BRANCH, request.getBranch(), "Branch", false);
        String department = validateOption(Field.DEPARTMENT, request.getDepartment(), "Department", false);
        LocalDate proposedEnd = request.getEndDate() == null ? MAX_DATE : request.getEndDate();
        if (proposedEnd.isBefore(start)) throw new IllegalArgumentException("End date cannot be before start date");
        EmploymentActionRequestEntity action = EmploymentActionRequestEntity.builder()
                .requestNumber(numberAllocationService.allocateNumber("EMPLOYMENT_ACTION"))
                .actionType("HIRE").partnerId(partnerId).proposedType(type)
                .proposedStartDate(start).proposedEndDate(proposedEnd)
                .proposedPosition(position).proposedBranch(branch).proposedDepartment(department)
                .effectiveDate(start).reason(required(request.getReason(), "Reason", 1000))
                .affectsPayroll(defaultBoolean(request.getAffectsPayroll(), true))
                .suspendSystemAccess(false).attachmentObjectIds(json(request.getAttachmentObjectIds()))
                .status(AWAITING_APPROVAL).requestedBy(actor()).version(0L).build();
        action = actionRepository.save(action);
        submitForApproval(action, ApprovalType.EMPLOYEE_HIRE);
        return toResponse(actionRepository.findById(action.getId()).orElseThrow());
    }

    @Transactional
    public EmploymentActionResponseDto requestAction(String employmentId, String actionType, EmploymentActionRequestDto request) {
        EmploymentEntity employment = requireEmployment(employmentId);
        String normalizedAction = normalizeAction(actionType);
        validateCommon(request, normalizedAction);
        if (actionRepository.existsByEmploymentIdAndStatusIn(employmentId, OPEN_STATUSES)) {
            throw new IllegalStateException("An employment action is already awaiting approval for this employment record");
        }
        if ("SUSPEND".equals(normalizedAction) && !Status.ACTIVE.equalsIgnoreCase(employment.getStatus())) {
            throw new IllegalStateException("Only active employment can be suspended");
        }
        if ("TERMINATE".equals(normalizedAction)
                && !Status.ACTIVE.equalsIgnoreCase(employment.getStatus())
                && !Status.SUSPENDED.equalsIgnoreCase(employment.getStatus())) {
            throw new IllegalStateException("Only active or suspended employment can be terminated");
        }
        if ("REINSTATE".equals(normalizedAction) && !Status.SUSPENDED.equalsIgnoreCase(employment.getStatus())) {
            throw new IllegalStateException("Only a suspended employment can be reinstated");
        }
        if ("REHIRE".equals(normalizedAction) && !Status.TERMINATED.equalsIgnoreCase(employment.getStatus())) {
            throw new IllegalStateException("Only a terminated employment can be rehired");
        }
        if (Set.of("SUSPEND", "TERMINATE").contains(normalizedAction)
                && !hasUploadedAttachments(request.getAttachmentObjectIds())) {
            throw new IllegalArgumentException("Supporting documentation is required for "
                    + normalizedAction.toLowerCase(Locale.ROOT) + " requests");
        }
        LocalDate effective = request.getEffectiveDate() == null ? LocalDate.now() : request.getEffectiveDate();
        LocalDate employmentStart = toLocalDate(employment.getStartDate());
        LocalDate previousEndDate = toLocalDate(employment.getEndDate());
        if ("REHIRE".equals(normalizedAction)) {
            if (previousEndDate == null) throw new IllegalStateException("The terminated employment does not have an end date");
            if (!effective.isAfter(previousEndDate)) {
                throw new IllegalArgumentException("Rehire date must be after the previous employment end date");
            }
            LocalDate proposedEnd = request.getEndDate() == null ? MAX_DATE : request.getEndDate();
            if (proposedEnd.isBefore(effective)) {
                throw new IllegalArgumentException("Rehire end date cannot be before the rehire start date");
            }
        } else {
            if (employmentStart != null && effective.isBefore(employmentStart)) {
                throw new IllegalArgumentException("Effective date cannot be before the employment start date");
            }
            if (previousEndDate != null && effective.isAfter(previousEndDate)) {
                throw new IllegalArgumentException("Effective date cannot be after the employment end date");
            }
        }
        EmploymentActionRequestEntity action = EmploymentActionRequestEntity.builder()
                .requestNumber(numberAllocationService.allocateNumber("EMPLOYMENT_ACTION"))
                .actionType(normalizedAction).employmentId(employmentId).partnerId(employment.getPartnerId())
                .proposedType(hasText(request.getType()) ? validateOption(Field.EMPLOYMENT_TYPE, request.getType(), "Employment type", true) : employment.getType())
                .proposedStartDate("REHIRE".equals(normalizedAction) ? effective : toLocalDate(employment.getStartDate()))
                .proposedEndDate(request.getEndDate() == null ? ("REHIRE".equals(normalizedAction) ? MAX_DATE : toLocalDate(employment.getEndDate())) : request.getEndDate())
                .proposedPosition(hasText(request.getPosition()) ? validateOption(Field.EMPLOYMENT_POSITION, request.getPosition(), "Position", true) : employment.getPosition())
                .proposedBranch(hasText(request.getBranch()) ? validateOption(Field.BRANCH, request.getBranch(), "Branch", false) : employment.getBranch())
                .proposedDepartment(hasText(request.getDepartment()) ? validateOption(Field.DEPARTMENT, request.getDepartment(), "Department", false) : employment.getDepartment())
                .effectiveDate(effective).expectedReturnDate(request.getExpectedReturnDate())
                .reason(required(request.getReason(), "Reason", 1000))
                .affectsPayroll(defaultBoolean(request.getAffectsPayroll(), false))
                .suspendSystemAccess(defaultBoolean(request.getSuspendSystemAccess(), false))
                .attachmentObjectIds(json(request.getAttachmentObjectIds()))
                .status(AWAITING_APPROVAL).requestedBy(actor()).version(0L).build();
        if ("SUSPEND".equals(normalizedAction) && action.getExpectedReturnDate() != null && action.getExpectedReturnDate().isBefore(effective)) {
            throw new IllegalArgumentException("Expected return date cannot be before the suspension date");
        }
        action = actionRepository.save(action);
        submitForApproval(action, approvalType(normalizedAction));
        return toResponse(actionRepository.findById(action.getId()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public List<EmploymentActionResponseDto> listActions(String status, String actionType) {
        return actionRepository.findAllByOrderByRequestedAtDesc().stream()
                .filter(a -> !hasText(status) || status.equalsIgnoreCase(a.getStatus()))
                .filter(a -> !hasText(actionType) || normalizeAction(actionType).equalsIgnoreCase(a.getActionType()))
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EmploymentActionResponseDto getAction(String id) { return toResponse(requireAction(id)); }

    @Transactional(readOnly = true)
    public List<EmploymentHistoryDto> history(String employmentId) {
        List<EmploymentStatusHistoryEntity> values = hasText(employmentId)
                ? historyRepository.findByEmploymentIdOrderByEffectiveDateDescChangedAtDesc(employmentId)
                : historyRepository.findAllByOrderByChangedAtDesc();
        return values.stream().map(this::toHistory).toList();
    }

    @Transactional
    public void approveAction(String actionId, String approvalRequestId, String actionBy) {
        EmploymentActionRequestEntity action = requireActionForUpdate(actionId);
        if (Status.APPROVED.equalsIgnoreCase(action.getStatus())) return;
        EmploymentEntity result = switch (action.getActionType()) {
            case "HIRE" -> applyHire(action, actionBy);
            case "SUSPEND" -> applySuspend(action, actionBy);
            case "TERMINATE" -> applyTerminate(action, actionBy);
            case "REHIRE" -> applyRehire(action, actionBy);
            case "REINSTATE" -> applyReinstate(action, actionBy);
            default -> throw new IllegalStateException("Unsupported employment action: " + action.getActionType());
        };
        action.setStatus(Status.APPROVED);
        action.setApprovalRequestId(approvalRequestId);
        action.setResultingEmploymentId(result.getId());
        action.setActionedAt(LocalDateTime.now()); action.setActionedBy(actionBy); action.setStatusReason("Approved");
        actionRepository.save(action);
    }

    @Transactional
    public void rejectAction(String actionId, String approvalRequestId, String actionBy, String reason) {
        finaliseWithoutApplication(actionId, approvalRequestId, actionBy, Status.REJECTED, reason);
    }

    @Transactional
    public void cancelAction(String actionId, String approvalRequestId, String actionBy, String reason) {
        finaliseWithoutApplication(actionId, approvalRequestId, actionBy, Status.CANCELLED, reason);
    }

    private void finaliseWithoutApplication(String actionId, String approvalRequestId, String actionBy, String status, String reason) {
        EmploymentActionRequestEntity action = requireActionForUpdate(actionId);
        if (Status.APPROVED.equalsIgnoreCase(action.getStatus())) throw new IllegalStateException("Approved employment action cannot be changed");
        action.setStatus(status); action.setApprovalRequestId(approvalRequestId); action.setActionedAt(LocalDateTime.now());
        action.setActionedBy(actionBy); action.setStatusReason(hasText(reason) ? reason : status);
        actionRepository.save(action);
    }

    private EmploymentEntity applyHire(EmploymentActionRequestEntity action, String actionBy) {
        if (employmentRepository.existsByPartnerIdAndStatusIn(action.getPartnerId(), List.of(Status.ACTIVE, Status.SUSPENDED))) {
            throw new IllegalStateException("The person already has an active or suspended employment record");
        }
        String employeeNumber = employeeNumberRepository.findByPartnerId(action.getPartnerId())
                .map(EmployeeNumberAssignmentEntity::getEmployeeNumber)
                .orElseGet(() -> allocateEmployeeNumber(action.getPartnerId(), actionBy));
        EmploymentEntity employment = EmploymentEntity.builder()
                .partnerId(action.getPartnerId()).employeeNumber(employeeNumber).type(action.getProposedType())
                .startDate(Date.valueOf(action.getProposedStartDate())).endDate(Date.valueOf(action.getProposedEndDate()))
                .position(action.getProposedPosition()).branch(action.getProposedBranch()).department(action.getProposedDepartment())
                .status(Status.ACTIVE).createdBy(actionBy).updatedBy(actionBy).build();
        employment = employmentRepository.save(employment);
        ensureEmployeeRole(employment.getPartnerId());
        recordHistory(employment, action, "HIRE", null, Status.ACTIVE, action.getEffectiveDate(), action.getReason(), null, snapshot(employment), actionBy);
        leaveConfigurationService.assignResolvedProfileOnHire(employment, action.getEffectiveDate());
        leaveBalanceService.initialiseForEmployment(employment, action.getEffectiveDate());
        restoreSystemAccessIfAppropriate(employment.getPartnerId(), actionBy);
        return employment;
    }

    private EmploymentEntity applySuspend(EmploymentActionRequestEntity action, String actionBy) {
        EmploymentEntity employment = requireEmployment(action.getEmploymentId());
        if (!Status.ACTIVE.equalsIgnoreCase(employment.getStatus())) {
            throw new IllegalStateException("Employment is no longer active and cannot be suspended");
        }
        String oldStatus = employment.getStatus(); Map<String, Object> old = snapshot(employment);
        employment.setStatus(Status.SUSPENDED); employment.setUpdatedBy(actionBy);
        employment = employmentRepository.save(employment);
        if (Boolean.TRUE.equals(action.getSuspendSystemAccess())) suspendSystemAccess(employment.getPartnerId(), "Employment suspended", actionBy);
        recordHistory(employment, action, "SUSPENSION", oldStatus, Status.SUSPENDED, action.getEffectiveDate(), action.getReason(), old, snapshot(employment), actionBy);
        return employment;
    }

    private EmploymentEntity applyReinstate(EmploymentActionRequestEntity action, String actionBy) {
        EmploymentEntity employment = requireEmployment(action.getEmploymentId());
        if (!Status.SUSPENDED.equalsIgnoreCase(employment.getStatus())) {
            throw new IllegalStateException("Employment is no longer suspended and cannot be reinstated");
        }
        String oldStatus = employment.getStatus();
        Map<String, Object> old = snapshot(employment);
        employment.setStatus(Status.ACTIVE);
        employment.setUpdatedBy(actionBy);
        employment = employmentRepository.save(employment);
        restoreSystemAccessIfAppropriate(employment.getPartnerId(), actionBy);
        recordHistory(employment, action, "REINSTATEMENT", oldStatus, Status.ACTIVE,
                action.getEffectiveDate(), action.getReason(), old, snapshot(employment), actionBy);
        return employment;
    }

    private EmploymentEntity applyTerminate(EmploymentActionRequestEntity action, String actionBy) {
        EmploymentEntity employment = requireEmployment(action.getEmploymentId());
        if (!Status.ACTIVE.equalsIgnoreCase(employment.getStatus())
                && !Status.SUSPENDED.equalsIgnoreCase(employment.getStatus())) {
            throw new IllegalStateException("Employment can no longer be terminated from its current status");
        }
        String oldStatus = employment.getStatus(); Map<String, Object> old = snapshot(employment);
        employment.setStatus(Status.TERMINATED); employment.setEndDate(Date.valueOf(action.getEffectiveDate())); employment.setUpdatedBy(actionBy);
        employment = employmentRepository.save(employment);
        if (!employmentRepository.existsByPartnerIdAndStatusIn(employment.getPartnerId(), List.of(Status.ACTIVE, Status.SUSPENDED))) removeEmployeeRole(employment.getPartnerId());
        if (Boolean.TRUE.equals(action.getSuspendSystemAccess())) suspendSystemAccess(employment.getPartnerId(), "Employment terminated", actionBy);
        recordHistory(employment, action, "TERMINATION", oldStatus, Status.TERMINATED, action.getEffectiveDate(), action.getReason(), old, snapshot(employment), actionBy);
        return employment;
    }

    private EmploymentEntity applyRehire(EmploymentActionRequestEntity action, String actionBy) {
        EmploymentEntity previous = requireEmployment(action.getEmploymentId());
        if (!Status.TERMINATED.equalsIgnoreCase(previous.getStatus())) {
            throw new IllegalStateException("Employment is no longer terminated and cannot be rehired");
        }
        if (employmentRepository.existsByPartnerIdAndStatusIn(previous.getPartnerId(), List.of(Status.ACTIVE, Status.SUSPENDED))) {
            throw new IllegalStateException("The person already has an active or suspended employment record");
        }
        String employeeNumber = employeeNumberRepository.findByPartnerId(previous.getPartnerId())
                .map(EmployeeNumberAssignmentEntity::getEmployeeNumber)
                .orElseGet(() -> allocateEmployeeNumber(previous.getPartnerId(), actionBy));
        EmploymentEntity employment = EmploymentEntity.builder()
                .previousEmploymentId(previous.getId()).partnerId(previous.getPartnerId()).employeeNumber(employeeNumber)
                .type(action.getProposedType()).startDate(Date.valueOf(action.getProposedStartDate()))
                .endDate(Date.valueOf(action.getProposedEndDate())).position(action.getProposedPosition())
                .branch(action.getProposedBranch()).department(action.getProposedDepartment())
                .status(Status.ACTIVE).createdBy(actionBy).updatedBy(actionBy).build();
        employment = employmentRepository.save(employment);
        ensureEmployeeRole(employment.getPartnerId());
        restoreSystemAccessIfAppropriate(employment.getPartnerId(), actionBy);
        recordHistory(employment, action, "REHIRE", Status.TERMINATED, Status.ACTIVE, action.getEffectiveDate(), action.getReason(), snapshot(previous), snapshot(employment), actionBy);
        leaveConfigurationService.assignResolvedProfileOnHire(employment, action.getEffectiveDate());
        leaveBalanceService.initialiseForEmployment(employment, action.getEffectiveDate());
        return employment;
    }

    private String allocateEmployeeNumber(String partnerId, String actionBy) {
        for (int attempt = 0; attempt < 100; attempt++) {
            String number = numberAllocationService.allocateNumber("EMPLOYEE");
            if (employeeNumberRepository.existsById(number)) continue;
            employeeNumberRepository.save(EmployeeNumberAssignmentEntity.builder()
                    .employeeNumber(number).partnerId(partnerId).allocatedBy(actionBy).build());
            return number;
        }
        throw new IllegalStateException(
                "Unable to allocate a unique employee number. Review the EMPLOYEE number allocation configuration");
    }

    private void submitForApproval(EmploymentActionRequestEntity action, ApprovalType type) {
        PartnerDto partner = resolvePartner(action.getPartnerId());
        String name = partnerName(partner);
        ApprovalSubmitRequest request = new ApprovalSubmitRequest();
        request.setApprovalType(type); request.setReferenceId(action.getId()); request.setReferenceNo(action.getRequestNumber());
        request.setTitle(humanize(action.getActionType()) + " – " + name);
        request.setDescription(approvalDescription(action, name, partner)); request.setRequesterId(action.getRequestedBy());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestNumber", action.getRequestNumber());
        payload.put("actionType", action.getActionType());
        payload.put("employeeName", name);
        payload.put("partnerId", action.getPartnerId());
        payload.put("identityNumber", identityNumber(partner));
        payload.put("effectiveDate", action.getEffectiveDate());
        payload.put("expectedReturnDate", action.getExpectedReturnDate());
        payload.put("employmentType", action.getProposedType());
        payload.put("position", action.getProposedPosition());
        payload.put("branch", action.getProposedBranch());
        payload.put("department", action.getProposedDepartment());
        payload.put("reason", action.getReason());
        payload.put("affectsPayroll", action.getAffectsPayroll());
        payload.put("suspendSystemAccess", action.getSuspendSystemAccess());
        payload.put("attachmentObjectIds", attachments(action.getAttachmentObjectIds()));
        request.setPayloadJson(json(payload));
        ApprovalRequestResponse response = approvalService.submitForApproval(request);
        action.setApprovalRequestId(response.getId());
        actionRepository.save(action);
    }

    private String approvalDescription(EmploymentActionRequestEntity action, String name, PartnerDto partner) {
        return humanize(action.getActionType()) + " request for " + name + " (" + identityNumber(partner) + "). " +
                "Effective date: " + action.getEffectiveDate() + ". Position: " + safe(action.getProposedPosition()) +
                ". Employment type: " + safe(action.getProposedType()) + ". Reason: " + action.getReason();
    }

    private ApprovalType approvalType(String action) {
        return switch (action) {
            case "SUSPEND" -> ApprovalType.EMPLOYEE_SUSPENSION;
            case "TERMINATE" -> ApprovalType.EMPLOYEE_TERMINATION;
            case "REHIRE" -> ApprovalType.EMPLOYEE_REHIRE;
            case "REINSTATE" -> ApprovalType.EMPLOYEE_REINSTATEMENT;
            default -> throw new IllegalArgumentException("Unsupported employment action: " + action);
        };
    }

    @Transactional
    public EmploymentDto updateEmploymentDetails(String employmentId, za.co.mawa.bes.dto.EmploymentEditDto request) {
        if (request == null) throw new IllegalArgumentException("Employment update is required");
        EmploymentEntity employment = requireEmployment(employmentId);
        String previousPosition = employment.getPosition();
        if (hasText(request.getEmployeeNumber()) && !request.getEmployeeNumber().trim().equals(employment.getEmployeeNumber())) {
            throw new IllegalArgumentException("Employee number cannot be changed after allocation");
        }
        Map<String, Object> oldValues = snapshot(employment);
        if (hasText(request.getType())) employment.setType(validateOption(Field.EMPLOYMENT_TYPE, request.getType(), "Employment type", true));
        if (hasText(request.getPosition())) employment.setPosition(validateOption(Field.EMPLOYMENT_POSITION, request.getPosition(), "Position", true));
        if (request.getBranch() != null) employment.setBranch(validateOption(Field.BRANCH, request.getBranch(), "Branch", false));
        if (request.getDepartment() != null) employment.setDepartment(validateOption(Field.DEPARTMENT, request.getDepartment(), "Department", false));
        if (hasText(request.getStartDate())) {
            LocalDate proposedStart = Conversion.stringToDate(request.getStartDate()) == null ? null
                    : new Date(Conversion.stringToDate(request.getStartDate()).getTime()).toLocalDate();
            LocalDate currentStart = toLocalDate(employment.getStartDate());
            if (!Objects.equals(proposedStart, currentStart)) {
                throw new IllegalArgumentException("Employment start date cannot be changed after hire approval");
            }
        }
        if (hasText(request.getEndDate())) {
            LocalDate proposedEnd = Conversion.stringToDate(request.getEndDate()) == null ? null
                    : new Date(Conversion.stringToDate(request.getEndDate()).getTime()).toLocalDate();
            LocalDate currentEnd = toLocalDate(employment.getEndDate());
            if (!Objects.equals(proposedEnd, currentEnd)) {
                throw new IllegalArgumentException("Use the termination approval process to change the employment end date");
            }
        }
        employment.setUpdatedBy(actor());
        employment = employmentRepository.save(employment);
        if (!Objects.equals(previousPosition, employment.getPosition())) {
            LocalDate effectiveDate = LocalDate.now();
            leaveConfigurationService.realignDerivedProfileAssignment(employment, effectiveDate);
            leaveBalanceService.initialiseForEmployment(employment, effectiveDate);
        }
        Map<String, Object> newValues = snapshot(employment);
        if (!Objects.equals(oldValues, newValues)) {
            historyRepository.save(EmploymentStatusHistoryEntity.builder()
                    .employmentId(employment.getId()).eventType("DETAILS_CHANGE")
                    .oldStatus(employment.getStatus()).newStatus(employment.getStatus())
                    .effectiveDate(LocalDate.now()).reason("Employment details updated")
                    .previousValues(json(oldValues)).newValues(json(newValues)).changedBy(actor()).build());
        }
        return toDto(employment);
    }

    private void recordHistory(EmploymentEntity employment, EmploymentActionRequestEntity action, String eventType,
                               String oldStatus, String newStatus, LocalDate effectiveDate, String reason,
                               Map<String, Object> oldValues, Map<String, Object> newValues, String actionBy) {
        historyRepository.save(EmploymentStatusHistoryEntity.builder()
                .employmentId(employment.getId()).actionRequestId(action.getId()).eventType(eventType)
                .oldStatus(oldStatus).newStatus(newStatus).effectiveDate(effectiveDate).reason(reason)
                .previousValues(json(oldValues)).newValues(json(newValues)).approvalRequestId(action.getApprovalRequestId())
                .changedBy(actionBy).build());
    }

    private EmploymentActionResponseDto toResponse(EmploymentActionRequestEntity action) {
        EmploymentDto current = null;
        if (hasText(action.getEmploymentId())) current = employmentRepository.findById(action.getEmploymentId()).map(this::toDto).orElse(null);
        return EmploymentActionResponseDto.builder().id(action.getId()).requestNumber(action.getRequestNumber())
                .actionType(action.getActionType()).employmentId(action.getEmploymentId()).partnerId(action.getPartnerId())
                .employee(resolvePartner(action.getPartnerId())).currentEmployment(current).proposedType(action.getProposedType())
                .proposedStartDate(action.getProposedStartDate()).proposedEndDate(action.getProposedEndDate())
                .proposedPosition(action.getProposedPosition()).proposedBranch(action.getProposedBranch())
                .proposedDepartment(action.getProposedDepartment()).effectiveDate(action.getEffectiveDate())
                .expectedReturnDate(action.getExpectedReturnDate()).reason(action.getReason())
                .affectsPayroll(action.getAffectsPayroll()).suspendSystemAccess(action.getSuspendSystemAccess())
                .attachmentObjectIds(attachments(action.getAttachmentObjectIds())).status(action.getStatus())
                .approvalRequestId(action.getApprovalRequestId()).resultingEmploymentId(action.getResultingEmploymentId())
                .requestedAt(action.getRequestedAt()).requestedBy(action.getRequestedBy()).actionedAt(action.getActionedAt())
                .actionedBy(action.getActionedBy()).statusReason(action.getStatusReason()).build();
    }

    private EmploymentHistoryDto toHistory(EmploymentStatusHistoryEntity h) {
        return EmploymentHistoryDto.builder().id(h.getId()).employmentId(h.getEmploymentId())
                .actionRequestId(h.getActionRequestId()).eventType(h.getEventType()).oldStatus(h.getOldStatus())
                .newStatus(h.getNewStatus()).effectiveDate(h.getEffectiveDate()).reason(h.getReason())
                .previousValues(map(h.getPreviousValues())).newValues(map(h.getNewValues()))
                .approvalRequestId(h.getApprovalRequestId()).changedAt(h.getChangedAt()).changedBy(h.getChangedBy()).build();
    }

    private EmploymentDto toDto(EmploymentEntity e) {
        EmploymentDto d = new EmploymentDto(); d.setId(e.getId()); d.setEmployeeNumber(e.getEmployeeNumber()); d.setEmployee(resolvePartner(e.getPartnerId()));
        d.setType(resolveOption(Field.EMPLOYMENT_TYPE, e.getType())); d.setStartDate(Conversion.dateToString(e.getStartDate()));
        d.setEndDate(Conversion.dateToString(e.getEndDate())); d.setPosition(e.getPosition());
        d.setPositionDescription(fieldOptionService.getFieldOptionDescription(Field.EMPLOYMENT_POSITION, e.getPosition()));
        d.setStatus(e.getStatus()); d.setBranch(resolveOption(Field.BRANCH, e.getBranch())); d.setDepartment(resolveOption(Field.DEPARTMENT, e.getDepartment())); return d;
    }

    private FieldOptionDto resolveOption(String field, String code) {
        if (!hasText(code)) return null; FieldOptionDto option = fieldOptionService.getFieldOption(field, code); if (option != null) return option;
        FieldOptionDto fallback = new FieldOptionDto(); fallback.setField(field); fallback.setCode(code); fallback.setDescription(code.replace('-', ' ')); return fallback;
    }

    private Map<String, Object> snapshot(EmploymentEntity e) {
        if (e == null) return null;
        Map<String, Object> values = new LinkedHashMap<>(); values.put("id", e.getId()); values.put("partnerId", e.getPartnerId());
        values.put("employeeNumber", e.getEmployeeNumber()); values.put("type", e.getType()); values.put("position", e.getPosition());
        values.put("branch", e.getBranch()); values.put("department", e.getDepartment()); values.put("status", e.getStatus());
        values.put("startDate", toLocalDate(e.getStartDate())); values.put("endDate", toLocalDate(e.getEndDate())); return values;
    }

    private void validateCommon(EmploymentActionRequestDto request, String action) {
        if (request == null) throw new IllegalArgumentException("Employment action request is required");
        if (!hasText(request.getReason())) throw new IllegalArgumentException("Reason is required");
        if ("HIRE".equals(action) && !hasText(request.getPartnerId())) throw new IllegalArgumentException("Employee is required");
        if (request.getEffectiveDate() == null && !"HIRE".equals(action)) request.setEffectiveDate(LocalDate.now());
    }
    private EmploymentEntity requireEmployment(String id) { return employmentRepository.findById(required(id, "Employment id", 255)).orElseThrow(() -> new NoSuchElementException("Employment record not found: " + id)); }
    private EmploymentActionRequestEntity requireAction(String id) { return actionRepository.findById(required(id, "Employment action id", 255)).orElseThrow(() -> new NoSuchElementException("Employment action not found: " + id)); }

    private EmploymentActionRequestEntity requireActionForUpdate(String id) {
        String actionId = required(id, "Employment action id", 255);
        return actionRepository.findByIdForUpdate(actionId)
                .orElseThrow(() -> new NoSuchElementException("Employment action not found: " + id));
    }
    private void validatePartner(String id) {
        try {
            if (partnerService.getOptional(id) == null) {
                throw new IllegalArgumentException("Employee partner not found: " + id);
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Employee partner not found: " + id);
        }
    }
    private String validateOption(String field, String value, String label, boolean required) {
        if (!hasText(value)) { if (required) throw new IllegalArgumentException(label + " is required"); return null; }
        String code = value.trim().toUpperCase(Locale.ROOT); if (fieldOptionService.getFieldOption(field, code) == null) throw new IllegalArgumentException(label + " must be selected from configured options"); return code;
    }
    private void ensureEmployeeRole(String partnerId) { if (partnerService.getRoles(partnerId).stream().noneMatch(r -> RoleType.EMPLOYEE.equalsIgnoreCase(r))) partnerService.addRole(partnerId, RoleType.EMPLOYEE); }
    private void removeEmployeeRole(String partnerId) { if (partnerService.getRoles(partnerId).stream().anyMatch(r -> RoleType.EMPLOYEE.equalsIgnoreCase(r))) partnerService.removeRole(partnerId, RoleType.EMPLOYEE); }
    private void suspendSystemAccess(String partnerId, String reason, String actionBy) {
        UserEntity user = userRepository.getByPartner(partnerId); if (user == null || Boolean.TRUE.equals(user.getProtectedUser())) return;
        user.setStatus(Status.SUSPENDED); user.setStatusReason(reason); user.setDisabledAt(new java.util.Date()); user.setDisabledBy(actionBy); userRepository.save(user);
    }
    private void restoreSystemAccessIfAppropriate(String partnerId, String actionBy) {
        UserEntity user = userRepository.getByPartner(partnerId); if (user == null || Boolean.TRUE.equals(user.getProtectedUser())) return;
        if (Status.SUSPENDED.equalsIgnoreCase(user.getStatus()) && hasText(user.getStatusReason()) && user.getStatusReason().startsWith("Employment")) {
            user.setStatus(Status.ACTIVE); user.setStatusReason("Employment active"); user.setDisabledAt(null); user.setDisabledBy(null); userRepository.save(user);
        }
    }
    private boolean hasUploadedAttachments(List<String> objectIds) {
        if (objectIds == null) return false;
        List<String> values = objectIds.stream().filter(this::hasText).map(String::trim).distinct().toList();
        return !values.isEmpty() && !attachmentRepository.findByObjectIdIn(values).isEmpty();
    }
    private PartnerDto resolvePartner(String id) { try { return partnerService.getOptional(id); } catch (Exception e) { return null; } }
    private String partnerName(PartnerDto p) { if (p == null) return "Unknown employee"; String name = Stream.of(p.getName2(), p.getName3(), p.getName1()).filter(this::hasText).collect(Collectors.joining(" ")); return hasText(name) ? name : p.getId(); }
    private String identityNumber(PartnerDto p) { return p != null && p.getIdentity() != null && hasText(p.getIdentity().getNumber()) ? p.getIdentity().getNumber() : "No identity recorded"; }
    private String normalizeAction(String value) { String normalized = required(value, "Action type", 30).toUpperCase(Locale.ROOT); if (!Set.of("SUSPEND", "TERMINATE", "REHIRE", "REINSTATE", "HIRE").contains(normalized)) throw new IllegalArgumentException("Unsupported employment action: " + value); return normalized; }
    private String required(String value, String label, int max) { if (!hasText(value)) throw new IllegalArgumentException(label + " is required"); String v=value.trim(); if(v.length()>max) throw new IllegalArgumentException(label+" cannot exceed "+max+" characters"); return v; }
    private String actor() { if (hasText(UserContext.getCurrentUserId())) return UserContext.getCurrentUserId(); if (hasText(UserContext.getCurrentUser())) return UserContext.getCurrentUser(); return "SYSTEM"; }
    private boolean defaultBoolean(Boolean value, boolean fallback) { return value == null ? fallback : value; }
    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private LocalDate toLocalDate(java.util.Date value) { return value == null ? null : new Date(value.getTime()).toLocalDate(); }
    private String safe(String value) { return hasText(value) ? value : "Not specified"; }
    private String humanize(String value) { String v=value.toLowerCase(Locale.ROOT).replace('_',' '); return Character.toUpperCase(v.charAt(0))+v.substring(1); }
    private String json(Object value) { if (value == null) return null; try { return objectMapper.writeValueAsString(value); } catch (Exception e) { throw new IllegalStateException("Unable to serialize employment action", e); } }
    private List<String> attachments(String json) { if (!hasText(json)) return List.of(); try { return objectMapper.readValue(json, new TypeReference<List<String>>(){}); } catch (Exception e) { return List.of(); } }
    private Map<String,Object> map(String json) { if (!hasText(json)) return Map.of(); try { return objectMapper.readValue(json, new TypeReference<Map<String,Object>>(){}); } catch (Exception e) { return Map.of(); } }
}
