package za.co.mawa.bes.service.v2;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.v2.appointment.AppointmentRequest;
import za.co.mawa.bes.dto.v2.appointment.AppointmentResponse;
import za.co.mawa.bes.dto.v2.servicemanagement.ServiceManagementDtos;
import za.co.mawa.bes.dto.v2.serviceorder.ServiceOrderLineRequest;
import za.co.mawa.bes.dto.v2.serviceorder.ServiceOrderRequest;
import za.co.mawa.bes.dto.v2.serviceorder.ServiceOrderResponse;

import java.sql.Time;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Service
public class ServiceManagementService {
    private static final Set<String> CONTRACT_STATUSES = Set.of("DRAFT", "ACTIVE", "SUSPENDED", "EXPIRED", "CANCELLED");
    private static final Set<String> FREQUENCIES = Set.of("DAILY", "WEEKLY", "MONTHLY");
    private static final Set<String> RESOURCE_TYPES = Set.of("EMPLOYEE", "TEAM", "FACILITY", "EQUIPMENT", "CAPACITY");

    private final JdbcTemplate jdbc;
    private final NumberAllocationService numbering;
    private final ServiceOrderService serviceOrderService;
    private final AppointmentService appointmentService;

    public ServiceManagementService(JdbcTemplate jdbc, NumberAllocationService numbering,
                                    ServiceOrderService serviceOrderService, AppointmentService appointmentService) {
        this.jdbc = jdbc;
        this.numbering = numbering;
        this.serviceOrderService = serviceOrderService;
        this.appointmentService = appointmentService;
    }

