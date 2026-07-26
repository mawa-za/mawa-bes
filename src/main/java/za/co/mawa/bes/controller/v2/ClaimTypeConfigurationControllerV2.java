package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.v2.ClaimTypeConfigurationService;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/v2/claim-type-configuration")
@RequiredArgsConstructor
public class ClaimTypeConfigurationControllerV2 {
    private final ClaimTypeConfigurationService service;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(@RequestParam(defaultValue = "false") boolean enabledOnly) {
        return ResponseEntity.ok(enabledOnly ? service.enabled() : service.list());
    }

    @PutMapping
    public ResponseEntity<List<Map<String, Object>>> save(
            @RequestBody List<Map<String, Object>> rows,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(service.save(rows, userId));
    }
}
