package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.tombstone.TombstoneDtos;
import za.co.mawa.bes.service.v2.tombstone.TombstoneManagementService;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping(value = "/v2/tombstones", produces = MediaType.APPLICATION_JSON_VALUE)
public class TombstoneManagementControllerV2 {

    private final TombstoneManagementService service;

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() { return execute(service::getDashboard); }

    @GetMapping("/orders")
    public ResponseEntity<?> orders(@RequestParam(required = false) String status,
                                    @RequestParam(required = false) String fundingStatus,
                                    @RequestParam(required = false) String query) {
        return execute(() -> service.getOrders(status, fundingStatus, query));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> order(@PathVariable String id) { return execute(() -> service.getOrder(id)); }

    @PostMapping(value = "/orders", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createOrder(@RequestBody TombstoneDtos.CreateOrderRequest request,
                                         @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return executeCreated(() -> service.createOrder(request, userId));
    }

    @PutMapping(value = "/orders/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateOrder(@PathVariable String id,
                                         @RequestBody TombstoneDtos.UpdateOrderRequest request,
                                         @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return execute(() -> service.updateOrder(id, request, userId));
    }

    @PostMapping(value = "/orders/{id}/funding", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addFunding(@PathVariable String id,
                                        @RequestBody TombstoneDtos.FundingAllocationRequest request,
                                        @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return execute(() -> service.addFunding(id, request, userId));
    }

    @PostMapping(value = "/orders/{id}/layby", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createLayby(@PathVariable String id,
                                         @RequestBody TombstoneDtos.LaybyAgreementRequest request,
                                         @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return executeCreated(() -> service.createLayby(id, request, userId));
    }

    @PostMapping(value = "/laybys/{id}/payments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> laybyPayment(@PathVariable String id,
                                          @RequestBody TombstoneDtos.LaybyPaymentRequest request,
                                          @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return execute(() -> service.recordLaybyPayment(id, request, userId));
    }

    @GetMapping("/laybys")
    public ResponseEntity<?> laybys(@RequestParam(required = false) String status) {
        return execute(() -> service.getLaybyAgreements(status));
    }

    @PostMapping(value = "/orders/{id}/site-assessments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addAssessment(@PathVariable String id,
                                           @RequestBody TombstoneDtos.SiteAssessmentRequest request,
                                           @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return executeCreated(() -> service.addSiteAssessment(id, request, userId));
    }

    @GetMapping("/site-assessments")
    public ResponseEntity<?> assessments(@RequestParam(required = false) String status) {
        return execute(() -> service.getAssessments(status));
    }

    @PostMapping(value = "/orders/{id}/amendments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createAmendment(@PathVariable String id,
                                             @RequestBody TombstoneDtos.AmendmentRequest request,
                                             @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return executeCreated(() -> service.createAmendment(id, request, userId));
    }

    @PutMapping(value = "/amendments/{id}/decision", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> decideAmendment(@PathVariable String id,
                                             @RequestBody TombstoneDtos.AmendmentDecisionRequest request,
                                             @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return execute(() -> service.decideAmendment(id, request, userId));
    }

    @PostMapping(value = "/orders/{id}/designs", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addDesign(@PathVariable String id,
                                       @RequestBody TombstoneDtos.DesignRequest request,
                                       @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return executeCreated(() -> service.addDesign(id, request, userId));
    }

    @PutMapping(value = "/designs/{id}/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> approveDesign(@PathVariable String id,
                                           @RequestBody TombstoneDtos.DesignApprovalRequest request,
                                           @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return execute(() -> service.approveDesign(id, request, userId));
    }

    @GetMapping("/designs")
    public ResponseEntity<?> designs(@RequestParam(required = false) String status) {
        return execute(() -> service.getDesigns(status));
    }

    @PostMapping(value = "/orders/{id}/production-jobs", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createProduction(@PathVariable String id,
                                              @RequestBody TombstoneDtos.ProductionJobRequest request,
                                              @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return executeCreated(() -> service.createProductionJob(id, request, userId));
    }

    @PutMapping(value = "/production-jobs/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> productionStatus(@PathVariable String id,
                                              @RequestBody TombstoneDtos.StatusUpdateRequest request,
                                              @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return execute(() -> service.updateProductionStatus(id, request, userId));
    }

    @PostMapping(value = "/production-jobs/{id}/supplier-payment-request", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> supplierPayment(@PathVariable String id,
                                             @RequestBody TombstoneDtos.SupplierPaymentRequest request,
                                             @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return executeCreated(() -> service.createSupplierPaymentRequest(id, request, userId));
    }

    @GetMapping("/production-jobs")
    public ResponseEntity<?> productionJobs(@RequestParam(required = false) String status) {
        return execute(() -> service.getProductionJobs(status));
    }

    @PostMapping(value = "/orders/{id}/installations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createInstallation(@PathVariable String id,
                                                @RequestBody TombstoneDtos.InstallationRequest request,
                                                @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return executeCreated(() -> service.createInstallation(id, request, userId));
    }

    @PutMapping(value = "/installations/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> installationStatus(@PathVariable String id,
                                                @RequestBody TombstoneDtos.StatusUpdateRequest request,
                                                @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return execute(() -> service.updateInstallationStatus(id, request, userId));
    }

    @PutMapping(value = "/installations/{installationId}/checklist/{checklistId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> checklist(@PathVariable String installationId,
                                       @PathVariable String checklistId,
                                       @RequestBody TombstoneDtos.ChecklistUpdateRequest request,
                                       @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return execute(() -> service.updateChecklist(installationId, checklistId, request, userId));
    }

    @PostMapping(value = "/installations/{id}/complete", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> completeInstallation(@PathVariable String id,
                                                  @RequestBody TombstoneDtos.InstallationCompletionRequest request,
                                                  @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return execute(() -> service.completeInstallation(id, request, userId));
    }

    @PostMapping(value = "/installations/{id}/accept", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> acceptInstallation(@PathVariable String id,
                                                @RequestBody TombstoneDtos.AcceptanceRequest request,
                                                @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return execute(() -> service.acceptInstallation(id, request, userId));
    }

    @PostMapping(value = "/installations/{id}/rework", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> rework(@PathVariable String id,
                                    @RequestBody TombstoneDtos.ReworkRequest request,
                                    @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return executeCreated(() -> service.createRework(id, request, userId));
    }

    @GetMapping("/installations")
    public ResponseEntity<?> installations(@RequestParam(required = false) String status) {
        return execute(() -> service.getInstallations(status));
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable String id,
                                    @RequestParam String reason,
                                    @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return execute(() -> service.cancelOrder(id, reason, userId));
    }

    private ResponseEntity<?> execute(Action action) {
        try { return ResponseEntity.ok(action.run()); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(e.getMessage()); }
        catch (IllegalStateException e) { return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()); }
    }

    private ResponseEntity<?> executeCreated(Action action) {
        try { return ResponseEntity.status(HttpStatus.CREATED).body(action.run()); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(e.getMessage()); }
        catch (IllegalStateException e) { return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()); }
    }

    @FunctionalInterface private interface Action { Object run() throws Exception; }
}
