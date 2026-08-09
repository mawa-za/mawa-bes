package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.v2.*;
import za.co.mawa.bes.entity.EmploymentEntity;
import za.co.mawa.bes.entity.v2.LeaveBalanceAdjustmentRequestEntity;
import za.co.mawa.bes.entity.v2.LeaveTypeEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.repository.AttachmentRepository;
import za.co.mawa.bes.repository.EmploymentRepository;
import za.co.mawa.bes.repository.v2.LeaveBalanceAdjustmentRequestRepository;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class LeaveBalanceAdjustmentService {
    private final LeaveBalanceAdjustmentRequestRepository requestRepository;
    private final AttachmentRepository attachmentRepository;
    private final EmploymentRepository employmentRepository;
    private final LeaveConfigurationService configurationService;
    private final LeaveBalanceService balanceService;
    private final NumberAllocationService numberAllocationService;
    private final ApprovalService approvalService;
    private final PartnerService partnerService;
    private final ObjectMapper objectMapper;

    public LeaveBalanceAdjustmentService(
            LeaveBalanceAdjustmentRequestRepository requestRepository,
            AttachmentRepository attachmentRepository,
            EmploymentRepository employmentRepository,
            LeaveConfigurationService configurationService,
            LeaveBalanceService balanceService,
            NumberAllocationService numberAllocationService,
            ApprovalService approvalService,
            PartnerService partnerService,
            ObjectMapper objectMapper) {
        this.requestRepository = requestRepository;
        this.attachmentRepository = attachmentRepository;
        this.employmentRepository = employmentRepository;
        this.configurationService = configurationService;
        this.balanceService = balanceService;
        this.numberAllocationService = numberAllocationService;
        this.approvalService = approvalService;
        this.partnerService = partnerService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public LeaveBalanceAdjustmentResponseDto requestAdjustment(LeaveBalanceAdjustmentRequestDto request) {
        if (request == null) throw new IllegalArgumentException("Leave balance adjustment request is required");
        EmploymentEntity employment = employmentRepository.findById(required(request.getEmploymentId(), "Employment id", 255))
                .orElseThrow(() -> new NoSuchElementException("Employment record not found: " + request.getEmploymentId()));
        LeaveTypeEntity leaveType = configurationService.requireLeaveType(required(request.getLeaveTypeId(), "Leave type", 255));
        BigDecimal amount = request.getAdjustmentAmount();
        if (amount == null || amount.signum() == 0) throw new IllegalArgumentException("Adjustment amount must not be zero");
        String reason = required(request.getReason(), "Reason", 1000);
        List<String> attachmentObjectIds = request.getAttachmentObjectIds() == null ? List.of()
                : request.getAttachmentObjectIds().stream().filter(this::hasText).map(String::trim).distinct().toList();
        if (attachmentObjectIds.isEmpty() || attachmentRepository.findByObjectIdIn(attachmentObjectIds).isEmpty()) {
            throw new IllegalArgumentException("Supporting documentation is required for a leave balance adjustment");
        }
        LocalDate effectiveDate = request.getEffectiveDate() == null ? LocalDate.now() : request.getEffectiveDate();
        String actor = actor();
        LeaveBalanceAdjustmentRequestEntity entity = LeaveBalanceAdjustmentRequestEntity.builder()
                .requestNumber(numberAllocationService.allocateNumber("LEAVE_BALANCE_ADJUSTMENT"))
                .employmentId(employment.getId()).leaveTypeId(leaveType.getId())
                .adjustmentAmount(amount).effectiveDate(effectiveDate).reason(reason)
                .attachmentObjectIds(json(attachmentObjectIds))
                .status(Status.AWAITING_APPROVAL).requestedBy(actor).version(0L).build();
        entity = requestRepository.save(entity);

        PartnerDto employee = resolvePartner(employment.getPartnerId());
        String employeeName = partnerName(employee);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestNumber", entity.getRequestNumber());
        payload.put("employeeName", employeeName);
        payload.put("employeeNumber", employment.getEmployeeNumber());
        payload.put("employmentId", employment.getId());
        payload.put("leaveType", leaveType.getName());
        payload.put("adjustmentAmount", amount);
        payload.put("effectiveDate", effectiveDate);
        payload.put("reason", reason);
        payload.put("attachmentObjectIds", attachmentObjectIds);

        ApprovalSubmitRequest approval = new ApprovalSubmitRequest();
        approval.setApprovalType(ApprovalType.LEAVE_BALANCE_ADJUSTMENT);
        approval.setReferenceId(entity.getId());
        approval.setReferenceNo(entity.getRequestNumber());
        approval.setTitle("Leave balance adjustment – " + employeeName + " – " + leaveType.getName());
        approval.setDescription("Adjust " + employeeName + " (" + employment.getEmployeeNumber() + ") "
                + leaveType.getName() + " balance by " + amount + " effective " + effectiveDate + ". Reason: " + reason);
        approval.setRequesterId(actor);
        approval.setPayloadJson(json(payload));
        ApprovalRequestResponse response = approvalService.submitForApproval(approval);
        entity.setApprovalRequestId(response.getId());
        return toResponse(requestRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceAdjustmentResponseDto> list() {
        return requestRepository.findAllByOrderByRequestedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public LeaveBalanceAdjustmentResponseDto get(String id) {
        return toResponse(require(id));
    }

    @Transactional
    public void approve(String id, String approvalRequestId, String actionBy) {
        LeaveBalanceAdjustmentRequestEntity request = requireForUpdate(id);
        if (Status.APPROVED.equalsIgnoreCase(request.getStatus())) return;
        if (!Status.AWAITING_APPROVAL.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Only adjustments awaiting approval can be approved");
        }
        EmploymentEntity employment = employmentRepository.findById(request.getEmploymentId()).orElseThrow();
        LeaveTypeEntity type = configurationService.requireLeaveType(request.getLeaveTypeId());
        balanceService.applyAdjustment(request.getId(), employment, type, request.getAdjustmentAmount(),
                request.getEffectiveDate(), request.getReason(), actionBy);
        request.setStatus(Status.APPROVED);
        request.setApprovalRequestId(approvalRequestId);
        request.setActionedAt(LocalDateTime.now());
        request.setActionedBy(actionBy);
        request.setStatusReason("Approved");
        requestRepository.save(request);
    }

    @Transactional
    public void reject(String id, String approvalRequestId, String actionBy, String reason) {
        finalise(id, approvalRequestId, actionBy, Status.REJECTED, reason);
    }

    @Transactional
    public void cancel(String id, String approvalRequestId, String actionBy, String reason) {
        finalise(id, approvalRequestId, actionBy, Status.CANCELLED, reason);
    }

    private void finalise(String id, String approvalRequestId, String actionBy, String status, String reason) {
        LeaveBalanceAdjustmentRequestEntity request = requireForUpdate(id);
        if (Status.APPROVED.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("An approved leave balance adjustment cannot be changed");
        }
        request.setStatus(status);
        request.setApprovalRequestId(approvalRequestId);
        request.setActionedAt(LocalDateTime.now());
        request.setActionedBy(actionBy);
        request.setStatusReason(hasText(reason) ? reason : status);
        requestRepository.save(request);
    }

    private LeaveBalanceAdjustmentRequestEntity require(String id) {
        return requestRepository.findById(required(id, "Adjustment request id", 255))
                .orElseThrow(() -> new NoSuchElementException("Leave balance adjustment request not found: " + id));
    }

    private LeaveBalanceAdjustmentRequestEntity requireForUpdate(String id) {
        String requestId = required(id, "Adjustment request id", 255);
        return requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new NoSuchElementException("Leave balance adjustment request not found: " + id));
    }

    private LeaveBalanceAdjustmentResponseDto toResponse(LeaveBalanceAdjustmentRequestEntity entity) {
        EmploymentEntity employment = employmentRepository.findById(entity.getEmploymentId()).orElse(null);
        LeaveTypeEntity type = configurationService.requireLeaveType(entity.getLeaveTypeId());
        return LeaveBalanceAdjustmentResponseDto.builder()
                .id(entity.getId()).requestNumber(entity.getRequestNumber()).employmentId(entity.getEmploymentId())
                .employeeNumber(employment == null ? null : employment.getEmployeeNumber())
                .employeeName(employment == null ? null : partnerName(resolvePartner(employment.getPartnerId())))
                .leaveTypeId(type.getId()).leaveTypeCode(type.getCode()).leaveTypeName(type.getName())
                .adjustmentAmount(entity.getAdjustmentAmount()).effectiveDate(entity.getEffectiveDate())
                .reason(entity.getReason()).attachmentObjectIds(attachments(entity.getAttachmentObjectIds()))
                .status(entity.getStatus()).approvalRequestId(entity.getApprovalRequestId())
                .requestedAt(entity.getRequestedAt()).requestedBy(entity.getRequestedBy())
                .actionedAt(entity.getActionedAt()).actionedBy(entity.getActionedBy()).statusReason(entity.getStatusReason())
                .build();
    }

    private PartnerDto resolvePartner(String partnerId) {
        try { return partnerService.getOptional(partnerId); } catch (Exception ignored) { return null; }
    }

    private String partnerName(PartnerDto partner) {
        if (partner == null) return "Unknown employee";
        String name = Stream.of(partner.getName2(), partner.getName3(), partner.getName1())
                .filter(this::hasText).collect(Collectors.joining(" "));
        return hasText(name) ? name : partner.getId();
    }

    private String actor() {
        if (hasText(UserContext.getCurrentUserId())) return UserContext.getCurrentUserId();
        if (hasText(UserContext.getCurrentUser())) return UserContext.getCurrentUser();
        return "SYSTEM";
    }

    private String required(String value, String label, int max) {
        if (!hasText(value)) throw new IllegalArgumentException(label + " is required");
        String result = value.trim();
        if (result.length() > max) throw new IllegalArgumentException(label + " cannot exceed " + max + " characters");
        return result;
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalStateException("Unable to serialize leave balance adjustment", exception); }
    }

    private List<String> attachments(String json) {
        if (!hasText(json)) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<List<String>>() { }); }
        catch (Exception ignored) { return List.of(); }
    }

    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
}
