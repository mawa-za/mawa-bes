package za.co.mawa.bes.service.v2;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingDto;
import za.co.mawa.bes.dto.v2.appointment.serviceorder.*;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoiceLineEntity;
import za.co.mawa.bes.entity.v2.AppointmentEntity;
import za.co.mawa.bes.entity.v2.AppointmentServiceOrderEntity;
import za.co.mawa.bes.entity.v2.AppointmentServiceOrderLineEntity;
import za.co.mawa.bes.repository.InvoiceRepository;
import za.co.mawa.bes.repository.v2.AppointmentRepository;
import za.co.mawa.bes.repository.v2.AppointmentServiceOrderRepository;
import za.co.mawa.bes.service.InvoiceService;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.service.ProductService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class AppointmentServiceOrderService {
    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "DRAFT", "CONFIRMED", "IN_PROGRESS", "COMPLETED", "CANCELLED", "INVOICED"
    );

    private final AppointmentServiceOrderRepository serviceOrderRepository;
    private final AppointmentRepository appointmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final NumberAllocationService numberAllocationService;
    private final ProductService productService;
    private final PartnerService partnerService;
    private final InvoiceService invoiceService;

    public AppointmentServiceOrderService(
            AppointmentServiceOrderRepository serviceOrderRepository,
            AppointmentRepository appointmentRepository,
            InvoiceRepository invoiceRepository,
            NumberAllocationService numberAllocationService,
            ProductService productService,
            PartnerService partnerService,
            InvoiceService invoiceService
    ) {
        this.serviceOrderRepository = serviceOrderRepository;
        this.appointmentRepository = appointmentRepository;
        this.invoiceRepository = invoiceRepository;
        this.numberAllocationService = numberAllocationService;
        this.productService = productService;
        this.partnerService = partnerService;
        this.invoiceService = invoiceService;
    }

    @Transactional
    public AppointmentServiceOrderResponse createFromAppointment(String appointmentId, String currentUser) {
        if (!hasText(appointmentId)) throw new IllegalArgumentException("appointmentId is required");
        Optional<AppointmentServiceOrderEntity> existing = serviceOrderRepository.findByAppointmentId(appointmentId.trim());
        if (existing.isPresent()) return toResponse(existing.get());

        AppointmentEntity appointment = appointmentRepository.findById(appointmentId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
        if ("CANCELLED".equalsIgnoreCase(appointment.getStatus()) || "MISSED".equalsIgnoreCase(appointment.getStatus())) {
            throw new IllegalStateException("A service order cannot be created for a cancelled or missed appointment");
        }

        AppointmentServiceOrderEntity order = AppointmentServiceOrderEntity.builder()
                .serviceOrderNo(generateServiceOrderNo())
                .appointmentId(appointment.getId())
                .customerPartnerId(appointment.getCustomerPartnerId())
                .assignedEmployeePartnerId(appointment.getEmployeePartnerId())
                .serviceDate(appointment.getAppointmentDate() == null ? LocalDate.now() : appointment.getAppointmentDate())
                .status("DRAFT")
                .location(trimToNull(appointment.getLocation()))
                .notes(trimToNull(appointment.getNotes()))
                .createdBy(actor(currentUser))
                .updatedBy(actor(currentUser))
                .build();

        if (hasText(appointment.getServiceProductId())) {
            ProductDto product = product(appointment.getServiceProductId());
            String description = product == null
                    ? "Appointment service"
                    : displayProduct(product);
            long price = product == null ? 0L : firstPriceInCents(product);
            AppointmentServiceOrderLineEntity line = AppointmentServiceOrderLineEntity.builder()
                    .serviceOrder(order)
                    .productId(appointment.getServiceProductId())
                    .description(description)
                    .quantity(1.0)
                    .unitPriceCents(price)
                    .discountCents(0L)
                    .taxCents(0L)
                    .employeePartnerId(appointment.getEmployeePartnerId())
                    .sortOrder(0)
                    .build();
            calculateLine(line);
            order.getLines().add(line);
        }
        calculateTotals(order);
        return toResponse(serviceOrderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public AppointmentServiceOrderResponse get(String id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<AppointmentServiceOrderResponse> search(
            String status,
            String customerId,
            String appointmentId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        Specification<AppointmentServiceOrderEntity> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(status) && !"ALL".equalsIgnoreCase(status)) {
                predicates.add(cb.equal(root.get("status"), normalizeStatus(status)));
            }
            if (hasText(customerId)) predicates.add(cb.equal(root.get("customerPartnerId"), customerId.trim()));
            if (hasText(appointmentId)) predicates.add(cb.equal(root.get("appointmentId"), appointmentId.trim()));
            if (fromDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("serviceDate"), fromDate));
            if (toDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("serviceDate"), toDate));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return serviceOrderRepository.findAll(
                        specification,
                        Sort.by(Sort.Direction.DESC, "serviceDate", "createdAt")
                ).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AppointmentServiceOrderResponse update(
            String id,
            AppointmentServiceOrderRequest request,
            String currentUser
    ) {
        AppointmentServiceOrderEntity order = find(id);
        if (request == null) throw new IllegalArgumentException("service order request is required");
        if ("INVOICED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("An invoiced service order cannot be changed. Issue a credit note or amend the invoice instead.");
        }
        if (request.getServiceDate() != null) order.setServiceDate(request.getServiceDate());
        if (request.getAssignedEmployeePartnerId() != null) {
            order.setAssignedEmployeePartnerId(trimToNull(request.getAssignedEmployeePartnerId()));
        }
        if (request.getLocation() != null) order.setLocation(trimToNull(request.getLocation()));
        if (request.getNotes() != null) order.setNotes(trimToNull(request.getNotes()));
        if (request.getStatus() != null) {
            String requestedStatus = normalizeStatus(request.getStatus());
            if ("INVOICED".equals(requestedStatus)) {
                throw new IllegalArgumentException(
                        "Invoiced status is assigned automatically when an invoice is created"
                );
            }
            order.setStatus(requestedStatus);
        }

        if (request.getLines() != null) {
            order.getLines().clear();
            int index = 0;
            for (ServiceOrderLineRequest requestedLine : request.getLines()) {
                AppointmentServiceOrderLineEntity line = mapLine(order, requestedLine, index++);
                order.getLines().add(line);
            }
        }
        calculateTotals(order);
        order.setUpdatedBy(actor(currentUser));
        return toResponse(serviceOrderRepository.save(order));
    }

    @Transactional
    public InvoiceEntity createInvoice(String id, String currentUser) {
        AppointmentServiceOrderEntity order = find(id);
        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("A cancelled service order cannot be invoiced");
        }
        if (order.getLines().isEmpty()) {
            throw new IllegalStateException("Add at least one service or product before invoicing");
        }

        List<InvoiceEntity> existing = invoiceRepository.findBySourceTypeAndSourceId("SERVICE_ORDER", order.getId());
        if (!existing.isEmpty()) {
            InvoiceEntity invoice = existing.get(0);
            order.setInvoiceId(invoice.getId());
            order.setStatus("INVOICED");
            order.setUpdatedBy(actor(currentUser));
            serviceOrderRepository.save(order);
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
                        .build())
                .toList();

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
                .currency("ZAR")
                .notes("Created from service order " + order.getServiceOrderNo())
                .createdBy(actor(currentUser))
                .lines(new ArrayList<>(invoiceLines))
                .payments(new ArrayList<>())
                .build();
        invoice = invoiceService.createInvoice(invoice);
        order.setInvoiceId(invoice.getId());
        order.setStatus("INVOICED");
        order.setUpdatedBy(actor(currentUser));
        serviceOrderRepository.save(order);

        AppointmentEntity appointment = appointmentRepository.findById(order.getAppointmentId()).orElse(null);
        if (appointment != null && !"PROCESSED".equalsIgnoreCase(appointment.getStatus())) {
            appointment.setStatus("PROCESSED");
            appointment.setUpdatedBy(actor(currentUser));
            appointmentRepository.save(appointment);
        }
        return invoice;
    }

    private AppointmentServiceOrderLineEntity mapLine(
            AppointmentServiceOrderEntity order,
            ServiceOrderLineRequest request,
            int sortOrder
    ) {
        if (request == null) throw new IllegalArgumentException("Service order line is required");
        String productId = trimToNull(request.getProductId());
        ProductDto product = productId == null ? null : product(productId);
        String description = trimToNull(request.getDescription());
        if (description == null && product != null) description = displayProduct(product);
        if (description == null) throw new IllegalArgumentException("Each service order line requires a description");
        double quantity = request.getQuantity() == null ? 1.0 : request.getQuantity();
        if (quantity <= 0) throw new IllegalArgumentException("Service order line quantity must be greater than zero");
        long unitPrice = request.getUnitPriceCents() == null
                ? (product == null ? 0L : firstPriceInCents(product))
                : request.getUnitPriceCents();
        if (unitPrice < 0) throw new IllegalArgumentException("Unit price cannot be negative");

        AppointmentServiceOrderLineEntity line = AppointmentServiceOrderLineEntity.builder()
                .serviceOrder(order)
                .productId(productId)
                .description(description)
                .quantity(quantity)
                .unitPriceCents(unitPrice)
                .discountCents(Math.max(0L, value(request.getDiscountCents())))
                .taxCents(Math.max(0L, value(request.getTaxCents())))
                .employeePartnerId(trimToNull(request.getEmployeePartnerId()))
                .sortOrder(sortOrder)
                .build();
        calculateLine(line);
        return line;
    }

    private void calculateLine(AppointmentServiceOrderLineEntity line) {
        long subtotal = Math.round(Math.max(0.0, line.getQuantity() == null ? 1.0 : line.getQuantity())
                * Math.max(0L, value(line.getUnitPriceCents())));
        long discount = Math.min(subtotal, Math.max(0L, value(line.getDiscountCents())));
        long tax = Math.max(0L, value(line.getTaxCents()));
        line.setSubtotalCents(subtotal);
        line.setDiscountCents(discount);
        line.setTaxCents(tax);
        line.setTotalCents(Math.max(0L, subtotal - discount + tax));
    }

    private void calculateTotals(AppointmentServiceOrderEntity order) {
        long subtotal = 0L;
        long discount = 0L;
        long tax = 0L;
        long total = 0L;
        int index = 0;
        for (AppointmentServiceOrderLineEntity line : order.getLines()) {
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

    private AppointmentServiceOrderEntity find(String id) {
        if (!hasText(id)) throw new IllegalArgumentException("serviceOrderId is required");
        return serviceOrderRepository.findById(id.trim())
                .orElseThrow(() -> new IllegalArgumentException("Service order not found: " + id));
    }

    private AppointmentServiceOrderResponse toResponse(AppointmentServiceOrderEntity order) {
        AppointmentEntity appointment = appointmentRepository.findById(order.getAppointmentId()).orElse(null);
        PartnerDto customer = safePartner(order.getCustomerPartnerId());
        PartnerDto employee = safePartner(order.getAssignedEmployeePartnerId());
        return AppointmentServiceOrderResponse.builder()
                .id(order.getId())
                .serviceOrderNo(order.getServiceOrderNo())
                .appointmentId(order.getAppointmentId())
                .appointmentNo(appointment == null ? null : appointment.getAppointmentNo())
                .customerPartnerId(order.getCustomerPartnerId())
                .customerName(fullName(customer))
                .assignedEmployeePartnerId(order.getAssignedEmployeePartnerId())
                .assignedEmployeeName(fullName(employee))
                .serviceDate(order.getServiceDate())
                .status(order.getStatus())
                .location(order.getLocation())
                .notes(order.getNotes())
                .subtotalCents(value(order.getSubtotalCents()))
                .discountCents(value(order.getDiscountCents()))
                .taxCents(value(order.getTaxCents()))
                .totalCents(value(order.getTotalCents()))
                .invoiceId(order.getInvoiceId())
                .lines(order.getLines().stream().map(line -> ServiceOrderLineResponse.builder()
                        .id(line.getId())
                        .productId(line.getProductId())
                        .description(line.getDescription())
                        .quantity(line.getQuantity())
                        .unitPriceCents(value(line.getUnitPriceCents()))
                        .discountCents(value(line.getDiscountCents()))
                        .taxCents(value(line.getTaxCents()))
                        .subtotalCents(value(line.getSubtotalCents()))
                        .totalCents(value(line.getTotalCents()))
                        .employeePartnerId(line.getEmployeePartnerId())
                        .sortOrder(line.getSortOrder())
                        .build()).toList())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private String generateServiceOrderNo() {
        String number = numberAllocationService.allocateNumber("SERVICE_ORDER");
        return "SO-" + number;
    }

    private ProductDto product(String productId) {
        try {
            return productService.getOptionalById(productId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private PartnerDto safePartner(String partnerId) {
        if (!hasText(partnerId)) return null;
        try {
            return partnerService.get(partnerId);
        } catch (Exception ignored) {
            return null;
        }
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

    private String normalizeStatus(String status) {
        if (!hasText(status)) throw new IllegalArgumentException("status is required");
        String normalized = status.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported service order status: " + status);
        }
        return normalized;
    }

    private String actor(String value) {
        return hasText(value) ? value.trim() : "SYSTEM";
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}
