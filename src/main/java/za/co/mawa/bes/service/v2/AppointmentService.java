package za.co.mawa.bes.service.v2;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.v2.appointment.AppointmentRequest;
import za.co.mawa.bes.dto.v2.appointment.AppointmentResponse;
import za.co.mawa.bes.entity.v2.AppointmentEntity;
import za.co.mawa.bes.entity.v2.AppointmentStatusHistoryEntity;
import za.co.mawa.bes.repository.v2.AppointmentRepository;
import za.co.mawa.bes.repository.v2.AppointmentStatusHistoryRepository;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.service.ProductService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AppointmentService {
    private static final List<String> ALLOWED_STATUSES = List.of("BOOKED", "PROCESSED", "MISSED", "CANCELLED");

    private final AppointmentRepository appointmentRepository;
    private final AppointmentStatusHistoryRepository historyRepository;
    private final PartnerService partnerService;
    private final ProductService productService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            AppointmentStatusHistoryRepository historyRepository,
            PartnerService partnerService,
            ProductService productService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.historyRepository = historyRepository;
        this.partnerService = partnerService;
        this.productService = productService;
    }

    public AppointmentResponse create(AppointmentRequest request, String currentUser) {
        validateCreate(request);
        AppointmentEntity entity = AppointmentEntity.builder()
                .appointmentNo(generateAppointmentNo())
                .customerPartnerId(firstNonBlank(request.getCustomerPartnerId(), request.getCustomerId()))
                .employeePartnerId(firstNonBlank(request.getEmployeePartnerId(), request.getEmployeeId()))
                .responsibleUserId(trimToNull(request.getResponsibleUserId()))
                .serviceProductId(firstNonBlank(request.getServiceProductId(), request.getProductId()))
                .appointmentDate(resolveDate(request))
                .startTime(resolveStartTime(request))
                .endTime(request.getEndTime())
                .durationMinutes(request.getDurationMinutes())
                .status(normalizeStatus(firstNonBlank(request.getStatus(), "BOOKED")))
                .location(trimToNull(request.getLocation()))
                .notes(trimToNull(request.getNotes()))
                .sourceType(trimToNull(request.getSourceType()))
                .sourceId(trimToNull(request.getSourceId()))
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();
        entity = appointmentRepository.save(entity);
        addHistory(entity.getId(), null, entity.getStatus(), "Appointment created", currentUser);
        return toResponse(entity);
    }

    public AppointmentResponse get(String id) {
        return toResponse(findEntity(id));
    }

    public List<AppointmentResponse> search(LocalDate bookDate, LocalDate fromDate, LocalDate toDate, String employeeId, String customerId, String status) {
        Specification<AppointmentEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (bookDate != null) predicates.add(cb.equal(root.get("appointmentDate"), bookDate));
            if (fromDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("appointmentDate"), fromDate));
            if (toDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("appointmentDate"), toDate));
            if (hasText(employeeId)) predicates.add(cb.equal(root.get("employeePartnerId"), employeeId));
            if (hasText(customerId)) predicates.add(cb.equal(root.get("customerPartnerId"), customerId));
            if (hasText(status) && !"ALL".equalsIgnoreCase(status)) predicates.add(cb.equal(root.get("status"), normalizeStatus(status)));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return appointmentRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "appointmentDate", "startTime"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AppointmentResponse update(String id, AppointmentRequest request, String currentUser) {
        AppointmentEntity entity = findEntity(id);
        if (request == null) throw new IllegalArgumentException("appointment request is required");
        if (hasText(firstNonBlank(request.getCustomerPartnerId(), request.getCustomerId()))) {
            entity.setCustomerPartnerId(firstNonBlank(request.getCustomerPartnerId(), request.getCustomerId()));
        }
        if (hasText(firstNonBlank(request.getEmployeePartnerId(), request.getEmployeeId()))) {
            entity.setEmployeePartnerId(firstNonBlank(request.getEmployeePartnerId(), request.getEmployeeId()));
        }
        if (hasText(request.getResponsibleUserId())) entity.setResponsibleUserId(request.getResponsibleUserId());
        if (hasText(firstNonBlank(request.getServiceProductId(), request.getProductId()))) {
            entity.setServiceProductId(firstNonBlank(request.getServiceProductId(), request.getProductId()));
        }
        LocalDate appointmentDate = resolveDate(request);
        if (appointmentDate != null) entity.setAppointmentDate(appointmentDate);
        LocalTime startTime = resolveStartTime(request);
        if (startTime != null) entity.setStartTime(startTime);
        if (request.getEndTime() != null) entity.setEndTime(request.getEndTime());
        if (request.getDurationMinutes() != null) entity.setDurationMinutes(request.getDurationMinutes());
        if (request.getLocation() != null) entity.setLocation(trimToNull(request.getLocation()));
        if (request.getNotes() != null) entity.setNotes(trimToNull(request.getNotes()));
        if (request.getStatus() != null) {
            String oldStatus = entity.getStatus();
            String newStatus = normalizeStatus(request.getStatus());
            entity.setStatus(newStatus);
            if (!newStatus.equals(oldStatus)) addHistory(entity.getId(), oldStatus, newStatus, "Status updated", currentUser);
        }
        entity.setUpdatedBy(currentUser);
        return toResponse(appointmentRepository.save(entity));
    }

    public AppointmentResponse updateStatus(String id, String status, String reason, String currentUser) {
        AppointmentEntity entity = findEntity(id);
        String oldStatus = entity.getStatus();
        String newStatus = normalizeStatus(status);
        entity.setStatus(newStatus);
        entity.setUpdatedBy(currentUser);
        entity = appointmentRepository.save(entity);
        if (!newStatus.equals(oldStatus)) addHistory(id, oldStatus, newStatus, reason, currentUser);
        return toResponse(entity);
    }

    public AppointmentResponse cancel(String id, String reason, String currentUser) {
        return updateStatus(id, "CANCELLED", reason == null ? "Appointment cancelled" : reason, currentUser);
    }

    public List<AppointmentStatusHistoryEntity> history(String id) {
        return historyRepository.findByAppointmentIdOrderByChangedAtDesc(id);
    }

    private AppointmentEntity findEntity(String id) {
        return appointmentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + id));
    }

    private AppointmentResponse toResponse(AppointmentEntity entity) {
        PartnerDto customer = null;
        PartnerDto employee = null;
        ProductDto product = null;
        try { if (hasText(entity.getCustomerPartnerId())) customer = partnerService.get(entity.getCustomerPartnerId()); } catch (Exception ignored) {}
        try { if (hasText(entity.getEmployeePartnerId())) employee = partnerService.get(entity.getEmployeePartnerId()); } catch (Exception ignored) {}
        try { if (hasText(entity.getServiceProductId())) product = productService.getOptionalById(entity.getServiceProductId()); } catch (Exception ignored) {}

        String time = entity.getStartTime() == null ? null : entity.getStartTime().toString();
        return AppointmentResponse.builder()
                .id(entity.getId())
                .appointmentNo(entity.getAppointmentNo())
                .number(entity.getAppointmentNo())
                .status(entity.getStatus())
                .appointmentDate(entity.getAppointmentDate())
                .bookDate(entity.getAppointmentDate() == null ? null : entity.getAppointmentDate().toString())
                .startTime(entity.getStartTime())
                .bookTime(time)
                .endTime(entity.getEndTime())
                .durationMinutes(entity.getDurationMinutes())
                .location(entity.getLocation())
                .notes(entity.getNotes())
                .sourceType(entity.getSourceType())
                .sourceId(entity.getSourceId())
                .customerPartnerId(entity.getCustomerPartnerId())
                .employeePartnerId(entity.getEmployeePartnerId())
                .responsibleUserId(entity.getResponsibleUserId())
                .serviceProductId(entity.getServiceProductId())
                .customer(customer)
                .customerPartner(customer)
                .employeeResponsible(employee)
                .employee(employee)
                .product(product)
                .productDto(product)
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private void validateCreate(AppointmentRequest request) {
        if (request == null) throw new IllegalArgumentException("appointment request is required");
        if (!hasText(firstNonBlank(request.getCustomerPartnerId(), request.getCustomerId()))) throw new IllegalArgumentException("customerPartnerId is required");
        if (resolveDate(request) == null) throw new IllegalArgumentException("appointmentDate is required");
        if (resolveStartTime(request) == null) throw new IllegalArgumentException("startTime is required");
    }

    private LocalDate resolveDate(AppointmentRequest request) {
        if (request == null) return null;
        if (request.getAppointmentDate() != null) return request.getAppointmentDate();
        if (hasText(request.getBookDate())) return LocalDate.parse(request.getBookDate().trim());
        return null;
    }

    private LocalTime resolveStartTime(AppointmentRequest request) {
        if (request == null) return null;
        if (request.getStartTime() != null) return request.getStartTime();
        if (hasText(request.getBookTime())) return LocalTime.parse(normalizeTime(request.getBookTime()));
        return null;
    }

    private String normalizeTime(String value) {
        String trimmed = value.trim();
        if (trimmed.length() == 5) return trimmed + ":00";
        return trimmed;
    }

    private String normalizeStatus(String status) {
        if (!hasText(status)) throw new IllegalArgumentException("status is required");
        String normalized = status.trim().toUpperCase(Locale.ROOT).replace('_', '-');
        if (!ALLOWED_STATUSES.contains(normalized)) throw new IllegalArgumentException("Unsupported appointment status: " + status);
        return normalized;
    }

    private String generateAppointmentNo() {
        String appointmentNo;
        do {
            appointmentNo = "APT-" + System.currentTimeMillis();
        } while (appointmentRepository.existsByAppointmentNo(appointmentNo));
        return appointmentNo;
    }

    private void addHistory(String appointmentId, String oldStatus, String newStatus, String reason, String currentUser) {
        historyRepository.save(AppointmentStatusHistoryEntity.builder()
                .appointmentId(appointmentId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .reason(reason)
                .changedBy(currentUser)
                .build());
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) if (hasText(value)) return value.trim();
        return null;
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
