package za.co.mawa.bes.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.AdminHandoffService;
import za.co.mawa.bes.service.PlatformBillingInvoiceMirrorService;

/** Internal admin path is allowlisted by security; always validate shared secret ourselves. */
@RestController
@RequiredArgsConstructor
public class InternalPlatformBillingController {
    private final AdminHandoffService handoffService;
    private final PlatformBillingInvoiceMirrorService revenueService;

    @PostMapping("/internal/admin/tenant/{tenant}/platform-billing/invoices")
    public ResponseEntity<?> importInvoice(@RequestHeader HttpHeaders headers,
                                            @PathVariable String tenant,
                                            @RequestBody JsonNode invoice) {
        handoffService.validateInternalToken(headers.getFirst("X-Mawa-Internal-Token"));
        return ResponseEntity.ok(revenueService.accept(invoice));
    }

    public record PostRevenueInvoiceRequest(String partnerId, String xeroTaxType) {}

    @PostMapping("/v2/platform-billing/revenue/{sourceId}/post")
    public ResponseEntity<?> postToErp(@PathVariable long sourceId,
                                        @RequestBody PostRevenueInvoiceRequest request) {
        return ResponseEntity.ok(revenueService.postToErp(sourceId, request.partnerId(), request.xeroTaxType()));
    }

    @GetMapping("/v2/platform-billing/revenue")
    public ResponseEntity<?> revenue() {
        return ResponseEntity.ok(revenueService.revenue());
    }
}
