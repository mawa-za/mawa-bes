package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.integration.FnbIntegrationSettingsDto;
import za.co.mawa.bes.service.v2.FnbIntegrationAdministrationService;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/v2/integrations/fnb")
public class FnbIntegrationAdminControllerV2 {
    private final FnbIntegrationAdministrationService service;

    @GetMapping("/settings")
    public ResponseEntity<FnbIntegrationSettingsDto> getSettings() {
        return ResponseEntity.ok(service.getSettings());
    }

    @PutMapping("/settings")
    public ResponseEntity<FnbIntegrationSettingsDto> saveSettings(@RequestBody FnbIntegrationSettingsDto request) {
        return ResponseEntity.ok(service.save(request));
    }

    @PostMapping("/settings")
    public ResponseEntity<FnbIntegrationSettingsDto> createOrUpdateSettings(@RequestBody FnbIntegrationSettingsDto request) {
        return ResponseEntity.ok(service.save(request));
    }
}
