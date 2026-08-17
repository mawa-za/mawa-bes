package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.v2.*;
import za.co.mawa.bes.entity.EmploymentEntity;
import za.co.mawa.bes.entity.v2.*;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.repository.AttachmentRepository;
import za.co.mawa.bes.repository.EmploymentRepository;
import za.co.mawa.bes.repository.v2.LeaveRequestRepository;
import za.co.mawa.bes.repository.v2.LeaveRequestStatusHistoryRepository;
import za.co.mawa.bes.service.FieldOptionService;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.utils.Field;
import za.co.mawa.bes.utils.Status;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class LeaveRequestV2Service {
    private static final List<String> ACTIVE_EMPLOYMENT_STATUSES = List.of(Status.ACTIVE, Status.SUSPENDED);
    private static final List<String> OVERLAP_STATUSES = List.of(Status.PENDING, Status.AWAITING_APPROVAL, Status.APPROVED);

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveRequestStatusHistoryRepository historyRepository;
    private final AttachmentRepository attachmentRepository;
    private final EmploymentRepository employmentRepository;
    private final LeaveConfigurationService configurationService;
    private final LeaveBalanceService balanceService;
    private final NumberAllocationService numberAllocationService;
    private final ApprovalService approvalService;
    private final PartnerService partnerService;
    private final FieldOptionService fieldOptionService;
    private final LeaveAccessService leaveAccessService;
    private final ObjectMapper objectMapper;

    public LeaveRequestV2Service(
            LeaveRequestRepository leaveRequestRepository,
            LeaveRequestStatusHistoryRepository historyRepository,
            AttachmentRepository attachmentRepository,
            EmploymentRepository employmentRepository,
            LeaveConfigurationService configurationService,
            LeaveBalanceService balanceService,
            NumberAllocationService numberAllocationService,
            ApprovalService approvalService,
            PartnerService partnerService,
            FieldOptionService fieldOptionService,
            LeaveAccessService leaveAccessService,
            ObjectMapper objectMapper) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.historyRepository = historyRepository;
        this.attachmentRepository = attachmentRepository;
        this.employmentRepository = employmentRepository;
        this.configurationService = configurationService;
        this.balanceService = balanceService;
        this.numberAllocationService = numberAllocationService;
        this.approvalService = approvalService;
        this.partnerService = partnerService;
        this.fieldOptionService = fieldOptionService;
        this.leaveAccessService = leaveAccessService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public LeaveRequestV2ResponseDto create(LeaveRequestV2CreateRequestDto request) {
        request = scopeToCurrentEmployee(request);
        PreparedLeave prepared = prepare(request, null);
        String actor = currentActor();
        LocalDateTime now = LocalDateTime.now();
        LeaveRequestEntity entity = LeaveRequestEntity.builder()
                .requestNumber(numberAllocationService.allocateNumber("LEAVE_REQUEST"))
                .employeePartnerId(prepared.employment().getPartnerId())
                .employmentId(prepared.employment().getId())
                .leaveType(prepared.leaveType().getCode())
                .leaveTypeId(prepared.leaveType().getId())
                .leaveProfileId(prepared.profile().getProfile().getId())
                .workingCalendarId(prepared.calendar().getId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .days(prepared.requestedAmount())
                .unit(prepared.leaveType().getUnit())
                .projectedBalance(prepared.projectedBalance())
                .requestReason(trim(request.getReason(), 1000))
                .attachmentObjectIds(json(request.getAttachmentObjectIds()))
                .status(Status.PENDING)
                .createdAt(now).createdBy(actor).updatedAt(now).updatedBy(actor).version(0L)
                .build();
        entity = leaveRequestRepository.save(entity);
        recordHistory(entity.getId(), null, Status.PENDING, "Leave request created", actor);
        return toResponse(entity, true);
    }

    @Transactional
    public LeaveRequestPreviewDto preview(LeaveRequestV2CreateRequestDto request) {
        try {
            request = scopeToCurrentEmployee(request);
            PreparedLeave prepared = prepare(request, null);
            return LeaveRequestPreviewDto.builder()
                    .employmentId(prepared.employment().getId())
                    .employeeNumber(prepared.employment().getEmployeeNumber())
                    .leaveTypeId(prepared.leaveType().getId())
                    .leaveTypeCode(prepared.leaveType().getCode())
                    .leaveTypeName(prepared.leaveType().getName())
                    .unit(prepared.leaveType().getUnit())
                    .leaveProfileId(prepared.profile().getProfile().getId())
                    .leaveProfileName(prepared.profile().getProfile().getName())
                    .assignmentSource(prepared.profile().getSource())
                    .workingCalendarId(prepared.calendar().getId())
                    .workingCalendarName(prepared.calendar().getName())
                    .requestedAmount(prepared.requestedAmount())
                    .availableBalance(prepared.availableBalance())
                    .projectedBalance(prepared.projectedBalance())
                    .supportingDocumentRequired(prepared.supportingDocumentRequired())
                    .allowed(prepared.allowed())
                    .message(prepared.message())
                    .build();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return LeaveRequestPreviewDto.builder().allowed(false).message(exception.getMessage()).build();
        }
    }

    @Transactional
    public LeaveRequestV2ResponseDto get(String id) {
        LeaveRequestEntity entity = require(id);
        assertCanView(entity);
        return toResponse(entity, true);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> access() {
        Map<String, Object> result = new LinkedHashMap<>();
        EmploymentEntity employment = null;
        try {
            employment = leaveAccessService.currentEmployment(LocalDate.now());
        } catch (NoSuchElementException | SecurityException ignored) {
            // A non-employee user can still be a leave approver by role.
        }
        result.put("hasEmployment", employment != null);
        if (employment != null) {
            PartnerDto employee = resolvePartner(employment.getPartnerId());
            result.put("employmentId", employment.getId());
            result.put("employeePartnerId", employment.getPartnerId());
            result.put("employeeNumber", employment.getEmployeeNumber());
            result.put("employeeName", partnerName(employee));
            result.put("position", employment.getPosition());
        }
        result.put("leaveApprover", leaveAccessService.isLeaveApprover());
        return result;
    }

    @Transactional
    public List<LeaveRequestV2ResponseDto> search(
            String status,
            String view,
            String leaveType,
            LocalDate fromDate,
            LocalDate toDate) {
        final boolean approverView = "APPROVER".equalsIgnoreCase(view);
        final List<String> approvableEmploymentIds = approverView
                ? leaveAccessService.approvableEmploymentIds()
                : List.of();
        if (approverView && approvableEmploymentIds.isEmpty()) return List.of();
        final String ownPartnerId = approverView ? null : leaveAccessService.currentPartnerId();

        Specification<LeaveRequestEntity> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (approverView) {
                predicates.add(root.get("employmentId").in(approvableEmploymentIds));
            } else {
                predicates.add(cb.equal(root.get("employeePartnerId"), ownPartnerId));
            }
            if (hasText(status)) predicates.add(cb.equal(root.get("status"), normalize(status)));
            if (hasText(leaveType)) {
                String normalized = normalize(leaveType);
                predicates.add(cb.or(cb.equal(root.get("leaveType"), normalized), cb.equal(root.get("leaveTypeId"), leaveType.trim())));
            }
            if (fromDate != null) predicates.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("endDate"), fromDate));
            if (toDate != null) predicates.add(cb.lessThanOrEqualTo(root.<LocalDate>get("startDate"), toDate));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return leaveRequestRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(entity -> toResponse(entity, false)).toList();
    }

    @Transactional
    public LeaveRequestV2ResponseDto update(String id, LeaveRequestV2UpdateRequestDto request) {
        LeaveRequestEntity entity = require(id);
        assertOwnRequest(entity);
        requireStatus(entity, Status.PENDING, "Only pending leave requests can be edited");
        LeaveRequestV2CreateRequestDto merged = scopeToCurrentEmployee(merge(entity, request));
        PreparedLeave prepared = prepare(merged, id);
        entity.setEmployeePartnerId(prepared.employment().getPartnerId());
        entity.setEmploymentId(prepared.employment().getId());
        entity.setLeaveType(prepared.leaveType().getCode());
        entity.setLeaveTypeId(prepared.leaveType().getId());
        entity.setLeaveProfileId(prepared.profile().getProfile().getId());
        entity.setWorkingCalendarId(prepared.calendar().getId());
        entity.setStartDate(merged.getStartDate());
        entity.setEndDate(merged.getEndDate());
        entity.setDays(prepared.requestedAmount());
        entity.setUnit(prepared.leaveType().getUnit());
        entity.setProjectedBalance(prepared.projectedBalance());
        entity.setRequestReason(trim(merged.getReason(), 1000));
        entity.setAttachmentObjectIds(json(merged.getAttachmentObjectIds()));
        entity.setUpdatedBy(currentActor());
        return toResponse(leaveRequestRepository.save(entity), true);
    }

    @Transactional
    public LeaveRequestV2ResponseDto submit(String id) {
        LeaveRequestEntity entity = require(id);
        assertOwnRequest(entity);
        if (Status.AWAITING_APPROVAL.equalsIgnoreCase(entity.getStatus()) || Status.APPROVED.equalsIgnoreCase(entity.getStatus())) {
            return toResponse(entity, true);
        }
        requireStatus(entity, Status.PENDING, "Only pending leave requests can be submitted");
        PreparedLeave prepared = validateStored(entity);
        validateSupportingDocuments(entity, prepared);
        if (!prepared.allowed()) throw new IllegalStateException(prepared.message());

        if (!Boolean.TRUE.equals(prepared.leaveType().getRequiresApproval())) {
            submitInternal(entity, null, currentActor());
            return approveFromApproval(id, "Automatically approved because the leave type does not require approval", currentActor());
        }

        PartnerDto employee = resolvePartner(entity.getEmployeePartnerId());
        String employeeName = partnerName(employee);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestNumber", entity.getRequestNumber());
        payload.put("employeeName", employeeName);
        payload.put("employeeNumber", prepared.employment().getEmployeeNumber());
        payload.put("employmentId", prepared.employment().getId());
        payload.put("leaveType", prepared.leaveType().getName());
        payload.put("leaveTypeCode", prepared.leaveType().getCode());
        payload.put("startDate", entity.getStartDate());
        payload.put("endDate", entity.getEndDate());
        payload.put("requestedAmount", entity.getDays());
        payload.put("unit", entity.getUnit());
        payload.put("availableBalance", prepared.availableBalance());
        payload.put("projectedBalance", prepared.projectedBalance());
        payload.put("leaveProfile", prepared.profile().getProfile().getName());
        payload.put("workingCalendar", prepared.calendar().getName());
        payload.put("reason", entity.getRequestReason());
        payload.put("attachmentObjectIds", attachments(entity.getAttachmentObjectIds()));

        ApprovalSubmitRequest approval = new ApprovalSubmitRequest();
        approval.setApprovalType(ApprovalType.LEAVE);
        approval.setReferenceId(entity.getId());
        approval.setReferenceNo(entity.getRequestNumber());
        approval.setTitle("Leave request – " + employeeName + " – " + prepared.leaveType().getName());
        approval.setDescription(employeeName + " (" + prepared.employment().getEmployeeNumber() + ") requested "
                + entity.getDays().stripTrailingZeros().toPlainString() + " " + entity.getUnit().toLowerCase(Locale.ROOT)
                + " of " + prepared.leaveType().getName() + " from " + entity.getStartDate() + " to " + entity.getEndDate()
                + ". Projected balance: " + prepared.projectedBalance() + "."
                + (hasText(entity.getRequestReason()) ? " Reason: " + entity.getRequestReason() : ""));
        approval.setRequesterId(currentActor());
        approval.setPayloadJson(json(payload));
        ApprovalRequestResponse response = approvalService.submitForApproval(approval);
        entity.setApprovalRequestId(response.getId());
        leaveRequestRepository.save(entity);
        return toResponse(require(id), true);
    }

    @Transactional
    public LeaveRequestV2ResponseDto submitFromApproval(String id, String approvalRequestId, String actionBy) {
        LeaveRequestEntity entity = require(id);
        return submitInternal(entity, approvalRequestId, actionBy);
    }

    @Transactional
    public LeaveRequestV2ResponseDto submitFromApproval(String id, String actionBy) {
        return submitFromApproval(id, null, actionBy);
    }

    private LeaveRequestV2ResponseDto submitInternal(LeaveRequestEntity entity, String approvalRequestId, String actor) {
        if (Status.AWAITING_APPROVAL.equalsIgnoreCase(entity.getStatus())) return toResponse(entity, true);
        requireStatus(entity, Status.PENDING, "Only pending leave requests can be submitted");
        entity.setSubmittedAt(LocalDateTime.now());
        if (hasText(approvalRequestId)) entity.setApprovalRequestId(approvalRequestId);
        return transition(entity, Status.AWAITING_APPROVAL, null, actor);
    }

    @Transactional
    public LeaveRequestV2ResponseDto approveFromApproval(String id, String reason, String actionBy) {
        LeaveRequestEntity entity = require(id);
        if (Status.APPROVED.equalsIgnoreCase(entity.getStatus())) return toResponse(entity, true);
        requireStatus(entity, Status.AWAITING_APPROVAL, "Only leave requests awaiting approval can be approved");
        PreparedLeave prepared = validateStored(entity);
        validateSupportingDocuments(entity, prepared);
        EmployeeLeaveLedgerEntity ledger = balanceService.debitApprovedLeave(
                entity.getId(), prepared.employment(), prepared.leaveType(), entity.getDays(), entity.getStartDate());
        entity.setBalanceLedgerId(ledger.getId());
        entity.setProjectedBalance(ledger.getBalanceAfter());
        entity.setApprovedAt(LocalDateTime.now());
        return transition(entity, Status.APPROVED, reason, actionBy);
    }

    @Transactional
    public LeaveRequestV2ResponseDto rejectFromApproval(String id, String reason, String actionBy) {
        LeaveRequestEntity entity = require(id);
        if (Status.REJECTED.equalsIgnoreCase(entity.getStatus())) return toResponse(entity, true);
        requireStatus(entity, Status.AWAITING_APPROVAL, "Only leave requests awaiting approval can be rejected");
        entity.setRejectedAt(LocalDateTime.now());
        return transition(entity, Status.REJECTED, reason, actionBy);
    }

    @Transactional
    public LeaveRequestV2ResponseDto cancelFromApproval(String id, String reason, String actionBy) {
        LeaveRequestEntity entity = require(id);
        if (Status.CANCELLED.equalsIgnoreCase(entity.getStatus())) return toResponse(entity, true);
        if (Status.APPROVED.equalsIgnoreCase(entity.getStatus())) {
            balanceService.reverseApprovedLeave(entity.getId(), actionBy);
        } else if (!Status.PENDING.equalsIgnoreCase(entity.getStatus())
                && !Status.AWAITING_APPROVAL.equalsIgnoreCase(entity.getStatus())) {
            throw new IllegalStateException("Only pending, submitted or approved leave requests can be cancelled");
        }
        entity.setCancelledAt(LocalDateTime.now());
        return transition(entity, Status.CANCELLED, reason, actionBy);
    }

    @Transactional
    public LeaveRequestV2ResponseDto cancel(String id, String reason) {
        LeaveRequestEntity entity = require(id);
        assertOwnRequest(entity);
        return cancelFromApproval(id, reason, currentActor());
    }

    @Transactional
    public void delete(String id) {
        LeaveRequestEntity entity = require(id);
        assertOwnRequest(entity);
        requireStatus(entity, Status.PENDING, "Only pending leave requests can be deleted");
        historyRepository.deleteAll(historyRepository.findByLeaveRequestIdOrderByChangedAtDesc(id));
        leaveRequestRepository.delete(entity);
    }

    private PreparedLeave validateStored(LeaveRequestEntity entity) {
        LeaveRequestV2CreateRequestDto request = new LeaveRequestV2CreateRequestDto();
        request.setEmploymentId(entity.getEmploymentId());
        request.setEmployee(entity.getEmployeePartnerId());
        request.setLeaveTypeId(entity.getLeaveTypeId());
        request.setType(entity.getLeaveType());
        request.setStartDate(entity.getStartDate());
        request.setEndDate(entity.getEndDate());
        request.setRequestedAmount(entity.getDays());
        request.setUnit(entity.getUnit());
        request.setReason(entity.getRequestReason());
        request.setAttachmentObjectIds(attachments(entity.getAttachmentObjectIds()));
        return prepare(request, entity.getId());
    }

    private PreparedLeave prepare(LeaveRequestV2CreateRequestDto request, String excludeId) {
        if (request == null) throw new IllegalArgumentException("Leave request is required");
        if (request.getStartDate() == null) throw new IllegalArgumentException("Start date is required");
        if (request.getEndDate() == null) throw new IllegalArgumentException("End date is required");
        if (request.getEndDate().isBefore(request.getStartDate())) throw new IllegalArgumentException("End date cannot be before start date");

        EmploymentEntity employment = resolveEmployment(request);
        validateEmploymentDates(employment, request.getStartDate(), request.getEndDate());
        LeaveTypeEntity leaveType = resolveLeaveType(request);
        validateLeaveTypeDates(leaveType, request.getStartDate());
        LeaveConfigurationService.ResolvedProfile profile = configurationService.resolveProfile(employment, request.getStartDate());
        LeaveProfileRuleEntity rule = configurationService.requireRule(profile.getProfile().getId(), leaveType.getId(), request.getStartDate());
        WorkingCalendarEntity calendar = configurationService.requireCalendar(profile.getProfile().getWorkingCalendarId());

        BigDecimal amount = calculateRequestedAmount(request, leaveType, calendar);
        validateRequestRules(employment, leaveType, rule, amount, request.getStartDate());
        validateNoOverlap(employment.getId(), request.getStartDate(), request.getEndDate(), excludeId);

        BigDecimal available = balanceService.availableBalance(employment, leaveType, request.getStartDate());
        BigDecimal projected = available.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal allowedNegative = Boolean.TRUE.equals(leaveType.getAllowNegativeBalance())
                ? decimal(rule.getMaximumNegativeBalance()) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        boolean allowed = projected.compareTo(allowedNegative.negate()) >= 0;
        String message = allowed ? "Leave request meets the configured rules"
                : "Insufficient " + leaveType.getName() + " balance. Available: " + available;
        boolean documentRequired = documentRequired(leaveType, rule, amount);
        return new PreparedLeave(employment, leaveType, profile, rule, calendar, amount, available, projected,
                documentRequired, allowed, message);
    }

    private EmploymentEntity resolveEmployment(LeaveRequestV2CreateRequestDto request) {
        if (hasText(request.getEmploymentId())) {
            return employmentRepository.findById(request.getEmploymentId().trim())
                    .orElseThrow(() -> new NoSuchElementException("Employment record not found: " + request.getEmploymentId()));
        }
        if (!hasText(request.getEmployee())) throw new IllegalArgumentException("Employee is required");
        List<EmploymentEntity> matches = employmentRepository.findApplicableEmployment(
                request.getEmployee().trim(), Date.valueOf(request.getStartDate()), ACTIVE_EMPLOYMENT_STATUSES);
        if (matches.isEmpty()) throw new IllegalArgumentException("No active employment record applies to the requested leave date");
        return matches.get(0);
    }

    private LeaveTypeEntity resolveLeaveType(LeaveRequestV2CreateRequestDto request) {
        if (hasText(request.getLeaveTypeId())) return configurationService.requireLeaveType(request.getLeaveTypeId().trim());
        if (!hasText(request.getType())) throw new IllegalArgumentException("Leave type is required");
        try {
            return configurationService.requireLeaveType(request.getType().trim());
        } catch (NoSuchElementException ignored) {
            return configurationService.requireLeaveTypeByCode(request.getType());
        }
    }

    private BigDecimal calculateRequestedAmount(LeaveRequestV2CreateRequestDto request, LeaveTypeEntity leaveType,
                                                WorkingCalendarEntity calendar) {
        String unit = normalizeUnit(hasText(request.getUnit()) ? request.getUnit() : leaveType.getUnit());
        if (!unit.equalsIgnoreCase(leaveType.getUnit())) {
            throw new IllegalArgumentException("The selected leave type is configured in " + leaveType.getUnit().toLowerCase(Locale.ROOT));
        }
        BigDecimal supplied = request.getRequestedAmount() != null ? request.getRequestedAmount() : request.getDays();
        if ("HOURS".equals(unit)) {
            if (supplied == null || supplied.signum() <= 0) throw new IllegalArgumentException("Requested hours must be greater than zero");
            return supplied.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal calculated = workingDays(request.getStartDate(), request.getEndDate(), leaveType, calendar);
        if (supplied == null) return calculated;
        BigDecimal normalized = supplied.setScale(2, RoundingMode.HALF_UP);
        if (normalized.signum() <= 0) throw new IllegalArgumentException("Requested days must be greater than zero");
        if (normalized.compareTo(calculated) == 0) return calculated;
        boolean singleWorkingDay = calculated.compareTo(BigDecimal.ONE) == 0;
        if (singleWorkingDay && Boolean.TRUE.equals(leaveType.getAllowHalfDay())
                && normalized.compareTo(new BigDecimal("0.50")) == 0) return normalized;
        throw new IllegalArgumentException("Requested days must match the calculated working days (" + calculated + ")");
    }

    private BigDecimal workingDays(LocalDate start, LocalDate end, LeaveTypeEntity type, WorkingCalendarEntity calendar) {
        Set<LocalDate> holidays = configurationService.holidays(calendar.getId(), start, end).stream()
                .flatMap(holiday -> {
                    if (Boolean.TRUE.equals(holiday.getRecurringAnnual())) {
                        List<LocalDate> dates = new ArrayList<>();
                        for (int year = start.getYear(); year <= end.getYear(); year++) {
                            try { dates.add(holiday.getHolidayDate().withYear(year)); } catch (Exception ignored) { }
                        }
                        return dates.stream();
                    }
                    return Stream.of(holiday.getHolidayDate());
                }).collect(Collectors.toSet());
        long count = 0;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            boolean weekendOrRestDay = !isWorkingDay(calendar, date.getDayOfWeek());
            if (!Boolean.TRUE.equals(type.getIncludeWeekends()) && weekendOrRestDay) continue;
            if (!Boolean.TRUE.equals(type.getIncludePublicHolidays()) && holidays.contains(date)) continue;
            count++;
        }
        if (count == 0) throw new IllegalArgumentException("The selected period contains no chargeable working days");
        return BigDecimal.valueOf(count).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isWorkingDay(WorkingCalendarEntity calendar, DayOfWeek day) {
        return switch (day) {
            case MONDAY -> Boolean.TRUE.equals(calendar.getMondayWorking());
            case TUESDAY -> Boolean.TRUE.equals(calendar.getTuesdayWorking());
            case WEDNESDAY -> Boolean.TRUE.equals(calendar.getWednesdayWorking());
            case THURSDAY -> Boolean.TRUE.equals(calendar.getThursdayWorking());
            case FRIDAY -> Boolean.TRUE.equals(calendar.getFridayWorking());
            case SATURDAY -> Boolean.TRUE.equals(calendar.getSaturdayWorking());
            case SUNDAY -> Boolean.TRUE.equals(calendar.getSundayWorking());
        };
    }

    private void validateRequestRules(EmploymentEntity employment, LeaveTypeEntity type, LeaveProfileRuleEntity rule,
                                      BigDecimal amount, LocalDate startDate) {
        BigDecimal minimum = decimal(type.getMinimumRequest());
        if (amount.compareTo(minimum) < 0) throw new IllegalArgumentException("Minimum request is " + minimum + " " + type.getUnit().toLowerCase(Locale.ROOT));
        if (type.getMaximumConsecutive() != null && amount.compareTo(type.getMaximumConsecutive()) > 0) {
            throw new IllegalArgumentException("Maximum consecutive request is " + type.getMaximumConsecutive());
        }
        LocalDate employmentStart = new Date(employment.getStartDate().getTime()).toLocalDate();
        if (rule.getWaitingPeriodDays() != null && startDate.isBefore(employmentStart.plusDays(rule.getWaitingPeriodDays()))) {
            throw new IllegalArgumentException("This leave type becomes available after a " + rule.getWaitingPeriodDays() + " day waiting period");
        }
    }

    private void validateEmploymentDates(EmploymentEntity employment, LocalDate start, LocalDate end) {
        if (!ACTIVE_EMPLOYMENT_STATUSES.contains(employment.getStatus())) {
            throw new IllegalArgumentException("Leave can only be requested for active or suspended employment");
        }
        LocalDate employmentStart = employment.getStartDate() == null ? LocalDate.MIN : new Date(employment.getStartDate().getTime()).toLocalDate();
        LocalDate employmentEnd = employment.getEndDate() == null ? LocalDate.MAX : new Date(employment.getEndDate().getTime()).toLocalDate();
        if (start.isBefore(employmentStart) || end.isAfter(employmentEnd)) {
            throw new IllegalArgumentException("Leave dates must fall within the employment period");
        }
    }

    private void validateLeaveTypeDates(LeaveTypeEntity leaveType, LocalDate startDate) {
        if (!Boolean.TRUE.equals(leaveType.getActive()) || startDate.isBefore(leaveType.getActiveFrom()) || startDate.isAfter(leaveType.getActiveTo())) {
            throw new IllegalArgumentException("The selected leave type is not active for the requested date");
        }
    }

    private void validateNoOverlap(String employmentId, LocalDate start, LocalDate end, String excludeId) {
        List<LeaveRequestEntity> overlaps = leaveRequestRepository.findOverlapping(employmentId, OVERLAP_STATUSES, start, end, excludeId);
        if (!overlaps.isEmpty()) {
            throw new IllegalStateException("The employee already has a pending or approved leave request overlapping this period: "
                    + overlaps.get(0).getRequestNumber());
        }
    }

    private boolean documentRequired(LeaveTypeEntity type, LeaveProfileRuleEntity rule, BigDecimal amount) {
        if (rule.getSupportingDocumentRequiredOverride() != null) return rule.getSupportingDocumentRequiredOverride();
        if (!Boolean.TRUE.equals(type.getRequiresSupportingDocument())) return false;
        return type.getDocumentRequiredAfter() == null || amount.compareTo(type.getDocumentRequiredAfter()) >= 0;
    }

    private void validateSupportingDocuments(LeaveRequestEntity entity, PreparedLeave prepared) {
        if (!prepared.supportingDocumentRequired()) return;
        List<String> objectIds = attachments(entity.getAttachmentObjectIds()).stream()
                .filter(this::hasText).map(String::trim).distinct().toList();
        if (objectIds.isEmpty() || attachmentRepository.findByObjectIdIn(objectIds).isEmpty()) {
            throw new IllegalStateException("Supporting documentation is required before this leave request can be submitted");
        }
    }

    private LeaveRequestV2CreateRequestDto merge(LeaveRequestEntity entity, LeaveRequestV2UpdateRequestDto request) {
        LeaveRequestV2CreateRequestDto merged = new LeaveRequestV2CreateRequestDto();
        merged.setType(hasText(request == null ? null : request.getType()) ? request.getType() : entity.getLeaveType());
        merged.setLeaveTypeId(hasText(request == null ? null : request.getLeaveTypeId()) ? request.getLeaveTypeId() : entity.getLeaveTypeId());
        merged.setEmployee(hasText(request == null ? null : request.getEmployee()) ? request.getEmployee() : entity.getEmployeePartnerId());
        merged.setEmploymentId(hasText(request == null ? null : request.getEmploymentId()) ? request.getEmploymentId() : entity.getEmploymentId());
        merged.setStartDate(request != null && request.getStartDate() != null ? request.getStartDate() : entity.getStartDate());
        merged.setEndDate(request != null && request.getEndDate() != null ? request.getEndDate() : entity.getEndDate());
        merged.setRequestedAmount(request != null && request.getRequestedAmount() != null ? request.getRequestedAmount()
                : request != null && request.getDays() != null ? request.getDays() : entity.getDays());
        merged.setUnit(hasText(request == null ? null : request.getUnit()) ? request.getUnit() : entity.getUnit());
        merged.setReason(request != null && request.getReason() != null ? request.getReason() : entity.getRequestReason());
        merged.setAttachmentObjectIds(request != null && request.getAttachmentObjectIds() != null
                ? request.getAttachmentObjectIds() : attachments(entity.getAttachmentObjectIds()));
        return merged;
    }

    private LeaveRequestV2ResponseDto transition(LeaveRequestEntity entity, String newStatus, String reason, String actor) {
        String oldStatus = entity.getStatus();
        entity.setStatus(newStatus);
        entity.setStatusReason(trim(reason, 1000));
        entity.setUpdatedBy(hasText(actor) ? actor : currentActor());
        entity = leaveRequestRepository.save(entity);
        recordHistory(entity.getId(), oldStatus, newStatus, reason, entity.getUpdatedBy());
        return toResponse(entity, true);
    }

    private void recordHistory(String requestId, String oldStatus, String newStatus, String reason, String actor) {
        historyRepository.save(LeaveRequestStatusHistoryEntity.builder()
                .leaveRequestId(requestId).oldStatus(oldStatus).newStatus(newStatus)
                .reason(trim(reason, 1000)).changedAt(LocalDateTime.now()).changedBy(actor).build());
    }

    private LeaveRequestV2CreateRequestDto scopeToCurrentEmployee(LeaveRequestV2CreateRequestDto request) {
        if (request == null) throw new IllegalArgumentException("Leave request is required");
        EmploymentEntity employment = leaveAccessService.currentEmployment(request.getStartDate());
        request.setEmploymentId(employment.getId());
        request.setEmployee(employment.getPartnerId());
        return request;
    }

    private void assertOwnRequest(LeaveRequestEntity entity) {
        if (entity == null || !leaveAccessService.currentPartnerId().equals(entity.getEmployeePartnerId())) {
            throw new SecurityException("You can only capture, change and track your own leave requests");
        }
    }

    private void assertCanView(LeaveRequestEntity entity) {
        if (entity == null) throw new SecurityException("Leave request access is not allowed");
        if (leaveAccessService.ownsEmployment(entity.getEmploymentId())) return;
        if (leaveAccessService.canApproveEmployment(entity.getEmploymentId())) return;
        throw new SecurityException("You can only view your own leave requests or leave for employees assigned to you for approval");
    }

    private LeaveRequestEntity require(String id) {
        if (!hasText(id)) throw new IllegalArgumentException("Leave request id is required");
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Leave request not found: " + id));
    }

    private void requireStatus(LeaveRequestEntity entity, String status, String message) {
        if (!status.equalsIgnoreCase(entity.getStatus())) throw new IllegalStateException(message);
    }

    private LeaveRequestV2ResponseDto toResponse(LeaveRequestEntity entity, boolean includeHistory) {
        LeaveTypeEntity type = resolveStoredLeaveType(entity);
        EmploymentEntity employment = hasText(entity.getEmploymentId()) ? employmentRepository.findById(entity.getEmploymentId()).orElse(null) : null;
        LeaveProfileEntity profile = hasText(entity.getLeaveProfileId()) ? configurationService.requireProfile(entity.getLeaveProfileId()) : null;
        WorkingCalendarEntity calendar = hasText(entity.getWorkingCalendarId()) ? configurationService.requireCalendar(entity.getWorkingCalendarId()) : null;
        BigDecimal available = null;
        String source = null;
        boolean supportingRequired = false;
        if (employment != null && type != null) {
            try {
                LeaveConfigurationService.ResolvedProfile resolved = configurationService.resolveProfile(employment, entity.getStartDate());
                source = resolved.getSource();
                available = balanceService.availableBalance(employment, type, entity.getStartDate());
                LeaveProfileRuleEntity rule = configurationService.requireRule(resolved.getProfile().getId(), type.getId(), entity.getStartDate());
                supportingRequired = documentRequired(type, rule, entity.getDays());
            } catch (Exception ignored) { }
        }
        List<LeaveRequestStatusHistoryV2Dto> history = includeHistory
                ? historyRepository.findByLeaveRequestIdOrderByChangedAtDesc(entity.getId()).stream()
                .map(item -> LeaveRequestStatusHistoryV2Dto.builder().id(item.getId()).oldStatus(item.getOldStatus())
                        .newStatus(item.getNewStatus()).reason(item.getReason()).changedAt(item.getChangedAt()).changedBy(item.getChangedBy()).build())
                .toList() : List.of();

        return LeaveRequestV2ResponseDto.builder()
                .id(entity.getId()).requestNumber(entity.getRequestNumber())
                .type(resolveOption(Field.LEAVE_TYPE, entity.getLeaveType()))
                .leaveType(type == null ? null : leaveTypeDto(type))
                .employee(resolvePartner(entity.getEmployeePartnerId()))
                .employmentId(entity.getEmploymentId())
                .employeeNumber(employment == null ? null : employment.getEmployeeNumber())
                .leaveProfileId(entity.getLeaveProfileId())
                .leaveProfileName(profile == null ? null : profile.getName())
                .workingCalendarId(entity.getWorkingCalendarId())
                .workingCalendarName(calendar == null ? null : calendar.getName())
                .assignmentSource(source)
                .startDate(entity.getStartDate()).endDate(entity.getEndDate()).days(entity.getDays()).unit(entity.getUnit())
                .availableBalance(available).projectedBalance(entity.getProjectedBalance())
                .requestReason(entity.getRequestReason()).attachmentObjectIds(attachments(entity.getAttachmentObjectIds()))
                .supportingDocumentRequired(supportingRequired).approvalRequestId(entity.getApprovalRequestId())
                .status(resolveOption(Field.TRANSACTION_STATUS, entity.getStatus())).statusReason(entity.getStatusReason())
                .submittedAt(entity.getSubmittedAt()).approvedAt(entity.getApprovedAt()).rejectedAt(entity.getRejectedAt())
                .cancelledAt(entity.getCancelledAt()).createdAt(entity.getCreatedAt()).updatedAt(entity.getUpdatedAt())
                .statusHistory(history).build();
    }

    private LeaveTypeEntity resolveStoredLeaveType(LeaveRequestEntity entity) {
        try {
            if (hasText(entity.getLeaveTypeId())) return configurationService.requireLeaveType(entity.getLeaveTypeId());
            if (hasText(entity.getLeaveType())) return configurationService.requireLeaveTypeByCode(entity.getLeaveType());
        } catch (Exception ignored) { }
        return null;
    }

    private LeaveTypeDto leaveTypeDto(LeaveTypeEntity e) {
        LeaveTypeDto d = new LeaveTypeDto();
        d.setId(e.getId()); d.setCode(e.getCode()); d.setName(e.getName()); d.setDescription(e.getDescription());
        d.setPaid(e.getPaid()); d.setUnit(e.getUnit()); d.setAllowHalfDay(e.getAllowHalfDay());
        d.setRequiresSupportingDocument(e.getRequiresSupportingDocument()); d.setDocumentRequiredAfter(e.getDocumentRequiredAfter());
        d.setMinimumRequest(e.getMinimumRequest()); d.setMaximumConsecutive(e.getMaximumConsecutive());
        d.setAllowNegativeBalance(e.getAllowNegativeBalance()); d.setIncludeWeekends(e.getIncludeWeekends());
        d.setIncludePublicHolidays(e.getIncludePublicHolidays()); d.setRequiresApproval(e.getRequiresApproval());
        d.setActiveFrom(e.getActiveFrom()); d.setActiveTo(e.getActiveTo()); d.setDisplayOrder(e.getDisplayOrder());
        d.setColour(e.getColour()); d.setIcon(e.getIcon()); d.setActive(e.getActive()); d.setVersion(e.getVersion());
        return d;
    }

    private PartnerDto resolvePartner(String id) {
        if (!hasText(id)) return null;
        try { return partnerService.getOptional(id); } catch (Exception ignored) { return null; }
    }

    private String partnerName(PartnerDto partner) {
        if (partner == null) return "Unknown employee";
        String name = Stream.of(partner.getName2(), partner.getName3(), partner.getName1())
                .filter(this::hasText).collect(Collectors.joining(" "));
        return hasText(name) ? name : partner.getId();
    }

    private FieldOptionDto resolveOption(String field, String code) {
        FieldOptionDto option = fieldOptionService.getFieldOption(field, code);
        if (option != null) return option;
        FieldOptionDto fallback = new FieldOptionDto();
        fallback.setField(field); fallback.setCode(code);
        fallback.setDescription(code == null ? "" : code.replace('-', ' '));
        return fallback;
    }

    private String normalizeUnit(String value) {
        String normalized = normalize(value);
        if (!Set.of("DAYS", "HOURS").contains(normalized)) throw new IllegalArgumentException("Leave unit must be DAYS or HOURS");
        return normalized;
    }

    private String currentActor() {
        if (hasText(UserContext.getCurrentUserId())) return UserContext.getCurrentUserId();
        if (hasText(UserContext.getCurrentUser())) return UserContext.getCurrentUser();
        if (hasText(UserContext.getCurrentUserPartner())) return UserContext.getCurrentUserPartner();
        return "SYSTEM";
    }

    private String json(Object value) {
        if (value == null) return null;
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalStateException("Unable to serialize leave request details", exception); }
    }

    private List<String> attachments(String value) {
        if (!hasText(value)) return List.of();
        try { return objectMapper.readValue(value, new TypeReference<List<String>>() { }); }
        catch (Exception ignored) { return List.of(); }
    }

    private BigDecimal decimal(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String trim(String value, int maxLength) {
        if (!hasText(value)) return null;
        String result = value.trim();
        if (result.length() > maxLength) throw new IllegalArgumentException("Value cannot exceed " + maxLength + " characters");
        return result;
    }

    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private String normalize(String value) { return value == null ? null : value.trim().toUpperCase(Locale.ROOT); }

    private record PreparedLeave(
            EmploymentEntity employment,
            LeaveTypeEntity leaveType,
            LeaveConfigurationService.ResolvedProfile profile,
            LeaveProfileRuleEntity rule,
            WorkingCalendarEntity calendar,
            BigDecimal requestedAmount,
            BigDecimal availableBalance,
            BigDecimal projectedBalance,
            boolean supportingDocumentRequired,
            boolean allowed,
            String message) { }
}
