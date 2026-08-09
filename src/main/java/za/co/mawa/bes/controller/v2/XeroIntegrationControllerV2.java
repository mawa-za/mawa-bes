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
import za.co.mawa.bes.dto.v2.integration.XeroSelectTenantRequestDto;
import za.co.mawa.bes.service.v2.integration.XeroActivationService;

import java.util.Map;

@RestController
@RequestMapping("/v2/integrations/xero")
public class XeroIntegrationControllerV2 {

    private final XeroActivationService xeroActivationService;

    public XeroIntegrationControllerV2(XeroActivationService xeroActivationService) {
        this.xeroActivationService = xeroActivationService;
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activate(@RequestBody XeroActivationRequestDto request) {
        return ResponseEntity.ok(xeroActivationService.activate(request));
    }

    @GetMapping("/secret-names")
    public ResponseEntity<?> secretNames() {
        return ResponseEntity.ok(xeroActivationService.secretNames());
    }

    @PostMapping("/deactivate")
    public ResponseEntity<?> deactivate() {
        return ResponseEntity.ok(xeroActivationService.deactivate());
    }

    @GetMapping("/connections")
    public ResponseEntity<?> connections() {
        return ResponseEntity.ok(xeroActivationService.connections());
    }

    @PostMapping("/select-tenant")
    public ResponseEntity<?> selectTenant(@RequestBody XeroSelectTenantRequestDto request) {
        return ResponseEntity.ok(xeroActivationService.selectTenant(request));
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
