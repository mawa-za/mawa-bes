package za.co.mawa.bes.controller.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.service.TenantAdminService;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/v2/support-tickets")
@CrossOrigin
public class SupportTicketControllerV2 {
    private final TenantAdminService tenantAdminService;
    private final ObjectMapper objectMapper;

    public SupportTicketControllerV2(TenantAdminService tenantAdminService, ObjectMapper objectMapper) {
        this.tenantAdminService = tenantAdminService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<?> clientTickets() {
        return execute(() -> tenantAdminService.getClientSupportTickets(tenant()));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> request) {
        Map<String, Object> safe = new LinkedHashMap<>(request);
        safe.put("actor", actor());
        safe.put("source", "TENANT_PORTAL");
        return execute(() -> tenantAdminService.createClientSupportTicket(tenant(), safe));
    }

    @PostMapping("/{ticketId}/detail")
    public ResponseEntity<?> detail(@PathVariable String ticketId) {
        return execute(() -> tenantAdminService.getSupportTicket(ticketId, tenant(), actor()));
    }

    @PostMapping("/{ticketId}/comment")
    public ResponseEntity<?> comment(@PathVariable String ticketId, @RequestBody Map<String, String> request) {
        return execute(() -> tenantAdminService.commentOnSupportTicket(ticketId, tenant(), actor(), request.get("message")));
    }

    private String tenant() {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null || tenant.isBlank()) throw new IllegalStateException("Tenant context is required");
        return tenant;
    }

    private String actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }

    private ResponseEntity<?> execute(Action action) {
        try {
            JsonNode json = objectMapper.readTree(action.run());
            return ResponseEntity.ok(json);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage() == null ? ex.toString() : ex.getMessage()));
        }
    }
    @FunctionalInterface private interface Action { String run(); }
}
