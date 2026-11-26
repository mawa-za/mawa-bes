package za.co.mawa.bes.controller.v2;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.dto.v2.integration.XeroActivationRequestDto;
import za.co.mawa.bes.dto.v2.integration.XeroInvoiceIntegrationRequestDto;
import za.co.mawa.bes.dto.v2.integration.XeroSelectTenantRequestDto;
import za.co.mawa.bes.service.v2.integration.XeroActivationService;

import java.util.Map;
import java.io.IOException;
import org.springframework.web.bind.annotation.PathVariable;
import za.co.mawa.bes.xero.XeroReconciliationOperationsService;
import za.co.mawa.bes.xero.XeroAccountingConfigurationService;

@RestController
@RequestMapping("/v2/integrations/xero")
public class XeroIntegrationControllerV2 {

    private final XeroActivationService xeroActivationService;
    private final XeroReconciliationOperationsService reconciliationOperations;
    private final XeroAccountingConfigurationService accountingConfiguration;

    public XeroIntegrationControllerV2(XeroActivationService xeroActivationService,
                                      XeroReconciliationOperationsService reconciliationOperations,
                                      XeroAccountingConfigurationService accountingConfiguration) {
        this.xeroActivationService = xeroActivationService;
        this.reconciliationOperations = reconciliationOperations;
        this.accountingConfiguration = accountingConfiguration;
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activate(@RequestBody XeroActivationRequestDto request) {
        accountingConfiguration.requireConfigurationWriteAccess();
        return ResponseEntity.ok(xeroActivationService.activate(request));
    }

    @GetMapping("/secret-names")
    public ResponseEntity<?> secretNames() {
        return ResponseEntity.ok(xeroActivationService.secretNames());
    }

    @PostMapping("/deactivate")
    public ResponseEntity<?> deactivate() {
        accountingConfiguration.requireConfigurationAccess();
        return ResponseEntity.ok(xeroActivationService.deactivate());
    }

    @PostMapping("/invoice-integration")
    public ResponseEntity<?> updateInvoiceIntegration(@RequestBody XeroInvoiceIntegrationRequestDto request) {
        accountingConfiguration.requireConfigurationWriteAccess();
        return ResponseEntity.ok(xeroActivationService.updateInvoiceIntegration(request));
    }

    @GetMapping("/connections")
    public ResponseEntity<?> connections() {
        return ResponseEntity.ok(xeroActivationService.connections());
    }

    @PostMapping("/select-tenant")
    public ResponseEntity<?> selectTenant(@RequestBody XeroSelectTenantRequestDto request) {
        accountingConfiguration.requireConfigurationWriteAccess();
        return ResponseEntity.ok(xeroActivationService.selectTenant(request));
    }

    @GetMapping("/accounting-configuration")
    public XeroAccountingConfigurationService.Configuration accountingConfiguration() {
        return accountingConfiguration.read();
    }

    @PostMapping("/accounting-configuration")
    public XeroAccountingConfigurationService.Configuration saveAccountingConfiguration(
            @RequestBody XeroAccountingConfigurationService.SaveRequest request) {
        return accountingConfiguration.save(request);
    }

    /** Local MAWA inventory only; does not query Xero or mutate reconciliation state. */
    @GetMapping("/reconciliation/read-only-assessment")
    public ResponseEntity<?> readOnlyAssessment() {
        return ResponseEntity.ok(reconciliationOperations.readOnlyAssessment());
    }

    @GetMapping("/reconciliation/capability")
    public ResponseEntity<?> reconciliationCapability() {
        accountingConfiguration.requireConfigurationAccess();
        return ResponseEntity.ok(java.util.Map.of("canCheck", reconciliationOperations.canCheck()));
    }

    @GetMapping("/reconciliation/pending")
    public ResponseEntity<?> pendingReconciliation() {
        return ResponseEntity.ok(reconciliationOperations.pending());
    }

    @PostMapping("/reconciliation/{type}/{id}/check")
    public ResponseEntity<?> checkReconciliation(@PathVariable String type, @PathVariable String id)
            throws IOException {
        return ResponseEntity.ok(reconciliationOperations.check(type, id));
    }

    @GetMapping("/reconciliation/{type}/{id}/audit")
    public ResponseEntity<?> reconciliationAudit(@PathVariable String type, @PathVariable String id) {
        return ResponseEntity.ok(reconciliationOperations.auditHistory(type, id));
    }

    @GetMapping("/reconciliation/{type}/{id}/release-readiness")
    public ResponseEntity<?> reconciliationReleaseReadiness(@PathVariable String type, @PathVariable String id) {
        return ResponseEntity.ok(reconciliationOperations.releaseReadiness(type, id));
    }

    @GetMapping("/reconciliation/invoice/{id}/ledger-snapshot")
    public ResponseEntity<?> invoiceLedgerSnapshot(@PathVariable String id) throws IOException {
        return ResponseEntity.ok(reconciliationOperations.invoiceLedgerSnapshot(id));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of(
                "code", "XERO_BAD_REQUEST",
                "message", safeMessage(exception, "Invalid Xero integration request"),
                "reauthorisationRequired", false
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> integrationState(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "code", "XERO_INTEGRATION_STATE",
                "message", safeMessage(exception, "Xero integration is not ready"),
                "reauthorisationRequired", requiresReauthorisation(exception)
        ));
    }

    private boolean requiresReauthorisation(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalised = message.toLowerCase();
                if (normalised.contains("invalid_grant")
                        || normalised.contains("refresh token")
                        || normalised.contains("reconnect xero")
                        || normalised.contains("authorisation has expired")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private String safeMessage(Throwable error, String fallback) {
        return error == null || error.getMessage() == null || error.getMessage().isBlank()
                ? fallback
                : error.getMessage();
    }
}
