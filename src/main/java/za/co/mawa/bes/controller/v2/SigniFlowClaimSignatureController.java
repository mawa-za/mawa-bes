package za.co.mawa.bes.controller.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.v2.SigniFlowClaimSignatureService;

import java.util.List;
import java.util.Map;

@RestController
public class SigniFlowClaimSignatureController {
    private final SigniFlowClaimSignatureService service;

    public SigniFlowClaimSignatureController(SigniFlowClaimSignatureService service) {
        this.service = service;
    }

    @GetMapping("/v2/signiflow/configuration")
    public ResponseEntity<Map<String, Object>> configuration() {
        return ResponseEntity.ok(service.getConfiguration());
    }

    @PutMapping("/v2/signiflow/configuration")
    public ResponseEntity<Map<String, Object>> saveConfiguration(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(service.saveConfiguration(request, userId));
    }

    @PostMapping("/v2/signiflow/configuration/test")
    public ResponseEntity<Map<String, Object>> testConfiguration() {
        return ResponseEntity.ok(service.testConfiguration());
    }

    @GetMapping("/v2/membership-claim/{claimId}/signiflow/signer-options")
    public ResponseEntity<List<Map<String, Object>>> signerOptions(@PathVariable String claimId) {
        return ResponseEntity.ok(service.signerOptions(claimId));
    }

    @GetMapping("/v2/membership-claim/{claimId}/signiflow")
    public ResponseEntity<List<Map<String, Object>>> workflows(@PathVariable String claimId) {
        return ResponseEntity.ok(service.workflows(claimId));
    }

    @PostMapping("/v2/membership-claim/{claimId}/signiflow/send")
    public ResponseEntity<Map<String, Object>> send(
            @PathVariable String claimId,
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(service.sendClaimForm(claimId,
                request == null ? Map.of() : request, userId));
    }

    @PostMapping("/v2/signiflow/workflows/{workflowId}/refresh")
    public ResponseEntity<Map<String, Object>> refresh(
            @PathVariable String workflowId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(service.refreshWorkflow(workflowId, userId));
    }

    @PostMapping("/v2/signiflow/workflows/{workflowId}/download-signed")
    public ResponseEntity<Map<String, Object>> downloadSigned(
            @PathVariable String workflowId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(service.downloadSignedDocument(workflowId, userId));
    }
}
