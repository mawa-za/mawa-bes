package za.co.mawa.bes.controller.v2;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.moduleusage.ModuleUsageResponse;
import za.co.mawa.bes.dto.moduleusage.TrackModuleUsageRequest;
import za.co.mawa.bes.service.v2.ModuleUsageService;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/v2/module-usage")
public class ModuleUsageControllerV2 {

    private final ModuleUsageService moduleUsageService;

    public ModuleUsageControllerV2(ModuleUsageService moduleUsageService) {
        this.moduleUsageService = moduleUsageService;
    }

    @PostMapping("/track")
    public ResponseEntity<ModuleUsageResponse> trackUsage(@Valid @RequestBody TrackModuleUsageRequest request) {
        return ResponseEntity.ok(moduleUsageService.trackUsage(request));
    }

    @GetMapping("/frequent")
    public ResponseEntity<List<ModuleUsageResponse>> getFrequentlyUsedModules(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(moduleUsageService.getFrequentlyUsedModules(userId, limit));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<ModuleUsageResponse>> getRecentlyUsedModules(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(moduleUsageService.getRecentlyUsedModules(userId, limit));
    }

    @DeleteMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetUserUsage(@RequestParam(required = false) String userId) {
        moduleUsageService.resetUserUsage(userId);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "User module usage reset successfully"
        ));
    }
}
