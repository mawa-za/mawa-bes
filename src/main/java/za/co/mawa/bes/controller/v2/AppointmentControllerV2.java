package za.co.mawa.bes.controller.v2;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.appointment.AppointmentRequest;
import za.co.mawa.bes.dto.v2.appointment.AppointmentStatusUpdateRequest;
import za.co.mawa.bes.service.v2.AppointmentService;
import za.co.mawa.bes.service.v2.ServiceOrderService;
import za.co.mawa.bes.service.InvoiceService;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/v2/appointment")
public class AppointmentControllerV2 {

    private final AppointmentService appointmentService;
    private final ServiceOrderService serviceOrderService;
    private final InvoiceService invoiceService;

    public AppointmentControllerV2(
            AppointmentService appointmentService,
            ServiceOrderService serviceOrderService,
            InvoiceService invoiceService
    ) {
        this.appointmentService = appointmentService;
        this.serviceOrderService = serviceOrderService;
        this.invoiceService = invoiceService;
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody AppointmentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String currentUser
    ) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.create(request, currentUser));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @GetMapping
    public ResponseEntity<?> getAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String status
    ) {
        try {
            return ResponseEntity.ok(appointmentService.search(bookDate, fromDate, toDate, employeeId, customerId, status));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @GetMapping("/calendar")
    public ResponseEntity<?> getCalendarAppointments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String status
    ) {
        return getAppointments(null, fromDate, toDate, employeeId, null, status);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(appointmentService.get(id));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable String id,
            @RequestBody AppointmentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String currentUser
    ) {
        try {
            return ResponseEntity.ok(appointmentService.update(id, request, currentUser));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable String id,
            @RequestBody AppointmentStatusUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String currentUser
    ) {
        try {
            return ResponseEntity.ok(appointmentService.updateStatus(id, request == null ? null : request.getStatus(), request == null ? null : request.getReason(), currentUser));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancel(
            @PathVariable String id,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-User-Id", required = false) String currentUser
    ) {
        try {
            return ResponseEntity.ok(appointmentService.cancel(id, reason, currentUser));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @PostMapping("/{id}/invoice")
    public ResponseEntity<?> invoiceAppointment(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String currentUser
    ) {
        try {
            var serviceOrder = serviceOrderService.createFromAppointment(id, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(invoiceService.mapToDto(serviceOrderService.createInvoice(serviceOrder.getId(), currentUser)));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> history(@PathVariable String id) {
        try {
            return ResponseEntity.ok(appointmentService.history(id));
        } catch (Exception e) {
            return badRequest(e);
        }
    }

    private ResponseEntity<Map<String, Object>> badRequest(Exception e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", e.getMessage() == null ? e.toString() : e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
