package za.co.mawa.bes.controller.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.v2.PaymentRequestInvoiceEmailService;

import java.util.Map;

@RestController
@RequestMapping("/v2/payment-request/invoice-email")
public class PaymentRequestInvoiceEmailController {
    private final PaymentRequestInvoiceEmailService service;

    public PaymentRequestInvoiceEmailController(PaymentRequestInvoiceEmailService service) {
        this.service = service;
    }

    @GetMapping("/configuration")
    public ResponseEntity<Map<String, Object>> configuration() {
        return ResponseEntity.ok(service.getConfiguration());
    }

    @PutMapping("/configuration")
    public ResponseEntity<Map<String, Object>> saveConfiguration(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(service.saveConfiguration(request, userId));
    }

    @PostMapping("/backfill")
    public ResponseEntity<Map<String, Object>> backfill(
            @RequestParam(defaultValue = "250") Integer limit,
            @RequestParam(defaultValue = "false") Boolean retryFailed,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(service.runBackfill(limit, retryFailed, userId));
    }

    @PostMapping("/{paymentRequestId}/retry")
    public ResponseEntity<Map<String, Object>> retry(
            @PathVariable String paymentRequestId,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(service.retry(paymentRequestId, userId));
    }
}
