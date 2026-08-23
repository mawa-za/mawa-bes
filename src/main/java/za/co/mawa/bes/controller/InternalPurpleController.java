package za.co.mawa.bes.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.v2.purple.PurpleDtos;
import za.co.mawa.bes.service.AdminHandoffService;
import za.co.mawa.bes.service.v2.PurpleTenantService;

import java.util.LinkedHashMap;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/internal/admin/tenant/{tenant}/purple")
public class InternalPurpleController {
    private static final String INTERNAL_TOKEN_HEADER = "X-Mawa-Internal-Token";
    private final AdminHandoffService adminHandoffService;
    private final PurpleTenantService purpleService;

    public InternalPurpleController(AdminHandoffService adminHandoffService, PurpleTenantService purpleService) {
        this.adminHandoffService = adminHandoffService;
        this.purpleService = purpleService;
    }

    @PostMapping("/catalog")
    public ResponseEntity<?> catalog(@RequestHeader HttpHeaders headers, @PathVariable String tenant) {
        return execute(headers, tenant, purpleService::catalog, HttpStatus.OK);
    }

    @PostMapping("/availability")
    public ResponseEntity<?> availability(@RequestHeader HttpHeaders headers, @PathVariable String tenant,
                                          @RequestBody PurpleDtos.AvailabilityRequest request) {
        return execute(headers, tenant, () -> purpleService.availability(request), HttpStatus.OK);
    }

    @PostMapping("/bookings")
    public ResponseEntity<?> booking(@RequestHeader HttpHeaders headers, @PathVariable String tenant,
                                     @RequestBody PurpleDtos.BookingRequest request) {
        return execute(headers, tenant, () -> purpleService.createBooking(request), HttpStatus.CREATED);
    }

    @PostMapping("/service-requests")
    public ResponseEntity<?> serviceRequest(@RequestHeader HttpHeaders headers, @PathVariable String tenant,
                                            @RequestBody PurpleDtos.ServiceRequestCreate request) {
        return execute(headers, tenant, () -> purpleService.createServiceRequest(request), HttpStatus.CREATED);
    }

    @PostMapping("/customer/locations")
    public ResponseEntity<?> locations(@RequestHeader HttpHeaders headers, @PathVariable String tenant,
                                       @RequestBody PurpleDtos.CustomerRequest request) {
        return execute(headers, tenant, () -> purpleService.customerLocations(request), HttpStatus.OK);
    }

    @PostMapping("/customer/locations/save")
    public ResponseEntity<?> saveLocation(@RequestHeader HttpHeaders headers, @PathVariable String tenant,
                                          @RequestBody PurpleDtos.ServiceLocationRequest request) {
        return execute(headers, tenant, () -> purpleService.saveCustomerLocation(request), HttpStatus.OK);
    }

    @PostMapping("/customer/contracts")
    public ResponseEntity<?> contracts(@RequestHeader HttpHeaders headers, @PathVariable String tenant,
                                       @RequestBody PurpleDtos.CustomerRequest request) {
        return execute(headers, tenant, () -> purpleService.customerContracts(request), HttpStatus.OK);
    }

    @PostMapping("/customer/bookings")
    public ResponseEntity<?> bookings(@RequestHeader HttpHeaders headers, @PathVariable String tenant,
                                      @RequestBody PurpleDtos.CustomerRequest request) {
        return execute(headers, tenant, () -> purpleService.customerBookings(request), HttpStatus.OK);
    }

    @PostMapping("/customer/service-requests")
    public ResponseEntity<?> serviceRequests(@RequestHeader HttpHeaders headers, @PathVariable String tenant,
                                             @RequestBody PurpleDtos.CustomerRequest request) {
        return execute(headers, tenant, () -> purpleService.customerServiceRequests(request), HttpStatus.OK);
    }

    @PostMapping("/customer/quotes")
    public ResponseEntity<?> quotes(@RequestHeader HttpHeaders headers, @PathVariable String tenant,
                                    @RequestBody PurpleDtos.CustomerRequest request) {
        return execute(headers, tenant, () -> purpleService.customerQuotes(request), HttpStatus.OK);
    }

    @PostMapping("/customer/invoices")
    public ResponseEntity<?> invoices(@RequestHeader HttpHeaders headers, @PathVariable String tenant,
                                      @RequestBody PurpleDtos.CustomerRequest request) {
        return execute(headers, tenant, () -> purpleService.customerInvoices(request), HttpStatus.OK);
    }

    private ResponseEntity<?> execute(HttpHeaders headers, String tenant, Action action, HttpStatus status) {
        try {
            adminHandoffService.validateInternalToken(headers.getFirst(INTERNAL_TOKEN_HEADER));
            TenantContext.setCurrentTenant(tenant);
            return ResponseEntity.status(status).body(action.run());
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", ex.getMessage() == null ? ex.toString() : ex.getMessage());
            return ResponseEntity.badRequest().body(body);
        }
    }

    @FunctionalInterface
    private interface Action { Object run() throws Exception; }
}
