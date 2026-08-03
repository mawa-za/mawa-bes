package za.co.mawa.bes.controller.v2;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.purple.PurpleDtos;
import za.co.mawa.bes.service.v2.PurpleTenantService;

import java.util.LinkedHashMap;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/v2/purple/provider-enrolment")
public class PurpleProviderControllerV2 {
    private final PurpleTenantService purpleService;

    public PurpleProviderControllerV2(PurpleTenantService purpleService) {
        this.purpleService = purpleService;
    }

    @GetMapping
    public ResponseEntity<?> configuration() {
        return execute(() -> purpleService.configuration(), HttpStatus.OK);
    }

    @GetMapping("/products")
    public ResponseEntity<?> products() {
        return execute(purpleService::availableProducts, HttpStatus.OK);
    }

    @PutMapping("/provider")
    public ResponseEntity<?> saveProvider(@RequestBody PurpleDtos.ProviderEnrolmentRequest request) {
        return execute(() -> purpleService.saveProvider(request), HttpStatus.OK);
    }

    @PutMapping("/services")
    public ResponseEntity<?> saveService(@RequestBody PurpleDtos.ServiceEnrolmentRequest request) {
        return execute(() -> purpleService.saveService(request), HttpStatus.OK);
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<?> deleteService(@PathVariable String id) {
        return execute(() -> { purpleService.deleteService(id); return Map.of("success", true); }, HttpStatus.OK);
    }

    @PutMapping("/availability-rules")
    public ResponseEntity<?> saveAvailabilityRule(@RequestBody PurpleDtos.AvailabilityRuleRequest request) {
        return execute(() -> purpleService.saveAvailabilityRule(request), HttpStatus.OK);
    }

    @DeleteMapping("/availability-rules/{id}")
    public ResponseEntity<?> deleteAvailabilityRule(@PathVariable String id) {
        return execute(() -> { purpleService.deleteAvailabilityRule(id); return Map.of("success", true); }, HttpStatus.OK);
    }

    private ResponseEntity<?> execute(Action action, HttpStatus status) {
        try {
            return ResponseEntity.status(status).body(action.run());
        } catch (Exception ex) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", ex.getMessage() == null ? ex.toString() : ex.getMessage());
            return ResponseEntity.badRequest().body(body);
        }
    }

    @FunctionalInterface
    private interface Action { Object run() throws Exception; }
}