    public Map<String, Object> dashboard() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activeContracts", count("SELECT COUNT(*) FROM service_contract WHERE status='ACTIVE'"));
        result.put("todayAppointments", count("SELECT COUNT(*) FROM appointment WHERE appointment_date=CURRENT_DATE AND status<>'CANCELLED'"));
        result.put("readyOrders", count("SELECT COUNT(*) FROM service_order WHERE status IN ('READY','CONFIRMED','SCHEDULED')"));
        result.put("inProgressOrders", count("SELECT COUNT(*) FROM service_order WHERE status='IN_PROGRESS'"));
        result.put("completedNotInvoiced", count("SELECT COUNT(*) FROM service_order WHERE status='COMPLETED' AND invoice_status='NOT_INVOICED'"));
        result.put("pendingResource", count("SELECT COUNT(*) FROM service_contract_occurrence WHERE status='PENDING_RESOURCE'"));
        return result;
    }

    public List<Map<String, Object>> locations(String customerPartnerId) {
        if (StringUtils.hasText(customerPartnerId)) {
            return jdbc.queryForList("SELECT * FROM service_location WHERE customer_partner_id=? ORDER BY active DESC,name", customerPartnerId.trim());
        }
        return jdbc.queryForList("SELECT * FROM service_location ORDER BY active DESC,updated_at DESC");
    }

    @Transactional
    public Map<String, Object> saveLocation(ServiceManagementDtos.LocationRequest r) {
        require(r != null, "Service location is required");
        String customer = text(r.getCustomerPartnerId(), "Customer is required");
        String name = text(r.getName(), "Location name is required");
        String id = StringUtils.hasText(r.getId()) ? r.getId().trim() : UUID.randomUUID().toString();
        jdbc.update("""
            INSERT INTO service_location(id,customer_partner_id,name,address_line1,address_line2,suburb,city,province,postal_code,
              contact_name,contact_number,contact_email,access_instructions,service_notes,latitude,longitude,active,created_by,updated_by)
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE customer_partner_id=VALUES(customer_partner_id),name=VALUES(name),address_line1=VALUES(address_line1),
              address_line2=VALUES(address_line2),suburb=VALUES(suburb),city=VALUES(city),province=VALUES(province),postal_code=VALUES(postal_code),
              contact_name=VALUES(contact_name),contact_number=VALUES(contact_number),contact_email=VALUES(contact_email),
              access_instructions=VALUES(access_instructions),service_notes=VALUES(service_notes),latitude=VALUES(latitude),longitude=VALUES(longitude),
              active=VALUES(active),updated_by=VALUES(updated_by)
            """, id, customer, name, trim(r.getAddressLine1()), trim(r.getAddressLine2()), trim(r.getSuburb()), trim(r.getCity()),
                trim(r.getProvince()), trim(r.getPostalCode()), trim(r.getContactName()), trim(r.getContactNumber()), trim(r.getContactEmail()),
                trim(r.getAccessInstructions()), trim(r.getServiceNotes()), r.getLatitude(), r.getLongitude(), bool(r.getActive(), true), actor(), actor());
        return row("SELECT * FROM service_location WHERE id=?", id);
    }

    public List<Map<String, Object>> resources() {
        return jdbc.queryForList("SELECT * FROM service_resource ORDER BY active DESC,resource_type,name");
    }

    @Transactional
    public Map<String, Object> saveResource(ServiceManagementDtos.ResourceRequest r) {
        require(r != null, "Service resource is required");
        String name = text(r.getName(), "Resource name is required");
        String type = normalize(r.getResourceType(), "RESOURCE TYPE", RESOURCE_TYPES);
        int capacity = r.getCapacity() == null ? 1 : r.getCapacity();
        require(capacity > 0, "Resource capacity must be greater than zero");
        String id = StringUtils.hasText(r.getId()) ? r.getId().trim() : UUID.randomUUID().toString();
        String no = exists("SELECT COUNT(*) FROM service_resource WHERE id=?", id)
                ? Objects.toString(row("SELECT resource_no FROM service_resource WHERE id=?", id).get("resource_no"))
                : numbering.allocateNumber("SERVICE_RESOURCE");
        jdbc.update("""
            INSERT INTO service_resource(id,resource_no,name,resource_type,employee_partner_id,capacity,location,active,notes,created_by,updated_by)
            VALUES(?,?,?,?,?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE name=VALUES(name),resource_type=VALUES(resource_type),employee_partner_id=VALUES(employee_partner_id),
              capacity=VALUES(capacity),location=VALUES(location),active=VALUES(active),notes=VALUES(notes),updated_by=VALUES(updated_by)
            """, id, no, name, type, trim(r.getEmployeePartnerId()), capacity, trim(r.getLocation()), bool(r.getActive(), true), trim(r.getNotes()), actor(), actor());
        return row("SELECT * FROM service_resource WHERE id=?", id);
    }

    public List<Map<String, Object>> requirements(String productId) {
        return jdbc.queryForList("""
            SELECT rr.*,r.resource_no,r.name AS resource_name,r.capacity
              FROM service_resource_requirement rr LEFT JOIN service_resource r ON r.id=rr.resource_id
             WHERE rr.product_id=? ORDER BY rr.resource_type,r.name
            """, text(productId, "Product is required"));
    }

    @Transactional
    public Map<String, Object> saveRequirement(ServiceManagementDtos.ResourceRequirementRequest r) {
        require(r != null, "Resource requirement is required");
        String productId = text(r.getProductId(), "Product is required");
        String type = normalize(r.getResourceType(), "RESOURCE TYPE", RESOURCE_TYPES);
        int quantity = r.getQuantity() == null ? 1 : r.getQuantity();
        require(quantity > 0, "Resource quantity must be greater than zero");
        String id = StringUtils.hasText(r.getId()) ? r.getId().trim() : UUID.randomUUID().toString();
        jdbc.update("""
            INSERT INTO service_resource_requirement(id,product_id,resource_type,resource_id,quantity,mandatory,created_by)
            VALUES(?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE product_id=VALUES(product_id),resource_type=VALUES(resource_type),resource_id=VALUES(resource_id),
              quantity=VALUES(quantity),mandatory=VALUES(mandatory)
            """, id, productId, type, trim(r.getResourceId()), quantity, bool(r.getMandatory(), true), actor());
        return row("SELECT * FROM service_resource_requirement WHERE id=?", id);
    }

    @Transactional
    public void deleteRequirement(String id) {
        jdbc.update("DELETE FROM service_resource_requirement WHERE id=?", text(id, "Requirement is required"));
    }

    public List<Map<String, Object>> contracts(String status, String customerPartnerId) {
        StringBuilder sql = new StringBuilder("""
            SELECT c.*,l.name AS service_location_name,
                   (SELECT COUNT(*) FROM service_contract_schedule s WHERE s.service_contract_id=c.id AND s.active=1) AS active_schedules
              FROM service_contract c LEFT JOIN service_location l ON l.id=c.service_location_id WHERE 1=1
            """);
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) { sql.append(" AND c.status=?"); args.add(status.trim().toUpperCase()); }
        if (StringUtils.hasText(customerPartnerId)) { sql.append(" AND c.customer_partner_id=?"); args.add(customerPartnerId.trim()); }
        sql.append(" ORDER BY c.updated_at DESC");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    public Map<String, Object> contract(String id) {
        Map<String, Object> result = new LinkedHashMap<>(row("""
            SELECT c.*,l.name AS service_location_name,l.address_line1,l.suburb,l.city
              FROM service_contract c LEFT JOIN service_location l ON l.id=c.service_location_id WHERE c.id=?
            """, text(id, "Contract is required")));
        result.put("lines", jdbc.queryForList("SELECT * FROM service_contract_line WHERE service_contract_id=? ORDER BY sort_order,id", id));
        result.put("schedules", jdbc.queryForList("SELECT * FROM service_contract_schedule WHERE service_contract_id=? ORDER BY created_at", id));
        result.put("occurrences", jdbc.queryForList("SELECT * FROM service_contract_occurrence WHERE schedule_id IN (SELECT id FROM service_contract_schedule WHERE service_contract_id=?) ORDER BY occurrence_date DESC LIMIT 100", id));
        return result;
    }

    @Transactional
    public Map<String, Object> saveContract(ServiceManagementDtos.ContractRequest r) {
        require(r != null, "Service contract is required");
        String customer = text(r.getCustomerPartnerId(), "Customer is required");
        LocalDate start = Objects.requireNonNull(r.getStartDate(), "Contract start date is required");
        if (r.getEndDate() != null) require(!r.getEndDate().isBefore(start), "Contract end date cannot be before start date");
        String status = normalize(r.getStatus() == null ? "DRAFT" : r.getStatus(), "CONTRACT STATUS", CONTRACT_STATUSES);
        String id = StringUtils.hasText(r.getId()) ? r.getId().trim() : UUID.randomUUID().toString();
        String contractNo = exists("SELECT COUNT(*) FROM service_contract WHERE id=?", id)
                ? Objects.toString(row("SELECT contract_no FROM service_contract WHERE id=?", id).get("contract_no"))
                : numbering.allocateNumber("SERVICE_CONTRACT");
        jdbc.update("""
            INSERT INTO service_contract(id,contract_no,customer_partner_id,billing_partner_id,service_location_id,quotation_id,status,start_date,end_date,
              billing_frequency,billing_timing,billing_mode,currency,notes,created_by,updated_by)
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE customer_partner_id=VALUES(customer_partner_id),billing_partner_id=VALUES(billing_partner_id),
              service_location_id=VALUES(service_location_id),quotation_id=VALUES(quotation_id),status=VALUES(status),start_date=VALUES(start_date),
              end_date=VALUES(end_date),billing_frequency=VALUES(billing_frequency),billing_timing=VALUES(billing_timing),billing_mode=VALUES(billing_mode),
              currency=VALUES(currency),notes=VALUES(notes),updated_by=VALUES(updated_by)
            """, id, contractNo, customer, trim(r.getBillingPartnerId()), trim(r.getServiceLocationId()), trim(r.getQuotationId()), status, start,
                r.getEndDate(), upper(r.getBillingFrequency(), "MONTHLY"), upper(r.getBillingTiming(), "ARREARS"), upper(r.getBillingMode(), "FIXED_PERIODIC"),
                upper(r.getCurrency(), "ZAR"), trim(r.getNotes()), actor(), actor());
        if (r.getLines() != null) replaceLines(id, r.getLines());
        if (r.getSchedules() != null) replaceSchedules(id, start, r.getSchedules());
        return contract(id);
    }

    @Transactional
    public Map<String, Object> changeContractStatus(String id, String status) {
        String normalized = normalize(status, "CONTRACT STATUS", CONTRACT_STATUSES);
        jdbc.update("UPDATE service_contract SET status=?,updated_by=? WHERE id=?", normalized, actor(), text(id, "Contract is required"));
        return contract(id);
    }

    public Map<String, Object> availability(ServiceManagementDtos.AvailabilityRequest r) {
        require(r != null, "Availability request is required");
        String product = text(r.getProductId(), "Product is required");
        LocalDate date = Objects.requireNonNull(r.getDate(), "Date is required");
        LocalTime start = Objects.requireNonNull(r.getStartTime(), "Start time is required");
        int duration = r.getDurationMinutes() == null || r.getDurationMinutes() <= 0 ? 30 : r.getDurationMinutes();
        LocalTime end = start.plusMinutes(duration);
        Allocation allocation = allocateResources(product, date, start, end);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("available", allocation.available());
        result.put("resources", allocation.resources().stream().map(resource -> Map.of(
                "id", resource.id(),
                "quantity", resource.quantity(),
                "name", resource.name()
        )).toList());
        result.put("missingRequirements", allocation.missingRequirements());
        return result;
    }

    public List<Map<String, Object>> requests(String status) {
        StringBuilder sql = new StringBuilder("""
            SELECT t.id,t.no,t.number,t.status,t.description AS summary,t.sub_description AS description,
                   tp.partner AS customer_partner_id,
                   TRIM(CONCAT_WS(' ',p.name1,p.name2,p.name3)) AS customer_name,
                   m.product_id,m.service_location_id,m.source_channel,m.external_request_id,m.preferred_date,
                   m.preferred_start_time,m.recurring_requested,m.recurrence_frequency,m.recurrence_interval,
                   sl.name AS service_location_name,pr.description AS service_name
              FROM transaction t
              LEFT JOIN transaction_partner tp ON tp.transaction=t.id AND tp.partner_function='CUSTOMER'
              LEFT JOIN partner p ON p.id=tp.partner
              LEFT JOIN service_request_metadata m ON m.service_request_id=t.id
              LEFT JOIN service_location sl ON sl.id=m.service_location_id
              LEFT JOIN product pr ON pr.id=m.product_id
             WHERE t.type='SERVICE-REQUEST'
            """);
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
            sql.append(" AND t.status=?");
            args.add(status.trim().toUpperCase(Locale.ROOT));
        }
        sql.append(" ORDER BY COALESCE(m.preferred_date,CURRENT_DATE) DESC,t.number DESC");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    @Transactional
    public ServiceOrderResponse createOrderFromRequest(String serviceRequestId) {
        String requestId = text(serviceRequestId, "Service request is required");
        ServiceOrderResponse created = serviceOrderService.createFromServiceRequest(requestId, actor(), false);
        Map<String, Object> metadata = requestMetadata(requestId);
        if (metadata.isEmpty()) return created;

        ServiceOrderRequest update = new ServiceOrderRequest();
        update.setStatus("READY");
        update.setServiceLocationId(Objects.toString(metadata.get("service_location_id"), null));
        if (StringUtils.hasText(update.getServiceLocationId())) {
            Map<String, Object> location = row("SELECT * FROM service_location WHERE id=?", update.getServiceLocationId());
            update.setLocation(locationText(location));
        }
        String productId = Objects.toString(metadata.get("product_id"), null);
        if (StringUtils.hasText(productId)) {
            update.setLines(List.of(productLine(productId)));
        }
        return serviceOrderService.update(created.getId(), update, actor());
    }

    @Transactional
    public Map<String, Object> createContractFromRequest(String serviceRequestId) {
        String requestId = text(serviceRequestId, "Service request is required");
        Map<String, Object> request = requests("ALL").stream()
                .filter(row -> requestId.equals(Objects.toString(row.get("id"))))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Service request not found"));
        String customer = Objects.toString(request.get("customer_partner_id"), null);
        String product = Objects.toString(request.get("product_id"), null);
        require(StringUtils.hasText(customer), "The service request must have a customer");
        require(StringUtils.hasText(product), "The service request must have a service product");

        LocalDate start = date(request.get("preferred_date"));
        if (start == null) start = LocalDate.now();
        LocalTime preferredTime = time(request.get("preferred_start_time"));
        if (preferredTime == null) preferredTime = LocalTime.of(8, 0);
        String frequency = Objects.toString(request.get("recurrence_frequency"), "WEEKLY").toUpperCase(Locale.ROOT);
        if (!FREQUENCIES.contains(frequency)) frequency = "WEEKLY";
        int interval = Math.max(1, number(request.get("recurrence_interval"), 1));

        ServiceManagementDtos.ContractRequest contract = new ServiceManagementDtos.ContractRequest();
        contract.setCustomerPartnerId(customer);
        contract.setServiceLocationId(Objects.toString(request.get("service_location_id"), null));
        contract.setStatus("DRAFT");
        contract.setStartDate(start);
        contract.setBillingFrequency("MONTHLY");
        contract.setBillingTiming("ARREARS");
        contract.setBillingMode("FIXED_PERIODIC");
        contract.setCurrency("ZAR");
        contract.setNotes("Created from service request " + Objects.toString(request.get("number"), requestId));

        ServiceManagementDtos.ContractLineRequest line = new ServiceManagementDtos.ContractLineRequest();
        ServiceOrderLineRequest productLine = productLine(product);
        line.setProductId(product);
        line.setDescription(productLine.getDescription());
        line.setQuantity(1.0);
        line.setUnitPriceCents(productLine.getUnitPriceCents());
        contract.setLines(List.of(line));

        ServiceManagementDtos.ContractScheduleRequest schedule = new ServiceManagementDtos.ContractScheduleRequest();
        schedule.setProductId(product);
        schedule.setFrequency(frequency);
        schedule.setIntervalCount(interval);
        schedule.setPreferredStartTime(preferredTime);
        schedule.setDurationMinutes(productDuration(product));
        schedule.setGenerationHorizonDays(60);
        schedule.setNextGenerationDate(start);
        schedule.setActive(true);
        if ("WEEKLY".equals(frequency)) schedule.setDayOfWeek(start.getDayOfWeek().getValue());
        if ("MONTHLY".equals(frequency)) schedule.setDayOfMonth(start.getDayOfMonth());
        contract.setSchedules(List.of(schedule));
        return saveContract(contract);
    }

    @Transactional
    public Map<String, Object> reserveResources(String appointmentId, String productId, LocalDate date, LocalTime start, int durationMinutes) {
        Allocation allocation = allocateResources(text(productId, "Product is required"), Objects.requireNonNull(date, "Date is required"),
                Objects.requireNonNull(start, "Start time is required"), start.plusMinutes(Math.max(1, durationMinutes)));
        if (!allocation.available()) {
            throw new IllegalStateException("Required service resources are no longer available: " + String.join(", ", allocation.missingRequirements()));
        }
        jdbc.update("DELETE FROM service_appointment_resource WHERE appointment_id=?", text(appointmentId, "Appointment is required"));
        for (AllocatedResource resource : allocation.resources()) {
            jdbc.update("INSERT INTO service_appointment_resource(id,appointment_id,resource_id,quantity,created_by) VALUES(?,?,?,?,?)",
                    UUID.randomUUID().toString(), appointmentId, resource.id(), resource.quantity(), actor());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("appointmentId", appointmentId);
        result.put("resourceCount", allocation.resources().size());
        result.put("employeePartnerId", allocation.firstEmployee());
        return result;
    }

    @Transactional
    public Map<String, Object> saveRequestMetadata(ServiceManagementDtos.RequestMetadataRequest r) {
        require(r != null, "Service request metadata is required");
        String requestId = text(r.getServiceRequestId(), "Service request is required");
        jdbc.update("""
            INSERT INTO service_request_metadata(service_request_id,product_id,service_location_id,source_channel,external_request_id,
              preferred_date,preferred_start_time,recurring_requested,recurrence_frequency,recurrence_interval)
            VALUES(?,?,?,?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE product_id=VALUES(product_id),service_location_id=VALUES(service_location_id),source_channel=VALUES(source_channel),
              external_request_id=VALUES(external_request_id),preferred_date=VALUES(preferred_date),preferred_start_time=VALUES(preferred_start_time),
              recurring_requested=VALUES(recurring_requested),recurrence_frequency=VALUES(recurrence_frequency),recurrence_interval=VALUES(recurrence_interval)
            """, requestId, trim(r.getProductId()), trim(r.getServiceLocationId()), upper(r.getSourceChannel(), "ERP"), trim(r.getExternalRequestId()),
                r.getPreferredDate(), r.getPreferredStartTime() == null ? null : Time.valueOf(r.getPreferredStartTime()), bool(r.getRecurringRequested(), false),
                trimUpper(r.getRecurrenceFrequency()), r.getRecurrenceInterval());
        return row("SELECT * FROM service_request_metadata WHERE service_request_id=?", requestId);
    }

    public Map<String, Object> requestMetadata(String serviceRequestId) {
        if (!exists("SELECT COUNT(*) FROM service_request_metadata WHERE service_request_id=?", serviceRequestId)) return Map.of();
        return row("SELECT * FROM service_request_metadata WHERE service_request_id=?", serviceRequestId);
    }

    @Transactional
    public Map<String, Object> generateRecurring() {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> schedules = jdbc.queryForList("""
            SELECT s.*,c.customer_partner_id,c.service_location_id,c.status AS contract_status,c.start_date,c.end_date,
                   c.currency,l.name AS location_name,l.address_line1,l.suburb,l.city
              FROM service_contract_schedule s
              JOIN service_contract c ON c.id=s.service_contract_id
              LEFT JOIN service_location l ON l.id=c.service_location_id
             WHERE s.active=1 AND c.status='ACTIVE'
               AND (c.end_date IS NULL OR c.end_date>=?)
            """, today);
        int generated = 0, pending = 0, skipped = 0;
        List<String> failures = new ArrayList<>();
        for (Map<String, Object> s : schedules) {
            try {
                Result counts = generateSchedule(s, today);
                generated += counts.generated; pending += counts.pending; skipped += counts.skipped;
            } catch (Exception ex) {
                failures.add(Objects.toString(s.get("id")) + ": " + safe(ex));
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schedules", schedules.size()); result.put("generated", generated); result.put("pendingResource", pending);
        result.put("skipped", skipped); result.put("failed", failures.size()); result.put("failures", failures);
        return result;
    }

    private Result generateSchedule(Map<String, Object> s, LocalDate today) {
        String scheduleId = Objects.toString(s.get("id"));
        LocalDate contractStart = date(s.get("start_date"));
        LocalDate contractEnd = date(s.get("end_date"));
        LocalDate cursor = date(s.get("next_generation_date"));
        if (cursor == null || cursor.isBefore(contractStart)) cursor = firstOccurrence(s, contractStart);
        if (cursor.isBefore(today)) cursor = advanceUntilOnOrAfter(s, cursor, today);
        int horizon = number(s.get("generation_horizon_days"), 60);
        LocalDate until = today.plusDays(horizon);
        if (contractEnd != null && contractEnd.isBefore(until)) until = contractEnd;
        int generated = 0, pending = 0, skipped = 0;
        while (!cursor.isAfter(until)) {
            if (exists("SELECT COUNT(*) FROM service_contract_occurrence WHERE schedule_id=? AND occurrence_date=?", scheduleId, cursor)) {
                skipped++;
            } else {
                boolean ok = createOccurrence(s, cursor);
                if (ok) generated++; else pending++;
            }
            cursor = next(s, cursor);
        }
        jdbc.update("UPDATE service_contract_schedule SET next_generation_date=?,updated_by=? WHERE id=?", cursor, actor(), scheduleId);
        return new Result(generated, pending, skipped);
    }

    private boolean createOccurrence(Map<String, Object> schedule, LocalDate occurrenceDate) {
        String contractId = Objects.toString(schedule.get("service_contract_id"));
        String productId = Objects.toString(schedule.get("product_id"));
        String customerId = Objects.toString(schedule.get("customer_partner_id"));
        String locationId = Objects.toString(schedule.get("service_location_id"), null);
        LocalTime start = time(schedule.get("preferred_start_time"));
        if (start == null) start = LocalTime.of(8, 0);
        int duration = number(schedule.get("duration_minutes"), 60);
        LocalTime end = start.plusMinutes(duration);
        Allocation allocation = allocateResources(productId, occurrenceDate, start, end);
        String location = locationText(schedule);

        ServiceOrderRequest orderReq = new ServiceOrderRequest();
        orderReq.setCustomerPartnerId(customerId);
        orderReq.setServiceContractId(contractId);
        orderReq.setServiceLocationId(locationId);
        orderReq.setOrderDate(occurrenceDate);
        orderReq.setScheduledStartAt(LocalDateTime.of(occurrenceDate, start));
        orderReq.setScheduledEndAt(LocalDateTime.of(occurrenceDate, end));
        orderReq.setAssignedEmployeePartnerId(allocation.firstEmployee());
        orderReq.setStatus(allocation.available() ? "SCHEDULED" : "READY");
        orderReq.setLocation(location);
        orderReq.setCurrency(Objects.toString(schedule.get("currency"), "ZAR"));
        orderReq.setNotes("Generated from recurring service contract");
        orderReq.setLines(contractLines(contractId, productId, occurrenceDate, start, end, allocation.firstEmployee()));
        ServiceOrderResponse order = serviceOrderService.create(orderReq, actor());

        String occurrenceId = UUID.randomUUID().toString();
        if (!allocation.available()) {
            jdbc.update("INSERT INTO service_contract_occurrence(id,schedule_id,occurrence_date,service_order_id,status) VALUES(?,?,?,?,?)",
                    occurrenceId, schedule.get("id"), occurrenceDate, order.getId(), "PENDING_RESOURCE");
            return false;
        }

        AppointmentRequest appointmentReq = new AppointmentRequest();
        appointmentReq.setCustomerPartnerId(customerId);
        appointmentReq.setEmployeePartnerId(allocation.firstEmployee());
        appointmentReq.setServiceProductId(productId);
        appointmentReq.setServiceLocationId(locationId);
        appointmentReq.setAppointmentDate(occurrenceDate);
        appointmentReq.setStartTime(start);
        appointmentReq.setDurationMinutes(duration);
        appointmentReq.setLocation(location);
        appointmentReq.setStatus("BOOKED");
        appointmentReq.setSourceType("SERVICE_CONTRACT");
        appointmentReq.setSourceId(contractId);
        AppointmentResponse appointment = appointmentService.create(appointmentReq, actor());
        jdbc.update("INSERT INTO appointment_service_order_link(appointment_id,service_order_id,created_by) VALUES(?,?,?)",
                appointment.getId(), order.getId(), actor());
        for (AllocatedResource resource : allocation.resources()) {
            jdbc.update("INSERT INTO service_appointment_resource(id,appointment_id,resource_id,quantity,created_by) VALUES(?,?,?,?,?)",
                    UUID.randomUUID().toString(), appointment.getId(), resource.id(), resource.quantity(), actor());
        }
        jdbc.update("INSERT INTO service_contract_occurrence(id,schedule_id,occurrence_date,service_order_id,appointment_id,status) VALUES(?,?,?,?,?,?)",
                occurrenceId, schedule.get("id"), occurrenceDate, order.getId(), appointment.getId(), "BOOKED");
        return true;
    }

    private List<ServiceOrderLineRequest> contractLines(String contractId, String productId, LocalDate date, LocalTime start, LocalTime end, String employee) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM service_contract_line WHERE service_contract_id=? AND active=1 AND product_id=? ORDER BY sort_order", contractId, productId);
        if (rows.isEmpty()) rows = jdbc.queryForList("SELECT * FROM service_contract_line WHERE service_contract_id=? AND active=1 ORDER BY sort_order", contractId);
        List<ServiceOrderLineRequest> result = new ArrayList<>();
        for (Map<String, Object> line : rows) {
            ServiceOrderLineRequest dto = new ServiceOrderLineRequest();
            dto.setProductId(Objects.toString(line.get("product_id"), null)); dto.setItemType("SERVICE");
            dto.setDescription(Objects.toString(line.get("description"), "Service"));
            Object q=line.get("quantity"); dto.setQuantity(q instanceof Number n ? n.doubleValue() : 1d);
            dto.setUnitPriceCents(longNumber(line.get("unit_price_cents"))); dto.setDiscountCents(longNumber(line.get("discount_cents")));
            dto.setTaxCents(longNumber(line.get("tax_cents"))); dto.setEmployeePartnerId(employee);
            dto.setScheduledStartAt(LocalDateTime.of(date,start)); dto.setScheduledEndAt(LocalDateTime.of(date,end)); dto.setCompletionStatus("NOT_STARTED");
            result.add(dto);
        }
        return result;
    }

    private Allocation allocateResources(String productId, LocalDate date, LocalTime start, LocalTime end) {
        List<Map<String, Object>> reqs = jdbc.queryForList("SELECT * FROM service_resource_requirement WHERE product_id=? ORDER BY mandatory DESC,resource_type", productId);
        List<AllocatedResource> allocated = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        String firstEmployee = null;
        for (Map<String, Object> req : reqs) {
            int needed = number(req.get("quantity"), 1);
            String specific = Objects.toString(req.get("resource_id"), null);
            String type = Objects.toString(req.get("resource_type"));
            List<Map<String, Object>> candidates;
            if (StringUtils.hasText(specific)) {
                candidates = jdbc.queryForList("SELECT * FROM service_resource WHERE id=? AND active=1", specific);
            } else {
                candidates = jdbc.queryForList("SELECT * FROM service_resource WHERE resource_type=? AND active=1 ORDER BY name", type);
            }
            int remaining = needed;
            for (Map<String, Object> resource : candidates) {
                int capacity = number(resource.get("capacity"),1);
                int used = resourceUsed(Objects.toString(resource.get("id")), date, start, end);
                int available = Math.max(0, capacity-used);
                if (available == 0) continue;
                int take = Math.min(remaining, available);
                allocated.add(new AllocatedResource(Objects.toString(resource.get("id")), take, Objects.toString(resource.get("name"), type)));
                if (firstEmployee == null && StringUtils.hasText(Objects.toString(resource.get("employee_partner_id"), null))) {
                    firstEmployee = Objects.toString(resource.get("employee_partner_id"));
                }
                remaining -= take;
                if (remaining == 0) break;
            }
            if (remaining > 0 && asBool(req.get("mandatory"))) missing.add(type + " x" + remaining);
        }
        return new Allocation(missing.isEmpty(), allocated, missing, firstEmployee);
    }

    private int resourceUsed(String resourceId, LocalDate date, LocalTime start, LocalTime end) {
        Integer used = jdbc.queryForObject("""
            SELECT COALESCE(SUM(ar.quantity),0)
              FROM service_appointment_resource ar JOIN appointment a ON a.id=ar.appointment_id
             WHERE ar.resource_id=? AND a.appointment_date=? AND a.status<>'CANCELLED'
               AND a.start_time < ?
               AND COALESCE(a.end_time,ADDTIME(a.start_time,SEC_TO_TIME(COALESCE(a.duration_minutes,30)*60))) > ?
            """, Integer.class, resourceId, date, Time.valueOf(end), Time.valueOf(start));
        return used == null ? 0 : used;
    }

    private void replaceLines(String contractId, List<ServiceManagementDtos.ContractLineRequest> lines) {
        jdbc.update("DELETE FROM service_contract_line WHERE service_contract_id=?", contractId);
        int sort=0;
        for (ServiceManagementDtos.ContractLineRequest l : lines) {
            String product = text(l.getProductId(), "Contract line product is required");
            String description = StringUtils.hasText(l.getDescription()) ? l.getDescription().trim() : product;
            jdbc.update("""
                INSERT INTO service_contract_line(id,service_contract_id,product_id,description,quantity,unit_price_cents,discount_cents,tax_cents,active,sort_order)
                VALUES(?,?,?,?,?,?,?,?,?,?)
                """, UUID.randomUUID().toString(), contractId, product, description, l.getQuantity()==null?1d:l.getQuantity(),
                    l.getUnitPriceCents()==null?0L:l.getUnitPriceCents(), l.getDiscountCents()==null?0L:l.getDiscountCents(),
                    l.getTaxCents()==null?0L:l.getTaxCents(), bool(l.getActive(), true), sort++);
        }
    }

    private void replaceSchedules(String contractId, LocalDate contractStart, List<ServiceManagementDtos.ContractScheduleRequest> schedules) {
        jdbc.update("DELETE FROM service_contract_schedule WHERE service_contract_id=?", contractId);
        for (ServiceManagementDtos.ContractScheduleRequest s : schedules) {
            String frequency = normalize(s.getFrequency(), "FREQUENCY", FREQUENCIES);
            int interval=s.getIntervalCount()==null?1:s.getIntervalCount(); require(interval>0,"Recurrence interval must be greater than zero");
            int horizon=s.getGenerationHorizonDays()==null?60:s.getGenerationHorizonDays(); require(horizon>=7 && horizon<=365,"Generation horizon must be between 7 and 365 days");
            LocalDate next = s.getNextGenerationDate()==null?contractStart:s.getNextGenerationDate();
            jdbc.update("""
                INSERT INTO service_contract_schedule(id,service_contract_id,product_id,frequency,interval_count,day_of_week,day_of_month,preferred_start_time,
                  duration_minutes,generation_horizon_days,next_generation_date,active,created_by,updated_by)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, UUID.randomUUID().toString(), contractId, text(s.getProductId(),"Schedule product is required"), frequency, interval,
                    s.getDayOfWeek(), s.getDayOfMonth(), s.getPreferredStartTime()==null?null:Time.valueOf(s.getPreferredStartTime()),
                    s.getDurationMinutes(), horizon, next, bool(s.getActive(),true), actor(), actor());
        }
    }

    private LocalDate firstOccurrence(Map<String,Object> s, LocalDate start) {
        String f=Objects.toString(s.get("frequency"));
        if ("WEEKLY".equals(f) && s.get("day_of_week")!=null) {
            int dow=number(s.get("day_of_week"), start.getDayOfWeek().getValue());
            LocalDate d=start; while(d.getDayOfWeek().getValue()!=dow)d=d.plusDays(1); return d;
        }
        if ("MONTHLY".equals(f) && s.get("day_of_month")!=null) {
            int dom=number(s.get("day_of_month"),start.getDayOfMonth());
            LocalDate d=start.withDayOfMonth(Math.min(dom,start.lengthOfMonth())); if(d.isBefore(start)){d=d.plusMonths(1);d=d.withDayOfMonth(Math.min(dom,d.lengthOfMonth()));} return d;
        }
        return start;
    }
    private LocalDate advanceUntilOnOrAfter(Map<String,Object>s,LocalDate d,LocalDate min){while(d.isBefore(min))d=next(s,d);return d;}
    private LocalDate next(Map<String,Object>s,LocalDate d){int i=number(s.get("interval_count"),1);String f=Objects.toString(s.get("frequency"));if("DAILY".equals(f))return d.plusDays(i);if("WEEKLY".equals(f))return d.plusWeeks(i);int dom=number(s.get("day_of_month"),d.getDayOfMonth());LocalDate n=d.plusMonths(i);return n.withDayOfMonth(Math.min(dom,n.lengthOfMonth()));}
    private ServiceOrderLineRequest productLine(String productId) {
        Map<String, Object> product = row("""
            SELECT p.id,p.description,COALESCE((SELECT pp.value FROM product_pricing pp WHERE pp.product=p.id AND pp.pricing='SELLING-PRICE' LIMIT 1),0) AS selling_price
              FROM product p WHERE p.id=?
            """, productId);
        ServiceOrderLineRequest line = new ServiceOrderLineRequest();
        line.setProductId(productId);
        line.setItemType("SERVICE");
        line.setDescription(Objects.toString(product.get("description"), "Service"));
        line.setQuantity(1.0);
        BigDecimal price = product.get("selling_price") instanceof BigDecimal bd ? bd : new BigDecimal(Objects.toString(product.get("selling_price"), "0"));
        line.setUnitPriceCents(price.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue());
        return line;
    }

    private int productDuration(String productId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT pa.value
              FROM product_attribute pa
             WHERE pa.product=? AND UPPER(pa.attribute) IN ('DURATION','DURATION-MINUTES','DURATION_MINUTES')
             ORDER BY CASE UPPER(pa.attribute) WHEN 'DURATION-MINUTES' THEN 1 WHEN 'DURATION_MINUTES' THEN 1 ELSE 2 END
             LIMIT 1
            """, productId);
        return rows.isEmpty() ? 60 : Math.max(1, number(rows.get(0).get("value"), 60));
    }

    private String locationText(Map<String,Object> row){return joinNonBlank(Objects.toString(row.get("name"),Objects.toString(row.get("location_name"),null)),Objects.toString(row.get("address_line1"),null),Objects.toString(row.get("suburb"),null),Objects.toString(row.get("city"),null));}
    private String joinNonBlank(String...v){List<String>x=new ArrayList<>();for(String s:v)if(StringUtils.hasText(s))x.add(s.trim());return String.join(", ",x);}
    private long count(String sql){Long n=jdbc.queryForObject(sql,Long.class);return n==null?0:n;}
    private boolean exists(String sql,Object...args){Integer n=jdbc.queryForObject(sql,Integer.class,args);return n!=null&&n>0;}
    private Map<String,Object> row(String sql,Object...args){List<Map<String,Object>> rows=jdbc.queryForList(sql,args);if(rows.isEmpty())throw new IllegalArgumentException("Record not found");return rows.get(0);}
    private String actor(){if(StringUtils.hasText(UserContext.getCurrentUserId()))return UserContext.getCurrentUserId();if(StringUtils.hasText(UserContext.getCurrentUser()))return UserContext.getCurrentUser();return "SYSTEM";}
    private String text(String s,String m){if(!StringUtils.hasText(s))throw new IllegalArgumentException(m);return s.trim();}
    private String trim(String s){return StringUtils.hasText(s)?s.trim():null;}
    private String trimUpper(String s){return StringUtils.hasText(s)?s.trim().toUpperCase(Locale.ROOT):null;}
    private String upper(String s,String def){return StringUtils.hasText(s)?s.trim().toUpperCase(Locale.ROOT):def;}
    private String normalize(String s,String label,Set<String> allowed){String v=text(s,label+" is required").toUpperCase(Locale.ROOT).replace('-','_');if(!allowed.contains(v))throw new IllegalArgumentException("Unsupported "+label.toLowerCase(Locale.ROOT)+": "+s);return v;}
    private int bool(Boolean b,boolean def){return b==null?(def?1:0):(b?1:0);}
    private boolean asBool(Object v){return v instanceof Boolean b?b:v instanceof Number n?n.intValue()!=0:"1".equals(Objects.toString(v))||"true".equalsIgnoreCase(Objects.toString(v));}
    private int number(Object v,int def){if(v instanceof Number n)return n.intValue();try{return v==null?def:Integer.parseInt(v.toString());}catch(Exception e){return def;}}
    private long longNumber(Object v){if(v instanceof Number n)return n.longValue();try{return v==null?0L:Long.parseLong(v.toString());}catch(Exception e){return 0L;}}
    private LocalDate date(Object v){if(v instanceof java.sql.Date d)return d.toLocalDate();if(v instanceof LocalDate d)return d;return v==null?null:LocalDate.parse(v.toString());}
    private LocalTime time(Object v){if(v instanceof Time t)return t.toLocalTime();if(v instanceof LocalTime t)return t;if(v==null)return null;String s=v.toString();if(s.length()>8)s=s.substring(0,8);return LocalTime.parse(s);}
    private void require(boolean ok,String m){if(!ok)throw new IllegalArgumentException(m);}
    private String safe(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}

    private record AllocatedResource(String id,int quantity,String name) {}
    private record Allocation(boolean available,List<AllocatedResource> resources,List<String> missingRequirements,String firstEmployee) {}
    private record Result(int generated,int pending,int skipped) {}
}
