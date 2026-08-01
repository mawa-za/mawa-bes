package za.co.mawa.bes.controller.v2;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.serviceorder.ServiceOrderRequest;
import za.co.mawa.bes.service.InvoiceService;
import za.co.mawa.bes.service.v2.ServiceOrderService;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/v2/service-orders")
public class ServiceOrderControllerV2 {
    private final ServiceOrderService serviceOrderService;
    private final InvoiceService invoiceService;

    public ServiceOrderControllerV2(ServiceOrderService serviceOrderService, InvoiceService invoiceService) {
        this.serviceOrderService = serviceOrderService;
        this.invoiceService = invoiceService;
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody ServiceOrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String currentUser
    ) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(serviceOrderService.create(request, currentUser));
        } catch (Exception exception) {
            return badRequest(exception);
        }
    }

    @PostMapping("/from-appointment/{appointmentId}")
    public ResponseEntity<?> createFromAppointment(
            @PathVariable String appointmentId,
            @RequestHeader(value = "X-User-Id", required = false) String currentUser
    ) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(serviceOrderService.createFromAppointment(appointmentId, currentUser));
        } catch (Exception exception) {
            return badRequest(exception);
        }
    }

    @PostMapping("/from-service-request/{serviceRequestId}")
    public ResponseEntity<?> createFromServiceRequest(
            @PathVariable String serviceRequestId,
            @RequestParam(defaultValue = "false") boolean additional,
            @RequestHeader(value = "X-User-Id", required = false) String currentUser
    ) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(serviceOrderService.createFromServiceRequest(serviceRequestId, currentUser, additional));
        } catch (Exception exception) {
            return badRequest(exception);
        }
    }

    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String sourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        try {
            return ResponseEntity.ok(serviceOrderService.search(
                    status, customerId, sourceType, sourceId, fromDate, toDate));
        } catch (Exception exception) {
            return badRequest(exception);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        try { return ResponseEntity.ok(serviceOrderService.get(id)); }
        catch (Exception exception) { return badRequest(exception); }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable String id,
            @RequestBody ServiceOrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String currentUser
    ) {
        try { return ResponseEntity.ok(serviceOrderService.update(id, request, currentUser)); }
        catch (Exception exception) { return badRequest(exception); }
    }

    @PostMapping("/{id}/invoice")
    public ResponseEntity<?> createInvoice(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String currentUser
    ) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(invoiceService.mapToDto(serviceOrderService.createInvoice(id, currentUser)));
        } catch (Exception exception) {
            return badRequest(exception);
        }
    }

    private ResponseEntity<Map<String, Object>> badRequest(Exception exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", exception.getMessage() == null ? "Unable to process service order" : exception.getMessage());
        body.put("error", "SERVICE_ORDER_REQUEST_FAILED");
        return ResponseEntity.badRequest().body(body);
    }
}
