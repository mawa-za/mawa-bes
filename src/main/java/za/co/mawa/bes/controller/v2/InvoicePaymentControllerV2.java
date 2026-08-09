package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.invoice.CaptureInvoicePaymentDto;
import za.co.mawa.bes.service.v2.InvoicePaymentService;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@Slf4j
@RequestMapping(value = "/v2/invoice", produces = MediaType.APPLICATION_JSON_VALUE)
public class InvoicePaymentControllerV2 {

    private final InvoicePaymentService invoicePaymentService;

    @PostMapping(value = "/{invoiceId}/payment", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> capturePayment(@PathVariable String invoiceId, @RequestBody CaptureInvoicePaymentDto request) {
        try {
            return ResponseEntity.ok(invoicePaymentService.capturePayment(invoiceId, request));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of(
                    "code", "INVOICE_PAYMENT_INVALID",
                    "message", exception.getMessage()));
        } catch (Exception exception) {
            log.error("Unable to record payment for invoice {}", invoiceId, exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of(
                    "code", "INVOICE_PAYMENT_FAILED",
                    "message", "The invoice payment could not be recorded right now"));
        }
    }
}
