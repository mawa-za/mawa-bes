package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.PaymentBatchResponseDto;
import za.co.mawa.bes.dto.v2.layby.LaybyDtos;
import za.co.mawa.bes.service.v2.LaybyManagementService;
import za.co.mawa.bes.service.v2.LaybyPdfService;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/v2")
@RequiredArgsConstructor
public class LaybyControllerV2 {

    private final LaybyManagementService laybyService;
    private final LaybyPdfService laybyPdfService;

    @GetMapping("/laybys/configuration")
    public ResponseEntity<Map<String, Object>> configuration() {
        return ResponseEntity.ok(laybyService.getConfiguration());
    }

    @PutMapping("/laybys/configuration")
    public ResponseEntity<Map<String, Object>> updateConfiguration(
            @RequestBody LaybyDtos.ConfigurationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(laybyService.updateConfiguration(request, userId));
    }

    @GetMapping("/laybys")
    public ResponseEntity<List<Map<String, Object>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String customerPartnerId) {
        return ResponseEntity.ok(laybyService.list(status, query, customerPartnerId));
    }

    @PostMapping("/laybys")
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody LaybyDtos.CreateLaybyRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(laybyService.create(request, userId));
    }

    @PostMapping("/quotations/{id}/convert-to-layby")
    public ResponseEntity<Map<String, Object>> convertQuotation(
            @PathVariable String id,
            @RequestBody(required = false) LaybyDtos.CreateLaybyRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        LaybyDtos.CreateLaybyRequest payload = request == null ? new LaybyDtos.CreateLaybyRequest() : request;
        payload.setQuotationId(id);
        return ResponseEntity.ok(laybyService.create(payload, userId));
    }

    @GetMapping("/laybys/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String id) {
        return ResponseEntity.ok(laybyService.get(id));
    }

    @GetMapping("/laybys/{id}/agreement-pdf")
    public ResponseEntity<byte[]> agreementPdf(@PathVariable String id) {
        Map<String, Object> layby = laybyService.get(id);
        String filename = String.valueOf(layby.getOrDefault("layby_no", "layby")) + "-agreement.pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(laybyPdfService.agreement(id));
    }

    @GetMapping("/laybys/{id}/statement-pdf")
    public ResponseEntity<byte[]> statementPdf(@PathVariable String id) {
        Map<String, Object> layby = laybyService.get(id);
        String filename = String.valueOf(layby.getOrDefault("layby_no", "layby")) + "-statement.pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(laybyPdfService.statement(id));
    }

    @PostMapping("/laybys/{id}/activate")
    public ResponseEntity<Map<String, Object>> activate(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(laybyService.activate(id, userId));
    }

    @PostMapping("/laybys/{id}/payments")
    public ResponseEntity<PaymentBatchResponseDto> payment(
            @PathVariable String id,
            @RequestBody LaybyDtos.PaymentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(laybyService.capturePayment(id, request, userId));
    }

    @PostMapping("/laybys/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(
            @PathVariable String id,
            @RequestBody LaybyDtos.CancellationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(laybyService.requestCancellation(id, request, userId));
    }

    @PostMapping("/laybys/{id}/refund/request-approval")
    public ResponseEntity<Map<String, Object>> requestRefundApproval(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(laybyService.requestRefundApproval(id, userId));
    }

    @PostMapping("/laybys/{id}/refund/paid")
    public ResponseEntity<Map<String, Object>> refundPaid(
            @PathVariable String id,
            @RequestBody(required = false) LaybyDtos.RefundPaidRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(laybyService.markRefundPaid(id, request, userId));
    }

    @PostMapping("/laybys/{id}/fulfil")
    public ResponseEntity<Map<String, Object>> fulfil(
            @PathVariable String id,
            @RequestBody(required = false) LaybyDtos.FulfilRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(laybyService.fulfil(id, request, userId));
    }
}
