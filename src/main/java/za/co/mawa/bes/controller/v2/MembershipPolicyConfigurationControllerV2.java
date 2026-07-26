package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.service.v2.MembershipPolicyConfigurationService;

import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/v2/membership-policy-configuration")
@RequiredArgsConstructor
public class MembershipPolicyConfigurationControllerV2 {
    private final MembershipPolicyConfigurationService service;

    @GetMapping
    public ResponseEntity<Map<String, Object>> get() { return ResponseEntity.ok(service.get()); }

    @PutMapping
    public ResponseEntity<Map<String, Object>> save(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(service.save(request, userId));
    }
}
