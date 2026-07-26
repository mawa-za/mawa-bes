package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.v2.StorageConfigurationService;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/v2/storage-configuration")
@RequiredArgsConstructor
public class StorageConfigurationControllerV2 {
    private final StorageConfigurationService service;

    @GetMapping("/warehouses")
    public ResponseEntity<List<Map<String, Object>>> warehouses(@RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(service.warehouses(activeOnly));
    }
    @PostMapping("/warehouses")
    public ResponseEntity<Map<String, Object>> saveWarehouse(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.saveWarehouse(body));
    }
    @GetMapping("/locations")
    public ResponseEntity<List<Map<String, Object>>> locations(@RequestParam String warehouseId, @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(service.locations(warehouseId, activeOnly));
    }
    @PostMapping("/locations")
    public ResponseEntity<Map<String, Object>> saveLocation(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.saveLocation(body));
    }
    @GetMapping("/bins")
    public ResponseEntity<List<Map<String, Object>>> bins(@RequestParam String locationId, @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(service.bins(locationId, activeOnly));
    }
    @PostMapping("/bins")
    public ResponseEntity<Map<String, Object>> saveBin(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.saveBin(body));
    }
}
