package za.co.mawa.bes.service.v2;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.v2.*;
import za.co.mawa.bes.entity.v2.LeaveRequestEntity;
import za.co.mawa.bes.entity.v2.LeaveRequestStatusHistoryEntity;
import za.co.mawa.bes.repository.v2.LeaveRequestRepository;
import za.co.mawa.bes.repository.v2.LeaveRequestStatusHistoryRepository;
import za.co.mawa.bes.service.FieldOptionService;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.utils.Field;
import za.co.mawa.bes.utils.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class LeaveRequestV2Service {
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveRequestStatusHistoryRepository historyRepository;
    private final PartnerService partnerService;
    private final FieldOptionService fieldOptionService;

    public LeaveRequestV2Service(
            LeaveRequestRepository leaveRequestRepository,
            LeaveRequestStatusHistoryRepository historyRepository,
            PartnerService partnerService,
            FieldOptionService fieldOptionService) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.historyRepository = historyRepository;
        this.partnerService = partnerService;
        this.fieldOptionService = fieldOptionService;
    }

    @Transactional
    public LeaveRequestV2ResponseDto create(LeaveRequestV2CreateRequestDto request) {
        validateRequest(request == null ? null : request.getType(),
                request == null ? null : request.getEmployee(),
                request == null ? null : request.getApprover(),
                request == null ? null : request.getStartDate(),
                request == null ? null : request.getEndDate());

        String actor = currentActor();
        LocalDateTime now = LocalDateTime.now();
        LeaveRequestEntity entity = LeaveRequestEntity.builder()
                .requestNumber(nextRequestNumber())
                .employeePartnerId(request.getEmployee().trim())
                .approverPartnerId(trimToNull(request.getApprover()))
                .leaveType(normalize(request.getType()))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .days(calculateDays(request.getStartDate(), request.getEndDate()))
                .status(Status.PENDING)
                .createdAt(now)
                .createdBy(actor)
                .updatedAt(now)
                .updatedBy(actor)
                .version(0L)
                .build();

        entity = leaveRequestRepository.save(entity);
        recordHistory(entity.getId(), null, Status.PENDING, "Leave request created", actor);
        return toResponse(entity, true);
    }

    @Transactional(readOnly = true)
    public LeaveRequestV2ResponseDto get(String id) {
        return toResponse(require(id), true);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestV2ResponseDto> search(
            String status,
            String employeePartnerId,
            String approverPartnerId,
            String leaveType,
            LocalDate fromDate,
            LocalDate toDate) {
        Specification<LeaveRequestEntity> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(status)) predicates.add(cb.equal(root.get("status"), normalize(status)));
            if (hasText(employeePartnerId)) predicates.add(cb.equal(root.get("employeePartnerId"), employeePartnerId.trim()));
            if (hasText(approverPartnerId)) predicates.add(cb.equal(root.get("approverPartnerId"), approverPartnerId.trim()));
            if (hasText(leaveType)) predicates.add(cb.equal(root.get("leaveType"), normalize(leaveType)));
            if (fromDate != null) predicates.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("endDate"), fromDate));
            if (toDate != null) predicates.add(cb.lessThanOrEqualTo(root.<LocalDate>get("startDate"), toDate));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return leaveRequestRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(entity -> toResponse(entity, false))
                .toList();
    }

    @Transactional
    public LeaveRequestV2ResponseDto update(String id, LeaveRequestV2UpdateRequestDto request) {
        LeaveRequestEntity entity = require(id);
        if (!Status.PENDING.equalsIgnoreCase(entity.getStatus())) {
            throw new IllegalStateException("Only pending leave requests can be edited");
        }

        String type = hasText(request == null ? null : request.getType()) ? request.getType() : entity.getLeaveType();
        String employee = hasText(request == null ? null : request.getEmployee()) ? request.getEmployee() : entity.getEmployeePartnerId();
        String approver = request != null && request.getApprover() != null ? request.getApprover() : entity.getApproverPartnerId();
        LocalDate startDate = request != null && request.getStartDate() != null ? request.getStartDate() : entity.getStartDate();
        LocalDate endDate = request != null && request.getEndDate() != null ? request.getEndDate() : entity.getEndDate();

        validateRequest(type, employee, approver, startDate, endDate);
        entity.setLeaveType(normalize(type));
        entity.setEmployeePartnerId(employee.trim());
        entity.setApproverPartnerId(trimToNull(approver));
        entity.setStartDate(startDate);
        entity.setEndDate(endDate);
        entity.setDays(calculateDays(startDate, endDate));
        entity.setUpdatedBy(currentActor());
        entity = leaveRequestRepository.save(entity);
        return toResponse(entity, true);
    }

    @Transactional
    public LeaveRequestV2ResponseDto submit(String id) {
        return submitInternal(id, currentActor());
    }

    @Transactional
    public LeaveRequestV2ResponseDto submitFromApproval(String id, String actionBy) {
        return submitInternal(id, actionBy);
    }

    private LeaveRequestV2ResponseDto submitInternal(String id, String actor) {
        LeaveRequestEntity entity = require(id);
        if (Status.AWAITING_APPROVAL.equalsIgnoreCase(entity.getStatus())) return toResponse(entity, true);
        if (!Status.PENDING.equalsIgnoreCase(entity.getStatus())) {
            throw new IllegalStateException("Only pending leave requests can be submitted");
        }
        entity.setSubmittedAt(LocalDateTime.now());
        return transition(entity, Status.AWAITING_APPROVAL, null, actor);
    }

    @Transactional
    public LeaveRequestV2ResponseDto approve(String id, String reason) {
        return approveFromApproval(id, reason, currentActor());
    }

    @Transactional
    public LeaveRequestV2ResponseDto approveFromApproval(String id, String reason, String actionBy) {
        LeaveRequestEntity entity = require(id);
        if (Status.APPROVED.equalsIgnoreCase(entity.getStatus())) return toResponse(entity, true);
        requireStatus(entity, Status.AWAITING_APPROVAL, "Only leave requests awaiting approval can be approved");
        entity.setApprovedAt(LocalDateTime.now());
        return transition(entity, Status.APPROVED, reason, actionBy);
    }

    @Transactional
    public LeaveRequestV2ResponseDto reject(String id, String reason) {
        return rejectFromApproval(id, reason, currentActor());
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
    public LeaveRequestV2ResponseDto cancel(String id, String reason) {
        return cancelFromApproval(id, reason, currentActor());
    }

    @Transactional
    public LeaveRequestV2ResponseDto cancelFromApproval(String id, String reason, String actionBy) {
        LeaveRequestEntity entity = require(id);
        if (Status.CANCELLED.equalsIgnoreCase(entity.getStatus())) return toResponse(entity, true);
        if (Status.APPROVED.equalsIgnoreCase(entity.getStatus())) {
            throw new IllegalStateException("Approved leave requests cannot be cancelled");
        }
        entity.setCancelledAt(LocalDateTime.now());
        return transition(entity, Status.CANCELLED, reason, actionBy);
    }

    @Transactional
    public void delete(String id) {
        LeaveRequestEntity entity = require(id);
        if (!Status.PENDING.equalsIgnoreCase(entity.getStatus())) {
            throw new IllegalStateException("Only pending leave requests can be deleted");
        }
        leaveRequestRepository.delete(entity);
    }

    private LeaveRequestV2ResponseDto transition(LeaveRequestEntity entity, String newStatus, String reason, String actor) {
        String oldStatus = entity.getStatus();
        actor = hasText(actor) ? actor : currentActor();
        entity.setStatus(newStatus);
        entity.setStatusReason(trimToNull(reason));
        entity.setUpdatedBy(actor);
        LeaveRequestEntity saved = leaveRequestRepository.save(entity);
        recordHistory(saved.getId(), oldStatus, newStatus, trimToNull(reason), actor);
        return toResponse(saved, true);
    }

    private void recordHistory(String requestId, String oldStatus, String newStatus, String reason, String actor) {
        historyRepository.save(LeaveRequestStatusHistoryEntity.builder()
                .leaveRequestId(requestId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .reason(reason)
                .changedAt(LocalDateTime.now())
                .changedBy(actor)
                .build());
    }

    private LeaveRequestEntity require(String id) {
        if (!hasText(id)) throw new IllegalArgumentException("Leave request id is required");
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Leave request not found: " + id));
    }

    private void requireStatus(LeaveRequestEntity entity, String expected, String message) {
        if (!expected.equalsIgnoreCase(entity.getStatus())) throw new IllegalStateException(message);
    }

    private void validateRequest(String type, String employee, String approver, LocalDate startDate, LocalDate endDate) {
        if (!hasText(type)) throw new IllegalArgumentException("Leave type is required");
        if (!hasText(employee)) throw new IllegalArgumentException("Employee is required");
        if (startDate == null) throw new IllegalArgumentException("Start date is required");
        if (endDate == null) throw new IllegalArgumentException("End date is required");
        if (endDate.isBefore(startDate)) throw new IllegalArgumentException("End date cannot be before start date");
        if (fieldOptionService.getFieldOption(Field.LEAVE_TYPE, normalize(type)) == null) {
            throw new IllegalArgumentException("Invalid LEAVE-TYPE option: " + type);
        }
        validatePartner(employee, "Employee");
        if (hasText(approver)) validatePartner(approver, "Approver");
    }

    private void validatePartner(String partnerId, String label) {
        try {
            partnerService.getOptional(partnerId.trim());
        } catch (Exception exception) {
            throw new IllegalArgumentException(label + " partner not found: " + partnerId);
        }
    }

    private BigDecimal calculateDays(LocalDate startDate, LocalDate endDate) {
        return BigDecimal.valueOf(ChronoUnit.DAYS.between(startDate, endDate) + 1L);
    }

    private LeaveRequestV2ResponseDto toResponse(LeaveRequestEntity entity, boolean includeHistory) {
        List<LeaveRequestStatusHistoryV2Dto> history = includeHistory
                ? historyRepository.findByLeaveRequestIdOrderByChangedAtDesc(entity.getId()).stream()
                    .map(item -> LeaveRequestStatusHistoryV2Dto.builder()
                            .id(item.getId())
                            .oldStatus(item.getOldStatus())
                            .newStatus(item.getNewStatus())
                            .reason(item.getReason())
                            .changedAt(item.getChangedAt())
                            .changedBy(item.getChangedBy())
                            .build())
                    .toList()
                : List.of();

        return LeaveRequestV2ResponseDto.builder()
                .id(entity.getId())
                .requestNumber(entity.getRequestNumber())
                .type(resolveOption(Field.LEAVE_TYPE, entity.getLeaveType()))
                .employee(resolvePartner(entity.getEmployeePartnerId()))
                .approver(resolvePartner(entity.getApproverPartnerId()))
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .days(entity.getDays())
                .status(resolveOption(Field.TRANSACTION_STATUS, entity.getStatus()))
                .statusReason(entity.getStatusReason())
                .submittedAt(entity.getSubmittedAt())
                .approvedAt(entity.getApprovedAt())
                .rejectedAt(entity.getRejectedAt())
                .cancelledAt(entity.getCancelledAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .statusHistory(history)
                .build();
    }

    private PartnerDto resolvePartner(String partnerId) {
        if (!hasText(partnerId)) return null;
        try {
            return partnerService.getOptional(partnerId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private FieldOptionDto resolveOption(String field, String code) {
        FieldOptionDto option = fieldOptionService.getFieldOption(field, code);
        if (option != null) return option;
        FieldOptionDto fallback = new FieldOptionDto();
        fallback.setField(field);
        fallback.setCode(code);
        fallback.setDescription(code == null ? "" : code.replace('-', ' '));
        return fallback;
    }

    private String nextRequestNumber() {
        return "LR-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String currentActor() {
        if (hasText(UserContext.getCurrentUserId())) return UserContext.getCurrentUserId();
        if (hasText(UserContext.getCurrentUser())) return UserContext.getCurrentUser();
        if (hasText(UserContext.getCurrentUserPartner())) return UserContext.getCurrentUserPartner();
        return "SYSTEM";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
