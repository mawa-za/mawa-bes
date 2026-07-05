package za.co.mawa.bes.controller.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.dto.v2.integration.XeroActivationRequestDto;
import za.co.mawa.bes.dto.v2.integration.XeroSelectTenantRequestDto;
import za.co.mawa.bes.service.v2.integration.XeroActivationService;

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
}
