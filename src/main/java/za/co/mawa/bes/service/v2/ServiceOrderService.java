package za.co.mawa.bes.service.v2;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.dto.service.request.ServiceRequestDto;
import za.co.mawa.bes.dto.v2.serviceorder.*;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoiceLineEntity;
import za.co.mawa.bes.entity.v2.*;
import za.co.mawa.bes.repository.InvoiceRepository;
import za.co.mawa.bes.repository.v2.AppointmentRepository;
import za.co.mawa.bes.repository.v2.AppointmentServiceOrderLinkRepository;
import za.co.mawa.bes.repository.v2.ServiceOrderRepository;
import za.co.mawa.bes.repository.v2.ServiceRequestServiceOrderLinkRepository;
import za.co.mawa.bes.service.InvoiceService;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.service.ProductService;
import za.co.mawa.bes.service.ServiceRequestService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class ServiceOrderService {
    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "DRAFT", "CONFIRMED", "IN_PROGRESS", "COMPLETED", "CANCELLED", "INVOICED"
    );
    private static final Set<String> ALLOWED_ITEM_TYPES = Set.of(
            "SERVICE", "PRODUCT", "CONSUMABLE", "PACKAGE", "ASSET", "CHARGE"
    );
    private static final Set<String> ALLOWED_COMPLETION_STATUSES = Set.of(
            "NOT_STARTED", "IN_PROGRESS", "COMPLETED", "CANCELLED", "NOT_REQUIRED"
    );

    private final ServiceOrderRepository serviceOrderRepository;
    private final AppointmentServiceOrderLinkRepository appointmentLinkRepository;
    private final ServiceRequestServiceOrderLinkRepository serviceRequestLinkRepository;
    private final AppointmentRepository appointmentRepository;
    private final ServiceRequestService serviceRequestService;
    private final InvoiceRepository invoiceRepository;
    private final NumberAllocationService numberAllocationService;
    private final ProductService productService;
    private final PartnerService partnerService;
    private final InvoiceService invoiceService;

    public ServiceOrderService(
            ServiceOrderRepository serviceOrderRepository,
            AppointmentServiceOrderLinkRepository appointmentLinkRepository,
            ServiceRequestServiceOrderLinkRepository serviceRequestLinkRepository,
            AppointmentRepository appointmentRepository,
            ServiceRequestService serviceRequestService,
            InvoiceRepository invoiceRepository,
            NumberAllocationService numberAllocationService,
            ProductService productService,
            PartnerService partnerService,
            InvoiceService invoiceService
    ) {
        this.serviceOrderRepository = serviceOrderRepository;
        this.appointmentLinkRepository = appointmentLinkRepository;
        this.serviceRequestLinkRepository = serviceRequestLinkRepository;
        this.appointmentRepository = appointmentRepository;
        this.serviceRequestService = serviceRequestService;
        this.invoiceRepository = invoiceRepository;
        this.numberAllocationService = numberAllocationService;
        this.productService = productService;
        this.partnerService = partnerService;
        this.invoiceService = invoiceService;
    }

    @Transactional
    public ServiceOrderResponse create(ServiceOrderRequest request, String currentUser) {
        if (request == null) throw new IllegalArgumentException("service order request is required");
        String customerId = trimToNull(request.getCustomerPartnerId());
        if (customerId == null) throw new IllegalArgumentException("Customer is required");
        validateSchedule(request.getScheduledStartAt(), request.getScheduledEndAt());

        ServiceOrderEntity order = ServiceOrderEntity.builder()
                .serviceOrderNo(generateServiceOrderNo())
                .customerPartnerId(customerId)
                .assignedEmployeePartnerId(trimToNull(request.getAssignedEmployeePartnerId()))
                .salesAreaId(trimToNull(request.getSalesAreaId()))
                .orderDate(request.getOrderDate() == null ? LocalDate.now() : request.getOrderDate())
                .scheduledStartAt(request.getScheduledStartAt())
                .scheduledEndAt(request.getScheduledEndAt())
                .status(request.getStatus() == null ? "DRAFT" : normalizeStatus(request.getStatus()))
                .location(trimToNull(request.getLocation()))
                .notes(trimToNull(request.getNotes()))
                .currency(normalizeCurrency(request.getCurrency()))
                .invoiceStatus("NOT_INVOICED")
                .createdBy(actor(currentUser))
                .updatedBy(actor(currentUser))
                .build();
        replaceLines(order, request.getLines());
        calculateTotals(order);
        return toResponse(serviceOrderRepository.save(order));
    }

    @Transactional
    public ServiceOrderResponse createFromAppointment(String appointmentId, String currentUser) {
        if (!hasText(appointmentId)) throw new IllegalArgumentException("appointmentId is required");
        Optional<AppointmentServiceOrderLinkEntity> existingLink =
                appointmentLinkRepository.findByAppointmentId(appointmentId.trim());
        if (existingLink.isPresent()) return toResponse(find(existingLink.get().getServiceOrderId()));

        AppointmentEntity appointment = appointmentRepository.findById(appointmentId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
        if ("CANCELLED".equalsIgnoreCase(appointment.getStatus()) || "MISSED".equalsIgnoreCase(appointment.getStatus())) {
            throw new IllegalStateException("A service order cannot be created for a cancelled or missed appointment");
        }

        LocalDate serviceDate = appointment.getAppointmentDate() == null ? LocalDate.now() : appointment.getAppointmentDate();
        LocalDateTime start = appointment.getStartTime() == null
                ? null : LocalDateTime.of(serviceDate, appointment.getStartTime());
        LocalDateTime end = appointment.getEndTime() == null
                ? (start != null && appointment.getDurationMinutes() != null
                    ? start.plusMinutes(appointment.getDurationMinutes()) : null)
                : LocalDateTime.of(serviceDate, appointment.getEndTime());

        ServiceOrderEntity order = ServiceOrderEntity.builder()
                .serviceOrderNo(generateServiceOrderNo())
                .customerPartnerId(appointment.getCustomerPartnerId())
                .assignedEmployeePartnerId(appointment.getEmployeePartnerId())
                .orderDate(serviceDate)
                .scheduledStartAt(start)
                .scheduledEndAt(end)
                .status("DRAFT")
                .location(trimToNull(appointment.getLocation()))
                .notes(trimToNull(appointment.getNotes()))
                .currency("ZAR")
                .invoiceStatus("NOT_INVOICED")
                .createdBy(actor(currentUser))
                .updatedBy(actor(currentUser))
                .build();

        if (hasText(appointment.getServiceProductId())) {
            ProductDto product = product(appointment.getServiceProductId());
            ServiceOrderLineEntity line = ServiceOrderLineEntity.builder()
                    .serviceOrder(order)
                    .productId(appointment.getServiceProductId())
                    .itemType(inferItemType(product, "SERVICE"))
                    .description(product == null ? "Appointment service" : displayProduct(product))
                    .quantity(1.0)
                    .unitPriceCents(product == null ? 0L : firstPriceInCents(product))
                    .discountCents(0L)
                    .taxCents(0L)
                    .employeePartnerId(appointment.getEmployeePartnerId())
                    .scheduledStartAt(start)
                    .scheduledEndAt(end)
                    .completionStatus("NOT_STARTED")
                    .sortOrder(0)
                    .build();
            calculateLine(line);
            order.getLines().add(line);
        }
        calculateTotals(order);
        order = serviceOrderRepository.save(order);
        appointmentLinkRepository.save(AppointmentServiceOrderLinkEntity.builder()
                .appointmentId(appointment.getId())
                .serviceOrderId(order.getId())
                .createdBy(actor(currentUser))
                .build());
        return toResponse(order);
    }

    @Transactional
    public ServiceOrderResponse createFromServiceRequest(String serviceRequestId, String currentUser) {
        return createFromServiceRequest(serviceRequestId, currentUser, false);
    }

    @Transactional
    public ServiceOrderResponse createFromServiceRequest(
            String serviceRequestId,
            String currentUser,
            boolean createAdditional
    ) {
        if (!hasText(serviceRequestId)) throw new IllegalArgumentException("serviceRequestId is required");
        List<ServiceRequestServiceOrderLinkEntity> existingLinks =
                serviceRequestLinkRepository.findByServiceRequestIdOrderByCreatedAtDesc(serviceRequestId.trim());
        if (!createAdditional && !existingLinks.isEmpty()) {
            return toResponse(find(existingLinks.get(0).getServiceOrderId()));
        }

        ServiceRequestDto serviceRequest;
        try {
            serviceRequest = serviceRequestService.get(serviceRequestId.trim());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Service request not found: " + serviceRequestId, exception);
        }
        if (serviceRequest == null || serviceRequest.getCustomer() == null ||
                !hasText(serviceRequest.getCustomer().getId())) {
            throw new IllegalStateException("The service request must have a customer before a service order can be created");
        }

        String employeeId = serviceRequest.getEmployeeResponsible() == null
                ? null : trimToNull(serviceRequest.getEmployeeResponsible().getId());
        if (employeeId == null && serviceRequest.getAssignee() != null && !serviceRequest.getAssignee().isEmpty()) {
            employeeId = trimToNull(serviceRequest.getAssignee().get(0).getId());
        }
        LocalDate orderDate = serviceRequest.getCreationDate() == null
                ? LocalDate.now()
                : java.time.Instant.ofEpochMilli(serviceRequest.getCreationDate().getTime())
                        .atZone(ZoneId.systemDefault()).toLocalDate();
        String notes = joinNotes(serviceRequest.getSummary(), serviceRequest.getDescription());

        ServiceOrderEntity order = ServiceOrderEntity.builder()
                .serviceOrderNo(generateServiceOrderNo())
                .customerPartnerId(serviceRequest.getCustomer().getId())
                .assignedEmployeePartnerId(employeeId)
                .orderDate(orderDate)
                .status("DRAFT")
                .notes(notes)
                .currency("ZAR")
                .invoiceStatus("NOT_INVOICED")
                .createdBy(actor(currentUser))
                .updatedBy(actor(currentUser))
                .build();
        calculateTotals(order);
        order = serviceOrderRepository.save(order);
        serviceRequestLinkRepository.save(ServiceRequestServiceOrderLinkEntity.builder()
                .serviceRequestId(serviceRequest.getId())
                .serviceOrderId(order.getId())
                .createdBy(actor(currentUser))
                .build());
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public ServiceOrderResponse get(String id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<ServiceOrderResponse> search(
            String status,
            String customerId,
            String sourceType,
            String sourceId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        if (hasText(sourceId)) {
            if ("APPOINTMENT".equalsIgnoreCase(sourceType)) {
                return appointmentLinkRepository.findByAppointmentId(sourceId.trim())
                        .map(link -> List.of(toResponse(find(link.getServiceOrderId()))))
                        .orElseGet(Collections::emptyList);
            }
            if ("SERVICE_REQUEST".equalsIgnoreCase(sourceType)) {
                return serviceRequestLinkRepository.findByServiceRequestIdOrderByCreatedAtDesc(sourceId.trim())
                        .stream()
                        .map(link -> toResponse(find(link.getServiceOrderId())))
                        .toList();
            }
            throw new IllegalArgumentException("sourceType must be APPOINTMENT or SERVICE_REQUEST when sourceId is supplied");
        }

        Specification<ServiceOrderEntity> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(status) && !"ALL".equalsIgnoreCase(status)) {
                predicates.add(cb.equal(root.get("status"), normalizeStatus(status)));
            }
            if (hasText(customerId)) predicates.add(cb.equal(root.get("customerPartnerId"), customerId.trim()));
            if (fromDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), fromDate));
            if (toDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("orderDate"), toDate));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return serviceOrderRepository.findAll(
                        specification,
                        Sort.by(Sort.Direction.DESC, "orderDate", "createdAt")
                ).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ServiceOrderResponse update(String id, ServiceOrderRequest request, String currentUser) {
        ServiceOrderEntity order = find(id);
        if (request == null) throw new IllegalArgumentException("service order request is required");
        if ("INVOICED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException(
                    "An invoiced service order cannot be changed. Issue a credit note or amend the invoice instead.");
        }
        if (request.getCustomerPartnerId() != null) {
            String customerId = trimToNull(request.getCustomerPartnerId());
            if (customerId == null) throw new IllegalArgumentException("Customer is required");
            order.setCustomerPartnerId(customerId);
        }
        if (request.getOrderDate() != null) order.setOrderDate(request.getOrderDate());
        if (request.getScheduledStartAt() != null || request.getScheduledEndAt() != null) {
            LocalDateTime start = request.getScheduledStartAt() == null
                    ? order.getScheduledStartAt() : request.getScheduledStartAt();
            LocalDateTime end = request.getScheduledEndAt() == null
                    ? order.getScheduledEndAt() : request.getScheduledEndAt();
            validateSchedule(start, end);
            order.setScheduledStartAt(start);
            order.setScheduledEndAt(end);
        }
        if (request.getAssignedEmployeePartnerId() != null) {
            order.setAssignedEmployeePartnerId(trimToNull(request.getAssignedEmployeePartnerId()));
        }
        if (request.getSalesAreaId() != null) order.setSalesAreaId(trimToNull(request.getSalesAreaId()));
        if (request.getLocation() != null) order.setLocation(trimToNull(request.getLocation()));
        if (request.getNotes() != null) order.setNotes(trimToNull(request.getNotes()));
        if (request.getCurrency() != null) order.setCurrency(normalizeCurrency(request.getCurrency()));
        if (request.getStatus() != null) {
            String requestedStatus = normalizeStatus(request.getStatus());
            if ("INVOICED".equals(requestedStatus)) {
                throw new IllegalArgumentException("Invoiced status is assigned automatically when an invoice is created");
            }
            order.setStatus(requestedStatus);
        }
        if (request.getLines() != null) replaceLines(order, request.getLines());
        calculateTotals(order);
        order.setUpdatedBy(actor(currentUser));
        return toResponse(serviceOrderRepository.save(order));
    }

    @Transactional
    public InvoiceEntity createInvoice(String id, String currentUser) {
        ServiceOrderEntity order = find(id);
        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("A cancelled service order cannot be invoiced");
        }
        if (order.getLines().isEmpty()) throw new IllegalStateException("Add at least one service or product before invoicing");

        List<InvoiceEntity> existing = invoiceRepository.findBySourceTypeAndSourceId("SERVICE_ORDER", order.getId());
        if (!existing.isEmpty()) {
            InvoiceEntity invoice = existing.get(0);
            markInvoiced(order, invoice.getId(), currentUser);
            return invoice;
        }

        calculateTotals(order);
        List<InvoiceLineEntity> invoiceLines = order.getLines().stream()
                .map(line -> InvoiceLineEntity.builder()
                        .productId(line.getProductId())
                        .description(line.getDescription())
                        .quantity(line.getQuantity())
                        .showAmount(true)
                        .unitPriceCents(value(line.getUnitPriceCents()))
                        .discountCents(value(line.getDiscountCents()))
                        .taxCents(value(line.getTaxCents()))
                        .subtotalCents(value(line.getSubtotalCents()))
                        .totalCents(value(line.getTotalCents()))
                        .build()).toList();

        InvoiceEntity invoice = InvoiceEntity.builder()
                .externalRef(order.getServiceOrderNo())
                .sourceType("SERVICE_ORDER")
                .sourceId(order.getId())
                .partnerId(order.getCustomerPartnerId())
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now())
                .status("DRAFT")
                .subtotalCents(value(order.getSubtotalCents()))
                .discountCents(value(order.getDiscountCents()))
                .taxCents(value(order.getTaxCents()))
                .totalCents(value(order.getTotalCents()))
                .paidCents(0L)
                .creditedCents(0L)
                .balanceCents(value(order.getTotalCents()))
                .currency(order.getCurrency())
                .notes("Created from service order " + order.getServiceOrderNo())
                .createdBy(actor(currentUser))
                .lines(new ArrayList<>(invoiceLines))
                .payments(new ArrayList<>())
                .build();
        invoice = invoiceService.createInvoice(invoice);
        markInvoiced(order, invoice.getId(), currentUser);
        return invoice;
    }

    private void markInvoiced(ServiceOrderEntity order, String invoiceId, String currentUser) {
        order.setInvoiceId(invoiceId);
        order.setInvoiceStatus("INVOICED");
        order.setStatus("INVOICED");
        order.setUpdatedBy(actor(currentUser));
        serviceOrderRepository.save(order);

        appointmentLinkRepository.findByServiceOrderId(order.getId()).ifPresent(link ->
                appointmentRepository.findById(link.getAppointmentId()).ifPresent(appointment -> {
                    if (!"PROCESSED".equalsIgnoreCase(appointment.getStatus())) {
                        appointment.setStatus("PROCESSED");
                        appointment.setUpdatedBy(actor(currentUser));
                        appointmentRepository.save(appointment);
                    }
                }));
    }

    private void replaceLines(ServiceOrderEntity order, List<ServiceOrderLineRequest> requestedLines) {
        order.getLines().clear();
        if (requestedLines == null) return;
        int index = 0;
        for (ServiceOrderLineRequest request : requestedLines) {
            order.getLines().add(mapLine(order, request, index++));
        }
    }

    private ServiceOrderLineEntity mapLine(ServiceOrderEntity order, ServiceOrderLineRequest request, int sortOrder) {
        if (request == null) throw new IllegalArgumentException("Service order line is required");
        String productId = trimToNull(request.getProductId());
        ProductDto product = productId == null ? null : product(productId);
        String description = trimToNull(request.getDescription());
        if (description == null && product != null) description = displayProduct(product);
        if (description == null) throw new IllegalArgumentException("Each service order line requires a description");
        double quantity = request.getQuantity() == null ? 1.0 : request.getQuantity();
        if (quantity <= 0) throw new IllegalArgumentException("Service order line quantity must be greater than zero");
        long unitPrice = request.getUnitPriceCents() == null
                ? (product == null ? 0L : firstPriceInCents(product)) : request.getUnitPriceCents();
        if (unitPrice < 0) throw new IllegalArgumentException("Unit price cannot be negative");
        LocalDateTime scheduledStartAt = request.getScheduledStartAt();
        LocalDateTime scheduledEndAt = request.getScheduledEndAt();
        validateSchedule(scheduledStartAt, scheduledEndAt);

        ServiceOrderLineEntity line = ServiceOrderLineEntity.builder()
                .serviceOrder(order)
                .productId(productId)
                .itemType(normalizeItemType(request.getItemType(), product))
                .description(description)
                .quantity(quantity)
                .unitPriceCents(unitPrice)
                .discountCents(Math.max(0L, value(request.getDiscountCents())))
                .taxCents(Math.max(0L, value(request.getTaxCents())))
                .employeePartnerId(trimToNull(request.getEmployeePartnerId()))
                .scheduledStartAt(scheduledStartAt)
                .scheduledEndAt(scheduledEndAt)
                .completionStatus(normalizeCompletionStatus(request.getCompletionStatus()))
                .sortOrder(sortOrder)
                .build();
        calculateLine(line);
        return line;
    }

    private void calculateLine(ServiceOrderLineEntity line) {
        long subtotal = Math.round(Math.max(0.0, line.getQuantity() == null ? 1.0 : line.getQuantity())
                * Math.max(0L, value(line.getUnitPriceCents())));
        long discount = Math.min(subtotal, Math.max(0L, value(line.getDiscountCents())));
        long tax = Math.max(0L, value(line.getTaxCents()));
        line.setSubtotalCents(subtotal);
        line.setDiscountCents(discount);
        line.setTaxCents(tax);
        line.setTotalCents(Math.max(0L, subtotal - discount + tax));
    }

    private void calculateTotals(ServiceOrderEntity order) {
        long subtotal = 0L, discount = 0L, tax = 0L, total = 0L;
        int index = 0;
        for (ServiceOrderLineEntity line : order.getLines()) {
            line.setServiceOrder(order);
            line.setSortOrder(index++);
            calculateLine(line);
            subtotal += value(line.getSubtotalCents());
            discount += value(line.getDiscountCents());
            tax += value(line.getTaxCents());
            total += value(line.getTotalCents());
        }
        order.setSubtotalCents(subtotal);
        order.setDiscountCents(discount);
        order.setTaxCents(tax);
        order.setTotalCents(total);
    }

    private ServiceOrderEntity find(String id) {
        if (!hasText(id)) throw new IllegalArgumentException("serviceOrderId is required");
        return serviceOrderRepository.findById(id.trim())
                .orElseThrow(() -> new IllegalArgumentException("Service order not found: " + id));
    }

    private ServiceOrderResponse toResponse(ServiceOrderEntity order) {
        PartnerDto customer = safePartner(order.getCustomerPartnerId());
        PartnerDto employee = safePartner(order.getAssignedEmployeePartnerId());
        List<ServiceOrderSourceResponse> sources = new ArrayList<>();
        appointmentLinkRepository.findByServiceOrderId(order.getId()).ifPresent(link -> {
            AppointmentEntity appointment = appointmentRepository.findById(link.getAppointmentId()).orElse(null);
            sources.add(ServiceOrderSourceResponse.builder()
                    .sourceType("APPOINTMENT")
                    .sourceId(link.getAppointmentId())
                    .sourceNo(appointment == null ? null : appointment.getAppointmentNo())
                    .build());
        });
        serviceRequestLinkRepository.findByServiceOrderId(order.getId()).ifPresent(link -> {
            String requestNo = null;
            try {
                ServiceRequestDto request = serviceRequestService.get(link.getServiceRequestId());
                requestNo = request == null ? null : request.getNumber();
            } catch (Exception ignored) {
            }
            sources.add(ServiceOrderSourceResponse.builder()
                    .sourceType("SERVICE_REQUEST")
                    .sourceId(link.getServiceRequestId())
                    .sourceNo(requestNo)
                    .build());
        });
        return ServiceOrderResponse.builder()
                .id(order.getId())
                .serviceOrderNo(order.getServiceOrderNo())
                .customerPartnerId(order.getCustomerPartnerId())
                .customerName(fullName(customer))
                .assignedEmployeePartnerId(order.getAssignedEmployeePartnerId())
                .assignedEmployeeName(fullName(employee))
                .salesAreaId(order.getSalesAreaId())
                .orderDate(order.getOrderDate())
                .scheduledStartAt(order.getScheduledStartAt())
                .scheduledEndAt(order.getScheduledEndAt())
                .status(order.getStatus())
                .location(order.getLocation())
                .notes(order.getNotes())
                .subtotalCents(value(order.getSubtotalCents()))
                .discountCents(value(order.getDiscountCents()))
                .taxCents(value(order.getTaxCents()))
                .totalCents(value(order.getTotalCents()))
                .currency(order.getCurrency())
                .invoiceId(order.getInvoiceId())
                .invoiceStatus(order.getInvoiceStatus())
                .sources(sources)
                .lines(order.getLines().stream().map(line -> ServiceOrderLineResponse.builder()
                        .id(line.getId())
                        .productId(line.getProductId())
                        .itemType(line.getItemType())
                        .description(line.getDescription())
                        .quantity(line.getQuantity())
                        .unitPriceCents(value(line.getUnitPriceCents()))
                        .discountCents(value(line.getDiscountCents()))
                        .taxCents(value(line.getTaxCents()))
                        .subtotalCents(value(line.getSubtotalCents()))
                        .totalCents(value(line.getTotalCents()))
                        .employeePartnerId(line.getEmployeePartnerId())
                        .scheduledStartAt(line.getScheduledStartAt())
                        .scheduledEndAt(line.getScheduledEndAt())
                        .completionStatus(line.getCompletionStatus())
                        .sortOrder(line.getSortOrder())
                        .build()).toList())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private String generateServiceOrderNo() {
        return "SO-" + numberAllocationService.allocateNumber("SERVICE_ORDER");
    }

    private ProductDto product(String productId) {
        try { return productService.getOptionalById(productId); }
        catch (Exception ignored) { return null; }
    }

    private PartnerDto safePartner(String partnerId) {
        if (!hasText(partnerId)) return null;
        try { return partnerService.get(partnerId); }
        catch (Exception ignored) { return null; }
    }

    private String fullName(PartnerDto partner) {
        if (partner == null) return null;
        return java.util.stream.Stream.of(partner.getName2(), partner.getName3(), partner.getName1(), partner.getName4())
                .filter(this::hasText)
                .reduce((left, right) -> left + " " + right)
                .orElse(partner.getNumber() == null ? partner.getId() : partner.getNumber());
    }

    private String displayProduct(ProductDto product) {
        String code = product.getCode() == null ? "" : product.getCode().trim();
        String description = product.getDescription() == null ? "" : product.getDescription().trim();
        if (code.isEmpty()) return description.isEmpty() ? "Service" : description;
        return description.isEmpty() ? code : code + " - " + description;
    }

    private long firstPriceInCents(ProductDto product) {
        if (product == null || product.getPricings() == null) return 0L;
        for (ProductPricingDto pricing : product.getPricings()) {
            BigDecimal amount = pricing.getValue();
            if (amount != null) return amount.multiply(BigDecimal.valueOf(100)).longValue();
        }
        return 0L;
    }

    private String normalizeItemType(String requestedItemType, ProductDto product) {
        String normalized = trimToNull(requestedItemType);
        if (normalized == null) return inferItemType(product, "SERVICE");
        normalized = normalized.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (!ALLOWED_ITEM_TYPES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Unsupported service order item type. Use SERVICE, PRODUCT, CONSUMABLE, PACKAGE, ASSET or CHARGE");
        }
        return normalized;
    }

    private String inferItemType(ProductDto product, String fallback) {
        if (product == null || product.getType() == null || !hasText(product.getType().getCode())) return fallback;
        String code = product.getType().getCode().trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (code.contains("CONSUMABLE")) return "CONSUMABLE";
        if (code.contains("PACKAGE")) return "PACKAGE";
        if (code.contains("ASSET")) return "ASSET";
        if (code.contains("SERVICE")) return "SERVICE";
        return "PRODUCT";
    }

    private String normalizeCompletionStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) return "NOT_STARTED";
        normalized = normalized.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (!ALLOWED_COMPLETION_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Unsupported line completion status. Use NOT_STARTED, IN_PROGRESS, COMPLETED, CANCELLED or NOT_REQUIRED");
        }
        return normalized;
    }

    private void validateSchedule(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !end.isAfter(start)) {
            throw new IllegalArgumentException("Scheduled end must be after scheduled start");
        }
    }

    private String joinNotes(String summary, String description) {
        String cleanSummary = trimToNull(summary);
        String cleanDescription = trimToNull(description);
        if (cleanSummary == null) return cleanDescription;
        if (cleanDescription == null || cleanSummary.equals(cleanDescription)) return cleanSummary;
        return cleanSummary + "\n\n" + cleanDescription;
    }

    private String normalizeStatus(String status) {
        if (!hasText(status)) throw new IllegalArgumentException("status is required");
        String normalized = status.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported service order status: " + status);
        }
        return normalized;
    }

    private String normalizeCurrency(String currency) {
        String value = trimToNull(currency);
        if (value == null) return "ZAR";
        value = value.toUpperCase(Locale.ROOT);
        if (value.length() != 3) throw new IllegalArgumentException("Currency must be a three-letter code");
        return value;
    }

    private String actor(String value) { return hasText(value) ? value.trim() : "SYSTEM"; }
    private String trimToNull(String value) { return hasText(value) ? value.trim() : null; }
    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private long value(Long value) { return value == null ? 0L : value; }
}
