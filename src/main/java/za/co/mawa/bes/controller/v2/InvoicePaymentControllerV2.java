package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.funeral.CaptureInvoicePaymentDto;
import za.co.mawa.bes.service.v2.FuneralManagementService;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping(value = "/v2/invoice", produces = MediaType.APPLICATION_JSON_VALUE)
public class InvoicePaymentControllerV2 {

    private final FuneralManagementService funeralManagementService;

    @PostMapping(value = "/{invoiceId}/payment", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> capturePayment(@PathVariable String invoiceId, @RequestBody CaptureInvoicePaymentDto request) {
        try {
            return ResponseEntity.ok(funeralManagementService.captureInvoicePayment(invoiceId, request));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }
}
