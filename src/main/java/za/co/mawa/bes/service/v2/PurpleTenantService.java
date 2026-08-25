package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.ContactDto;
import za.co.mawa.bes.dto.partner.PartnerCreateDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.dto.v2.appointment.AppointmentRequest;
import za.co.mawa.bes.dto.v2.appointment.AppointmentResponse;
import za.co.mawa.bes.dto.v2.purple.PurpleDtos;
import za.co.mawa.bes.dto.v2.servicemanagement.ServiceManagementDtos;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.service.PurplePlatformClient;
import za.co.mawa.bes.service.TenantAdminService;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.Status;
import za.co.mawa.bes.utils.TransactionType;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PurpleTenantService {
    private final JdbcTemplate jdbc;
    private final TenantAdminService tenantAdminService;
    private final PurplePlatformClient purplePlatformClient;
    private final AppointmentService appointmentService;
    private final PartnerService partnerService;
    private final TransactionService transactionService;
    private final ObjectMapper objectMapper;
    private final ServiceManagementService serviceManagementService;

    public PurpleTenantService(
            JdbcTemplate jdbc,
            TenantAdminService tenantAdminService,
            PurplePlatformClient purplePlatformClient,
            AppointmentService appointmentService,
            PartnerService partnerService,
            TransactionService transactionService,
            ObjectMapper objectMapper,
            ServiceManagementService serviceManagementService
    ) {
        this.jdbc = jdbc;
        this.tenantAdminService = tenantAdminService;
        this.purplePlatformClient = purplePlatformClient;
        this.appointmentService = appointmentService;
        this.partnerService = partnerService;
        this.transactionService = transactionService;
        this.objectMapper = objectMapper;
        this.serviceManagementService = serviceManagementService;
    }

    public PurpleDtos.ProviderConfigurationResponse configuration() {
        PurpleDtos.ProviderConfigurationResponse response = new PurpleDtos.ProviderConfigurationResponse();
        response.setProvider(provider());
        response.setServices(services());
        response.setAvailabilityRules(availabilityRules(null));
        return response;
    }

    public Map<String, Object> provider() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT public_slug AS publicSlug,display_name AS displayName,description,logo_url AS logoUrl,
                       contact_email AS contactEmail,contact_number AS contactNumber,
                       booking_enabled AS bookingEnabled,service_request_enabled AS serviceRequestEnabled,active
                  FROM purple_provider_enrolment WHERE id=1
                """);
        return rows.isEmpty() ? new LinkedHashMap<>() : rows.get(0);
    }

    @Transactional
    public Map<String, Object> saveProvider(PurpleDtos.ProviderEnrolmentRequest request) {
        if (request == null) throw new IllegalArgumentException("Provider enrolment is required");
        String slug = normalizeSlug(request.getPublicSlug());
        String displayName = requireText(request.getDisplayName(), "Purple display name is required");
        String actor = actor();
        jdbc.update("""
                INSERT INTO purple_provider_enrolment(
                    id,public_slug,display_name,description,logo_url,contact_email,contact_number,
                    booking_enabled,service_request_enabled,active,created_by,updated_by
                ) VALUES(1,?,?,?,?,?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE
                    public_slug=VALUES(public_slug),display_name=VALUES(display_name),description=VALUES(description),
                    logo_url=VALUES(logo_url),contact_email=VALUES(contact_email),contact_number=VALUES(contact_number),
                    booking_enabled=VALUES(booking_enabled),service_request_enabled=VALUES(service_request_enabled),
                    active=VALUES(active),updated_by=VALUES(updated_by)
                """,
                slug, displayName, trimToNull(request.getDescription()), trimToNull(request.getLogoUrl()),
                trimToNull(request.getContactEmail()), trimToNull(request.getContactNumber()),
                bool(request.getBookingEnabled()), bool(request.getServiceRequestEnabled()), bool(request.getActive()),
                actor, actor
        );
        Map<String, Object> saved = provider();
        String tenantId = requireText(TenantContext.getCurrentTenant(), "Tenant is required");
        za.co.mawa.bes.dto.TenantDto tenant = tenantAdminService.getAll().stream()
                .filter(item -> tenantId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tenant metadata was not found for Purple provider synchronization"));
        Map<String, Object> sync = new LinkedHashMap<>(saved);
        sync.put("actor", actor);
        sync.put("tenantHost", requireText(tenant.getHost(), "Tenant host is required for Purple provider synchronization"));
        sync.put("tenantUrl", tenant.getUrl());
        purplePlatformClient.upsertProvider(tenantId, sync);
        return saved;
    }

    public List<Map<String, Object>> availableProducts() {
        return jdbc.queryForList("""
                SELECT p.id,p.code,p.description,p.type,
                       (SELECT pp.value FROM product_pricing pp
                         WHERE pp.product=p.id AND pp.pricing='SELLING-PRICE'
                           AND (pp.valid_from IS NULL OR pp.valid_from<=CURRENT_DATE)
                           AND (pp.valid_to IS NULL OR pp.valid_to>=CURRENT_DATE)
                         ORDER BY pp.valid_from DESC LIMIT 1) AS price,
                       EXISTS(SELECT 1 FROM purple_service_enrolment pse WHERE pse.product_id=p.id) AS enrolled
                  FROM product p
                 WHERE (p.valid_from IS NULL OR p.valid_from<=CURRENT_DATE)
                   AND (p.valid_to IS NULL OR p.valid_to>=CURRENT_DATE)
                 ORDER BY p.description,p.code
                """);
    }

    public List<Map<String, Object>> services() {
        return jdbc.queryForList("""
                SELECT pse.id,pse.product_id AS productId,p.code AS productCode,
                       COALESCE(NULLIF(pse.display_name,''),p.description) AS displayName,
                       COALESCE(NULLIF(pse.description,''),p.description) AS description,
                       p.type AS productType,pse.booking_enabled AS bookingEnabled,
                       pse.service_request_enabled AS serviceRequestEnabled,pse.duration_minutes AS durationMinutes,
                       pse.slot_interval_minutes AS slotIntervalMinutes,pse.buffer_before_minutes AS bufferBeforeMinutes,
                       pse.buffer_after_minutes AS bufferAfterMinutes,pse.location,pse.display_order AS displayOrder,
                       pse.active,
                       (SELECT pp.value FROM product_pricing pp
                         WHERE pp.product=p.id AND pp.pricing='SELLING-PRICE'
                           AND (pp.valid_from IS NULL OR pp.valid_from<=CURRENT_DATE)
                           AND (pp.valid_to IS NULL OR pp.valid_to>=CURRENT_DATE)
                         ORDER BY pp.valid_from DESC LIMIT 1) AS price
                  FROM purple_service_enrolment pse
                  JOIN product p ON p.id=pse.product_id
                 ORDER BY pse.display_order,COALESCE(NULLIF(pse.display_name,''),p.description)
                """);
    }

    @Transactional
    public Map<String, Object> saveService(PurpleDtos.ServiceEnrolmentRequest request) {
        if (request == null) throw new IllegalArgumentException("Purple service is required");
        String productId = requireText(request.getProductId(), "Product or service is required");
        Integer productCount = jdbc.queryForObject("SELECT COUNT(*) FROM product WHERE id=?", Integer.class, productId);
        if (productCount == null || productCount == 0) throw new IllegalArgumentException("Selected MAWA product or service was not found");
        int duration = positive(request.getDurationMinutes(), 30, "Duration must be greater than zero");
        int interval = positive(request.getSlotIntervalMinutes(), duration, "Slot interval must be greater than zero");
        String id = jdbc.query("SELECT id FROM purple_service_enrolment WHERE product_id=?", rs -> rs.next() ? rs.getString(1) : null, productId);
        if (!StringUtils.hasText(id)) id = UUID.randomUUID().toString();
        String actor = actor();
        jdbc.update("""
                INSERT INTO purple_service_enrolment(
                    id,product_id,display_name,description,booking_enabled,service_request_enabled,duration_minutes,
                    slot_interval_minutes,buffer_before_minutes,buffer_after_minutes,location,display_order,active,created_by,updated_by
                ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE
                    display_name=VALUES(display_name),description=VALUES(description),booking_enabled=VALUES(booking_enabled),
                    service_request_enabled=VALUES(service_request_enabled),duration_minutes=VALUES(duration_minutes),
                    slot_interval_minutes=VALUES(slot_interval_minutes),buffer_before_minutes=VALUES(buffer_before_minutes),
                    buffer_after_minutes=VALUES(buffer_after_minutes),location=VALUES(location),display_order=VALUES(display_order),
                    active=VALUES(active),updated_by=VALUES(updated_by)
                """,
                id, productId, trimToNull(request.getDisplayName()), trimToNull(request.getDescription()),
                bool(request.getBookingEnabled()), bool(request.getServiceRequestEnabled()), duration, interval,
                nonNegative(request.getBufferBeforeMinutes()), nonNegative(request.getBufferAfterMinutes()),
                trimToNull(request.getLocation()), request.getDisplayOrder() == null ? 0 : request.getDisplayOrder(),
                request.getActive() == null || request.getActive() ? 1 : 0, actor, actor
        );
        return service(id, false);
    }

    @Transactional
    public void deleteService(String id) {
        jdbc.update("DELETE FROM purple_service_enrolment WHERE id=?", requireText(id, "Purple service is required"));
    }

    public List<Map<String, Object>> availabilityRules(String serviceId) {
        if (StringUtils.hasText(serviceId)) {
            return jdbc.queryForList("""
                    SELECT id,service_enrolment_id AS serviceEnrolmentId,employee_partner_id AS employeePartnerId,
                           day_of_week AS dayOfWeek,TIME_FORMAT(start_time,'%H:%i') AS startTime,
                           TIME_FORMAT(end_time,'%H:%i') AS endTime,valid_from AS validFrom,valid_to AS validTo,location,active
                      FROM purple_availability_rule WHERE service_enrolment_id=? ORDER BY day_of_week,start_time
                    """, serviceId);
        }
        return jdbc.queryForList("""
                SELECT id,service_enrolment_id AS serviceEnrolmentId,employee_partner_id AS employeePartnerId,
                       day_of_week AS dayOfWeek,TIME_FORMAT(start_time,'%H:%i') AS startTime,
                       TIME_FORMAT(end_time,'%H:%i') AS endTime,valid_from AS validFrom,valid_to AS validTo,location,active
                  FROM purple_availability_rule ORDER BY day_of_week,start_time
                """);
    }

    @Transactional
    public Map<String, Object> saveAvailabilityRule(PurpleDtos.AvailabilityRuleRequest request) {
        if (request == null) throw new IllegalArgumentException("Availability rule is required");
        String serviceId = requireText(request.getServiceEnrolmentId(), "Purple service is required");
        int day = request.getDayOfWeek() == null ? 0 : request.getDayOfWeek();
        if (day < 1 || day > 7) throw new IllegalArgumentException("Day of week must be between Monday (1) and Sunday (7)");
        LocalTime start = parseTime(request.getStartTime(), "Start time is required");
        LocalTime end = parseTime(request.getEndTime(), "End time is required");
        if (!end.isAfter(start)) throw new IllegalArgumentException("End time must be after start time");
        String id = StringUtils.hasText(request.getId()) ? request.getId().trim() : UUID.randomUUID().toString();
        String actor = actor();
        jdbc.update("""
                INSERT INTO purple_availability_rule(
                    id,service_enrolment_id,employee_partner_id,day_of_week,start_time,end_time,valid_from,valid_to,location,active,created_by,updated_by
                ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE
                    service_enrolment_id=VALUES(service_enrolment_id),employee_partner_id=VALUES(employee_partner_id),
                    day_of_week=VALUES(day_of_week),start_time=VALUES(start_time),end_time=VALUES(end_time),
                    valid_from=VALUES(valid_from),valid_to=VALUES(valid_to),location=VALUES(location),active=VALUES(active),updated_by=VALUES(updated_by)
                """,
                id, serviceId, trimToNull(request.getEmployeePartnerId()), day, Time.valueOf(start), Time.valueOf(end),
                request.getValidFrom(), request.getValidTo(), trimToNull(request.getLocation()),
                request.getActive() == null || request.getActive() ? 1 : 0, actor, actor
        );
        return jdbc.queryForMap("""
                SELECT id,service_enrolment_id AS serviceEnrolmentId,employee_partner_id AS employeePartnerId,
                       day_of_week AS dayOfWeek,TIME_FORMAT(start_time,'%H:%i') AS startTime,
                       TIME_FORMAT(end_time,'%H:%i') AS endTime,valid_from AS validFrom,valid_to AS validTo,location,active
                  FROM purple_availability_rule WHERE id=?
                """, id);
    }

    @Transactional
    public void deleteAvailabilityRule(String id) {
        jdbc.update("DELETE FROM purple_availability_rule WHERE id=?", requireText(id, "Availability rule is required"));
    }

    public Map<String, Object> catalog() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", provider());
        result.put("services", services().stream().filter(row -> asBoolean(row.get("active"))).toList());
        return result;
    }

    public List<Map<String, Object>> availability(PurpleDtos.AvailabilityRequest request) {
        String serviceId = requireText(request == null ? null : request.getServiceId(), "Purple service is required");
        Map<String, Object> service = service(serviceId, true);
        if (!asBoolean(service.get("bookingEnabled"))) throw new IllegalStateException("This service is not enabled for Purple bookings");
        LocalDate from = request.getFromDate() == null ? LocalDate.now() : request.getFromDate();
        LocalDate to = request.getToDate() == null ? from.plusDays(30) : request.getToDate();
        if (to.isBefore(from)) throw new IllegalArgumentException("Availability end date cannot be before start date");
        if (to.isAfter(from.plusDays(90))) to = from.plusDays(90);
        return calculateAvailability(service, from, to);
    }

    @Transactional
    public Map<String, Object> createBooking(PurpleDtos.BookingRequest request) {
        if (request == null) throw new IllegalArgumentException("Booking request is required");
        requireProviderFeature("bookingEnabled");
        Map<String, Object> service = service(requireText(request.getServiceId(), "Purple service is required"), true);
        if (!asBoolean(service.get("bookingEnabled"))) throw new IllegalStateException("This service is not enabled for Purple bookings");
        LocalDate date = Objects.requireNonNull(request.getAppointmentDate(), "Appointment date is required");
        LocalTime start = parseTime(request.getStartTime(), "Appointment time is required");
        List<Map<String, Object>> available = calculateAvailability(service, date, date);
        Map<String, Object> chosen = available.stream()
                .filter(slot -> date.toString().equals(Objects.toString(slot.get("date"))))
                .filter(slot -> start.toString().substring(0,5).equals(Objects.toString(slot.get("startTime"))))
                .findFirst().orElseThrow(() -> new IllegalStateException("The selected time is no longer available"));

        String partnerId = ensureCustomer(request);
        validateCustomerLocation(partnerId, request.getServiceLocationId());
        AppointmentRequest appointment = new AppointmentRequest();
        appointment.setCustomerPartnerId(partnerId);
        appointment.setEmployeePartnerId(Objects.toString(chosen.get("employeePartnerId"), null));
        appointment.setServiceProductId(Objects.toString(service.get("productId")));
        appointment.setServiceLocationId(trimToNull(request.getServiceLocationId()));
        appointment.setAppointmentDate(date);
        appointment.setStartTime(start);
        appointment.setDurationMinutes(asInt(service.get("durationMinutes"), 30));
        appointment.setLocation(firstNonBlank(request.getLocation(), Objects.toString(chosen.get("location"), null), Objects.toString(service.get("location"), null)));
        appointment.setNotes(trimToNull(request.getNotes()));
        appointment.setSourceType("PURPLE");
        appointment.setSourceId(request.getPurpleCustomerId());
        AppointmentResponse saved = appointmentService.create(appointment, "purple:" + request.getPurpleCustomerId());
        serviceManagementService.reserveResources(saved.getId(), Objects.toString(service.get("productId")), date, start, asInt(service.get("durationMinutes"), 30));
        String employeeKey = Objects.toString(chosen.get("employeePartnerId"), null);
        if (employeeKey == null) employeeKey = "";
        try {
            jdbc.update("""
                    INSERT INTO purple_appointment_link(
                        appointment_id,purple_customer_id,service_enrolment_id,employee_partner_key,appointment_date,start_time
                    ) VALUES(?,?,?,?,?,?)
                    """, saved.getId(), request.getPurpleCustomerId(), service.get("id"), employeeKey, date, Time.valueOf(start));
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new IllegalStateException("The selected time is no longer available", ex);
        }
        return appointmentMap(saved, Objects.toString(service.get("displayName"), null));
    }

    @Transactional
    public Map<String, Object> createServiceRequest(PurpleDtos.ServiceRequestCreate request) {
        if (request == null) throw new IllegalArgumentException("Service request is required");
        requireProviderFeature("serviceRequestEnabled");
        Map<String, Object> service = null;
        if (StringUtils.hasText(request.getServiceId())) {
            service = service(request.getServiceId(), true);
            if (!asBoolean(service.get("serviceRequestEnabled"))) throw new IllegalStateException("This service is not enabled for Purple service requests");
        }
        String partnerId = ensureCustomer(request);
        validateCustomerLocation(partnerId, request.getServiceLocationId());
        String summary = requireText(firstNonBlank(request.getSummary(), service == null ? null : Objects.toString(service.get("displayName"), null)), "Service request summary is required");
        String description = requireText(request.getDescription(), "Service request description is required");
        if (request.getAdditionalDetails() != null && !request.getAdditionalDetails().isEmpty()) {
            try { description += "\n\nPurple details: " + objectMapper.writeValueAsString(request.getAdditionalDetails()); } catch (Exception ignored) {}
        }
        TransactionCreateDto create = new TransactionCreateDto();
        create.setType(TransactionType.SERVICE_REQUEST);
        create.setCustomerId(partnerId);
        create.setDescription(summary);
        create.setSubDescription(description);
        create.setCategory(firstNonBlank(request.getCategory(), "GENERAL"));
        create.setPriority(firstNonBlank(request.getPriority(), "NORMAL"));
        create.setStatus(Status.NOT_YET_STARTED);
        create.setStatusReason(Status.SERVICE_REQUEST_STATUS_REASON);
        TransactionDto saved = transactionService.create(create);
        jdbc.update("INSERT INTO purple_service_request_link(service_request_id,purple_customer_id) VALUES(?,?)", saved.getId(), request.getPurpleCustomerId());
        ServiceManagementDtos.RequestMetadataRequest metadata = new ServiceManagementDtos.RequestMetadataRequest();
        metadata.setServiceRequestId(saved.getId());
        metadata.setProductId(service == null ? null : Objects.toString(service.get("productId"), null));
        metadata.setServiceLocationId(trimToNull(request.getServiceLocationId()));
        metadata.setSourceChannel("PURPLE");
        metadata.setExternalRequestId(request.getPurpleCustomerId() + ":" + saved.getId());
        metadata.setPreferredDate(request.getPreferredDate());
        metadata.setPreferredStartTime(request.getPreferredStartTime());
        metadata.setRecurringRequested(request.getRecurringRequested());
        metadata.setRecurrenceFrequency(request.getRecurrenceFrequency());
        metadata.setRecurrenceInterval(request.getRecurrenceInterval());
        serviceManagementService.saveRequestMetadata(metadata);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", saved.getId());
        result.put("number", saved.getNumber());
        result.put("summary", summary);
        result.put("description", description);
        result.put("status", saved.getStatus());
        result.put("serviceId", request.getServiceId());
        return result;
    }

    public List<Map<String, Object>> customerLocations(PurpleDtos.CustomerRequest customer) {
        String partnerId = ensureCustomer(customer);
        return serviceManagementService.locations(partnerId).stream().filter(row -> asBoolean(row.get("active"))).toList();
    }

    @Transactional
    public Map<String, Object> saveCustomerLocation(PurpleDtos.ServiceLocationRequest request) {
        String partnerId = ensureCustomer(request);
        if (StringUtils.hasText(request.getId())) {
            Map<String, Object> existing = serviceManagementService.locations(partnerId).stream()
                    .filter(row -> Objects.equals(request.getId(), Objects.toString(row.get("id"))))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Service location was not found for this customer"));
        }
        ServiceManagementDtos.LocationRequest location = new ServiceManagementDtos.LocationRequest();
        location.setId(request.getId()); location.setCustomerPartnerId(partnerId); location.setName(request.getName());
        location.setAddressLine1(request.getAddressLine1()); location.setAddressLine2(request.getAddressLine2());
        location.setSuburb(request.getSuburb()); location.setCity(request.getCity()); location.setProvince(request.getProvince());
        location.setPostalCode(request.getPostalCode()); location.setContactName(request.getContactName());
        location.setContactNumber(request.getContactNumber()); location.setContactEmail(request.getContactEmail());
        location.setAccessInstructions(request.getAccessInstructions()); location.setServiceNotes(request.getServiceNotes());
        location.setLatitude(request.getLatitude()); location.setLongitude(request.getLongitude()); location.setActive(request.getActive());
        return serviceManagementService.saveLocation(location);
    }

    public List<Map<String, Object>> customerContracts(PurpleDtos.CustomerRequest customer) {
        String partnerId = linkedPartner(customer);
        if (partnerId == null) return List.of();
        return serviceManagementService.contracts("ALL", partnerId).stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>(row);
            item.remove("billing_partner_id");
            return item;
        }).toList();
    }

    public List<Map<String, Object>> customerBookings(PurpleDtos.CustomerRequest customer) {
        requireCustomerId(customer);
        return jdbc.queryForList("""
                SELECT a.id,a.appointment_no AS appointmentNo,a.status,a.appointment_date AS appointmentDate,
                       TIME_FORMAT(a.start_time,'%H:%i') AS startTime,TIME_FORMAT(a.end_time,'%H:%i') AS endTime,
                       a.duration_minutes AS durationMinutes,a.location,a.notes,a.service_product_id AS productId,
                       p.description AS serviceName
                  FROM purple_appointment_link l
                  JOIN appointment a ON a.id=l.appointment_id
                  LEFT JOIN product p ON p.id=a.service_product_id
                 WHERE l.purple_customer_id=? ORDER BY a.appointment_date DESC,a.start_time DESC
                """, customer.getPurpleCustomerId());
    }

    public List<Map<String, Object>> customerServiceRequests(PurpleDtos.CustomerRequest customer) {
        requireCustomerId(customer);
        return jdbc.queryForList("""
                SELECT t.id,t.number,t.status,t.description AS summary,t.sub_description AS description,t.category,t.priority,
                       MAX(CASE WHEN td.type='CREATED' THEN td.value END) AS createdAt
                  FROM purple_service_request_link l
                  JOIN `transaction` t ON t.id=l.service_request_id
                  LEFT JOIN transaction_date td ON td.transaction=t.id
                 WHERE l.purple_customer_id=?
                 GROUP BY t.id,t.number,t.status,t.description,t.sub_description,t.category,t.priority
                 ORDER BY createdAt DESC
                """, customer.getPurpleCustomerId());
    }

    public List<Map<String, Object>> customerQuotes(PurpleDtos.CustomerRequest customer) {
        String partnerId = linkedPartner(customer);
        if (partnerId == null) return List.of();
        List<Map<String, Object>> quotes = jdbc.queryForList("""
                SELECT t.id,t.number,t.status,t.sub_description AS description,t.description AS summary,
                       MAX(CASE WHEN td.type='CREATED' THEN td.value END) AS createdAt,
                       COALESCE(SUM(ti.quantity*ti.unit_price),0) AS total
                  FROM `transaction` t
                  JOIN transaction_partner tp ON tp.transaction=t.id AND tp.partner_function='CUSTOMER'
                  LEFT JOIN transaction_date td ON td.transaction=t.id
                  LEFT JOIN transaction_item ti ON ti.transaction=t.id
                 WHERE t.type='QUOTATION' AND tp.partner=?
                 GROUP BY t.id,t.number,t.status,t.description,t.sub_description
                 ORDER BY createdAt DESC
                """, partnerId);
        for (Map<String, Object> quote : quotes) {
            quote.put("items", jdbc.queryForList("""
                    SELECT ti.item,ti.product AS productId,p.code AS productCode,p.description,
                           ti.quantity,ti.unit_price AS unitPrice,(ti.quantity*ti.unit_price) AS total
                      FROM transaction_item ti LEFT JOIN product p ON p.id=ti.product
                     WHERE ti.transaction=? ORDER BY ti.item
                    """, quote.get("id")));
        }
        return quotes;
    }

    public List<Map<String, Object>> customerInvoices(PurpleDtos.CustomerRequest customer) {
        String partnerId = linkedPartner(customer);
        if (partnerId == null) return List.of();
        List<Map<String, Object>> invoices = jdbc.queryForList("""
                SELECT id,invoice_no AS invoiceNo,external_ref AS externalRef,source_type AS sourceType,source_id AS sourceId,
                       invoice_date AS invoiceDate,due_date AS dueDate,status,subtotal_cents AS subtotalCents,
                       tax_cents AS taxCents,discount_cents AS discountCents,total_cents AS totalCents,
                       paid_cents AS paidCents,credited_cents AS creditedCents,balance_cents AS balanceCents,currency,notes
                  FROM invoice WHERE partner_id=? ORDER BY invoice_date DESC,invoice_no DESC
                """, partnerId);
        for (Map<String, Object> invoice : invoices) {
            invoice.put("lines", jdbc.queryForList("""
                    SELECT id,product_id AS productId,description,quantity,show_amount AS showAmount,
                           unit_price_cents AS unitPriceCents,discount_cents AS discountCents,
                           tax_cents AS taxCents,subtotal_cents AS subtotalCents,total_cents AS totalCents
                      FROM invoice_line WHERE invoice_id=? ORDER BY id
                    """, invoice.get("id")));
        }
        return invoices;
    }

    private List<Map<String, Object>> calculateAvailability(Map<String, Object> service, LocalDate from, LocalDate to) {
        String serviceId = Objects.toString(service.get("id"));
        String productId = Objects.toString(service.get("productId"));
        int duration = asInt(service.get("durationMinutes"), 30);
        int interval = asInt(service.get("slotIntervalMinutes"), duration);
        int before = asInt(service.get("bufferBeforeMinutes"), 0);
        int after = asInt(service.get("bufferAfterMinutes"), 0);
        List<Map<String, Object>> rules = availabilityRules(serviceId).stream().filter(row -> asBoolean(row.get("active"))).toList();
        Map<String, Map<String, Object>> slots = new LinkedHashMap<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            int day = date.getDayOfWeek().getValue();
            for (Map<String, Object> rule : rules) {
                if (asInt(rule.get("dayOfWeek"), 0) != day || !dateWithin(date, rule.get("validFrom"), rule.get("validTo"))) continue;
                LocalTime start = parseTime(Objects.toString(rule.get("startTime")), "Invalid availability start time");
                LocalTime end = parseTime(Objects.toString(rule.get("endTime")), "Invalid availability end time");
                String employee = Objects.toString(rule.get("employeePartnerId"), null);
                for (LocalTime slotStart = start; !slotStart.plusMinutes(duration).isAfter(end); slotStart = slotStart.plusMinutes(interval)) {
                    if (date.equals(LocalDate.now()) && slotStart.isBefore(LocalTime.now())) continue;
                    LocalTime slotEnd = slotStart.plusMinutes(duration);
                    if (isException(serviceId, employee, date, slotStart, slotEnd)) continue;
                    if (isBooked(productId, employee, date, slotStart.minusMinutes(before), slotEnd.plusMinutes(after))) continue;
                    ServiceManagementDtos.AvailabilityRequest resourceAvailability = new ServiceManagementDtos.AvailabilityRequest();
                    resourceAvailability.setProductId(productId);
                    resourceAvailability.setDate(date);
                    resourceAvailability.setStartTime(slotStart);
                    resourceAvailability.setDurationMinutes(duration);
                    if (!asBoolean(serviceManagementService.availability(resourceAvailability).get("available"))) continue;
                    Map<String, Object> slot = new LinkedHashMap<>();
                    slot.put("serviceId", serviceId);
                    slot.put("date", date.toString());
                    slot.put("startTime", slotStart.format(DateTimeFormatter.ofPattern("HH:mm")));
                    slot.put("endTime", slotEnd.format(DateTimeFormatter.ofPattern("HH:mm")));
                    slot.put("employeePartnerId", employee);
                    slot.put("location", firstNonBlank(Objects.toString(rule.get("location"), null), Objects.toString(service.get("location"), null)));
                    String slotKey = serviceId + "|" + date + "|" + slotStart + "|" + Objects.toString(employee, "");
                    slots.putIfAbsent(slotKey, slot);
                }
            }
        }
        return new ArrayList<>(slots.values());
    }

    private boolean isBooked(String productId, String employee, LocalDate date, LocalTime from, LocalTime to) {
        String sql = """
                SELECT COUNT(*) FROM appointment
                 WHERE service_product_id=? AND appointment_date=? AND status<>'CANCELLED'
                   AND (? IS NULL OR employee_partner_id=?)
                   AND start_time < ?
                   AND COALESCE(end_time,ADDTIME(start_time,SEC_TO_TIME(COALESCE(duration_minutes,30)*60))) > ?
                """;
        Integer count = jdbc.queryForObject(sql, Integer.class, productId, date, employee, employee, Time.valueOf(to), Time.valueOf(from));
        return count != null && count > 0;
    }

    private boolean isException(String serviceId, String employee, LocalDate date, LocalTime start, LocalTime end) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM purple_availability_exception
                 WHERE exception_date=? AND available=0
                   AND (service_enrolment_id IS NULL OR service_enrolment_id=?)
                   AND (employee_partner_id IS NULL OR employee_partner_id=?)
                   AND (start_time IS NULL OR start_time < ?)
                   AND (end_time IS NULL OR end_time > ?)
                """, Integer.class, date, serviceId, employee, Time.valueOf(end), Time.valueOf(start));
        return count != null && count > 0;
    }

    private String ensureCustomer(PurpleDtos.CustomerRequest customer) {
        requireCustomerId(customer);
        String existing = linkedPartner(customer);
        if (existing != null) return existing;
        PartnerCreateDto create = new PartnerCreateDto();
        create.setType("CUSTOMER");
        create.setName1(firstNonBlank(customer.getDisplayName(), customer.getEmail(), customer.getCellphone(), "Purple Customer"));
        PartnerDto partner = partnerService.create(create);
        tryAddContact(partner.getId(), "EMAIL", customer.getEmail());
        tryAddContact(partner.getId(), "CELLPHONE", customer.getCellphone());
        jdbc.update("INSERT INTO purple_customer_link(purple_customer_id,partner_id,email,cellphone) VALUES(?,?,?,?)",
                customer.getPurpleCustomerId(), partner.getId(), trimToNull(customer.getEmail()), trimToNull(customer.getCellphone()));
        return partner.getId();
    }

    private void tryAddContact(String partnerId, String type, String value) {
        if (!StringUtils.hasText(value)) return;
        try {
            ContactDto contact = new ContactDto();
            contact.setPartner(partnerId); contact.setType(type); contact.setValue(value.trim());
            partnerService.addContact(contact);
        } catch (Exception ignored) {
            try { jdbc.update("INSERT INTO partner_contact(partner,type,value,valid_from,valid_to) VALUES(?,?,?,CURRENT_DATE,'9999-12-31') ON DUPLICATE KEY UPDATE value=VALUES(value)", partnerId, type, value.trim()); }
            catch (Exception ignoredAgain) {}
        }
    }

    private String linkedPartner(PurpleDtos.CustomerRequest customer) {
        requireCustomerId(customer);
        return jdbc.query("SELECT partner_id FROM purple_customer_link WHERE purple_customer_id=?", rs -> rs.next() ? rs.getString(1) : null, customer.getPurpleCustomerId());
    }

    private void validateCustomerLocation(String partnerId, String serviceLocationId) {
        if (!StringUtils.hasText(serviceLocationId)) return;
        boolean owned = serviceManagementService.locations(partnerId).stream()
                .anyMatch(row -> serviceLocationId.trim().equals(Objects.toString(row.get("id"))));
        if (!owned) throw new IllegalArgumentException("Service location was not found for this customer");
    }

    private void requireCustomerId(PurpleDtos.CustomerRequest customer) {
        requireText(customer == null ? null : customer.getPurpleCustomerId(), "Purple customer is required");
    }

    private void requireProviderFeature(String feature) {
        Map<String, Object> provider = provider();
        if (provider.isEmpty() || !asBoolean(provider.get("active"))) throw new IllegalStateException("This service provider is not active on Purple");
        if (!asBoolean(provider.get(feature))) throw new IllegalStateException("This Purple feature is not enabled by the service provider");
    }

    private Map<String, Object> service(String id, boolean requireActive) {
        List<Map<String, Object>> rows = services().stream()
                .filter(row -> Objects.equals(id, Objects.toString(row.get("id"))) || Objects.equals(id, Objects.toString(row.get("productId"))))
                .toList();
        if (rows.isEmpty()) throw new IllegalArgumentException("Purple service was not found");
        Map<String, Object> service = rows.get(0);
        if (requireActive && !asBoolean(service.get("active"))) throw new IllegalStateException("Purple service is inactive");
        return service;
    }

    private Map<String, Object> appointmentMap(AppointmentResponse saved, String serviceName) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", saved.getId()); result.put("appointmentNo", saved.getAppointmentNo()); result.put("status", saved.getStatus());
        result.put("appointmentDate", saved.getAppointmentDate()); result.put("startTime", saved.getStartTime()); result.put("endTime", saved.getEndTime());
        result.put("durationMinutes", saved.getDurationMinutes()); result.put("location", saved.getLocation()); result.put("notes", saved.getNotes());
        result.put("serviceName", serviceName); result.put("productId", saved.getServiceProductId());
        return result;
    }

    private boolean dateWithin(LocalDate date, Object fromValue, Object toValue) {
        LocalDate from = asLocalDate(fromValue); LocalDate to = asLocalDate(toValue);
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }
    private LocalDate asLocalDate(Object value) {
        if (value instanceof java.sql.Date d) return d.toLocalDate();
        if (value instanceof LocalDate d) return d;
        return value == null ? null : LocalDate.parse(value.toString());
    }
    private LocalTime parseTime(String value, String message) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException(message);
        String v=value.trim(); if (v.length()==5) v += ":00"; return LocalTime.parse(v);
    }
    private int positive(Integer value, int defaultValue, String message) { int v=value==null?defaultValue:value; if(v<=0) throw new IllegalArgumentException(message); return v; }
    private int nonNegative(Integer value) { int v=value==null?0:value; if(v<0) throw new IllegalArgumentException("Buffer minutes cannot be negative"); return v; }
    private int bool(Boolean value) { return Boolean.TRUE.equals(value) ? 1 : 0; }
    private boolean asBoolean(Object value) { if(value instanceof Boolean b) return b; if(value instanceof Number n) return n.intValue()!=0; return "true".equalsIgnoreCase(Objects.toString(value,"")) || "1".equals(Objects.toString(value,"")); }
    private int asInt(Object value, int defaultValue) { if(value==null) return defaultValue; if(value instanceof Number n) return n.intValue(); try{return Integer.parseInt(value.toString());}catch(Exception e){return defaultValue;} }
    private String normalizeSlug(String value) { String slug=requireText(value,"Public provider name is required").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("(^-|-$)",""); if(slug.length()<2||slug.length()>100) throw new IllegalArgumentException("Public provider name must be between 2 and 100 characters"); return slug; }
    private String actor() { return firstNonBlank(UserContext.getCurrentUser(), UserContext.getCurrentUserPartner(), "system"); }
    private String requireText(String value,String message){if(!StringUtils.hasText(value))throw new IllegalArgumentException(message);return value.trim();}
    private String trimToNull(String value){return StringUtils.hasText(value)?value.trim():null;}
    private String firstNonBlank(String... values){if(values!=null)for(String value:values)if(StringUtils.hasText(value))return value.trim();return null;}
}
