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
@RequestMapping("/v2/reseller-portal")
@CrossOrigin
public class ResellerPortalControllerV2 {
    private final TenantAdminService tenantAdminService;
    private final ObjectMapper objectMapper;

    public ResellerPortalControllerV2(TenantAdminService tenantAdminService, ObjectMapper objectMapper) {
        this.tenantAdminService = tenantAdminService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile() { return execute(() -> tenantAdminService.getResellerProfile(reseller())); }

    @GetMapping("/clients")
    public ResponseEntity<?> clients() { return execute(() -> tenantAdminService.getResellerAssignments(reseller(), actor())); }

    @PostMapping("/clients")
    public ResponseEntity<?> createClient(@RequestBody Map<String, Object> request) {
        Map<String, Object> safeRequest = new LinkedHashMap<>();
        safeRequest.put("name", request.get("name"));
        safeRequest.put("urlPrefix", request.get("urlPrefix"));
        safeRequest.put("supportLevel", request.get("supportLevel"));
        safeRequest.put("billingResponsibility", request.get("billingResponsibility"));
        safeRequest.put("actor", actor());
        return execute(() -> tenantAdminService.provisionResellerTenant(reseller(), safeRequest));
    }

    @PostMapping("/clients/{clientTenantId}/provision/retry")
    public ResponseEntity<?> retryClientProvisioning(@PathVariable String clientTenantId) {
        return execute(() -> tenantAdminService.retryResellerTenantProvisioning(
                reseller(), clientTenantId, actor()));
    }

    @GetMapping("/support-sessions")
    public ResponseEntity<?> sessions() { return execute(() -> tenantAdminService.getResellerSupportSessions(reseller(), actor())); }

    @PostMapping("/support-sessions")
    public ResponseEntity<?> startSession(@RequestBody Map<String, Object> request) {
        Map<String, Object> safeRequest = new LinkedHashMap<>(request);
        safeRequest.put("requestedBy", actor());
        return execute(() -> tenantAdminService.startResellerSupportSession(reseller(), safeRequest));
    }

    @PostMapping("/support-sessions/{sessionId}/open")
    public ResponseEntity<?> openSession(@PathVariable String sessionId) {
        return execute(() -> tenantAdminService.openResellerSupportSession(reseller(), sessionId, actor()));
    }

    @PostMapping("/support-sessions/{sessionId}/revoke")
    public ResponseEntity<?> revokeSession(@PathVariable String sessionId) {
        return execute(() -> tenantAdminService.revokeResellerSupportSession(reseller(), sessionId, actor()));
    }

    @GetMapping("/support-tickets")
    public ResponseEntity<?> tickets() {
        return execute(() -> tenantAdminService.getResellerSupportTickets(reseller(), actor()));
    }

    @PostMapping("/support-tickets/{ticketId}/detail")
    public ResponseEntity<?> ticket(@PathVariable String ticketId) {
        return execute(() -> tenantAdminService.getSupportTicket(ticketId, reseller(), actor()));
    }

    @PostMapping("/support-tickets/{ticketId}/comment")
    public ResponseEntity<?> comment(@PathVariable String ticketId, @RequestBody Map<String, String> request) {
        return execute(() -> tenantAdminService.commentOnSupportTicket(ticketId, reseller(), actor(), request.get("message")));
    }

    @PostMapping("/support-tickets/{ticketId}/status")
    public ResponseEntity<?> status(@PathVariable String ticketId, @RequestBody Map<String, String> request) {
        return execute(() -> tenantAdminService.updateSupportTicketStatus(ticketId, reseller(), actor(), request.get("status")));
    }

    @PostMapping("/support-tickets/{ticketId}/assign")
    public ResponseEntity<?> assign(@PathVariable String ticketId, @RequestBody Map<String, String> request) {
        return execute(() -> tenantAdminService.assignSupportTicket(ticketId, reseller(), actor(), request.get("assignedTo")));
    }

    @PostMapping("/support-tickets/{ticketId}/escalate")
    public ResponseEntity<?> escalate(@PathVariable String ticketId, @RequestBody Map<String, String> request) {
        return execute(() -> tenantAdminService.escalateSupportTicket(ticketId, reseller(), actor(), request.get("reason")));
    }

    private String reseller() {
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
            String body = action.run();
            JsonNode json = objectMapper.readTree(body);
            return ResponseEntity.ok(json);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage() == null ? ex.toString() : ex.getMessage()));
        }
    }
    @FunctionalInterface private interface Action { String run(); }
}
