package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.AuthenticationResponseDto;
import za.co.mawa.bes.dto.admin.AdminHandoffExchangeRequestDto;
import za.co.mawa.bes.dto.admin.AdminHandoffRequestDto;
import za.co.mawa.bes.dto.admin.InternalAdminResponseDto;
import za.co.mawa.bes.dto.TenantPropertyDto;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.service.AdminHandoffService;
import za.co.mawa.bes.service.AttachmentService;
import za.co.mawa.bes.service.InternalScheduledJobService;
import za.co.mawa.bes.service.TenantAdminService;
import za.co.mawa.bes.service.TenantService;
import za.co.mawa.bes.service.v2.PosPrintingService;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.EnrollmentCreateRequest;
import za.co.mawa.bes.dto.v2.PosPrintingDtos.TerminalEnabledRequest;

import java.util.Iterator;

@RestController
@CrossOrigin
public class InternalAdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalAdminController.class);

    private static final String INTERNAL_TOKEN_HEADER = "X-Mawa-Internal-Token";

    @Autowired
    private AdminHandoffService adminHandoffService;

    @Autowired
    private TenantAdminService tenantAdminService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private InternalScheduledJobService internalScheduledJobService;

    @Autowired
    private PosPrintingService posPrintingService;

    private final Gson gson = new Gson();

    @RequestMapping(value = "/internal/admin/handoff", method = RequestMethod.POST)
    public ResponseEntity<?> createHandoff(@RequestHeader HttpHeaders headers, @RequestBody AdminHandoffRequestDto requestDto) {
        try {
            validateInternalToken(headers);
            return ResponseEntity.ok(gson.toJson(adminHandoffService.createHandoff(requestDto)));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @RequestMapping(value = "/internal/admin/tenant/{tenant}/refresh-config", method = RequestMethod.POST)
    public ResponseEntity<?> refreshTenantConfiguration(@RequestHeader HttpHeaders headers, @PathVariable String tenant) {
        try {
            validateInternalToken(headers);
            int updatedProperties = syncPropertiesFromAdminConsole(tenant);
            return ResponseEntity.ok(gson.toJson(new InternalAdminResponseDto(true, "Tenant configuration refresh accepted. Properties updated: " + updatedProperties, tenant)));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @RequestMapping(value = "/internal/admin/tenant/{tenant}/modules/sync", method = RequestMethod.POST)
    public ResponseEntity<?> syncTenantModules(@RequestHeader HttpHeaders headers, @PathVariable String tenant) {
        try {
            validateInternalToken(headers);
            int updatedProperties = syncPropertiesFromAdminConsole(tenant);
            return ResponseEntity.ok(gson.toJson(new InternalAdminResponseDto(true, "Tenant module sync accepted. Properties updated: " + updatedProperties, tenant)));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @RequestMapping(value = "/internal/admin/tenant/{tenant}/scheduled-jobs/{jobCode}/run", method = RequestMethod.POST)
    public ResponseEntity<?> runScheduledJob(
            @RequestHeader HttpHeaders headers,
            @PathVariable String tenant,
            @PathVariable String jobCode
    ) {
        try {
            validateInternalToken(headers);
            TenantContext.setCurrentTenant(tenant);
            return ResponseEntity.ok(internalScheduledJobService.run(jobCode));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        } catch (Exception ex) {
            LOGGER.error("Scheduled job {} failed for tenant {}", jobCode, tenant, ex);
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("tenant", tenant);
            payload.put("jobCode", jobCode);
            payload.put("success", false);
            payload.put("message", ex.getMessage());
            payload.put("errorType", ex.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(payload);
        }
    }

    @RequestMapping(value = "/internal/admin/tenant/{tenant}/attachments/migrate-to-gcp", method = RequestMethod.POST)
    public ResponseEntity<?> migrateTenantAttachments(
            @RequestHeader HttpHeaders headers,
            @PathVariable String tenant,
            @RequestParam(required = false, defaultValue = "") String afterId,
            @RequestParam(required = false, defaultValue = "25") int limit
    ) {
        try {
            validateInternalToken(headers);

            // InternalTenantContextFilter establishes this before Hibernate can
            // bind an EntityManager. Keep this assignment as a defensive
            // fallback for non-servlet tests and direct method invocation.
            TenantContext.setCurrentTenant(tenant);

            AttachmentService.MigrationResult result =
                    attachmentService.migrateLegacyDatabaseFilesToGcpBatch(afterId, limit);

            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("tenant", tenant);
            payload.put("attempted", result.attempted());
            payload.put("migrated", result.migrated());
            payload.put("failed", result.failed());
            payload.put("remaining", result.remaining());
            payload.put("completed", result.completed());
            payload.put("scanComplete", result.scanComplete());
            payload.put("nextCursor", result.nextCursor());
            payload.put("failures", result.failures());
            payload.put("message", result.completed()
                    ? "Attachment migration completed"
                    : result.scanComplete()
                        ? "Attachment scan completed with legacy records that could not be migrated"
                        : "Attachment migration batch completed");
            return ResponseEntity.ok(gson.toJson(payload));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        } catch (Exception ex) {
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("tenant", tenant);
            payload.put("message", ex.getMessage());
            payload.put("errorType", ex.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(gson.toJson(payload));
        }
    }

    @RequestMapping(value = "/internal/admin/tenant/{tenant}/pos-printing/summary", method = RequestMethod.POST)
    public ResponseEntity<?> getPosPrintingSummary(@RequestHeader HttpHeaders headers, @PathVariable String tenant) {
        try {
            validateInternalToken(headers);
            TenantContext.setCurrentTenant(tenant);
            java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("agents", posPrintingService.listAgents());
            response.put("terminals", posPrintingService.listTerminals());
            response.put("jobs", posPrintingService.listJobs());
            return ResponseEntity.ok(response);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @RequestMapping(value = "/internal/admin/tenant/{tenant}/pos-printing/enrollments", method = RequestMethod.POST)
    public ResponseEntity<?> createPosPrintingEnrollment(@RequestHeader HttpHeaders headers, @PathVariable String tenant, @RequestBody EnrollmentCreateRequest request) {
        try { validateInternalToken(headers); TenantContext.setCurrentTenant(tenant); return ResponseEntity.ok(posPrintingService.createEnrollment(request)); }
        catch (SecurityException ex) { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage()); }
        catch (Exception ex) { return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage()); }
    }

    @RequestMapping(value = "/internal/admin/tenant/{tenant}/pos-printing/agents/{agentId}/revoke", method = RequestMethod.POST)
    public ResponseEntity<?> revokePosPrintAgent(@RequestHeader HttpHeaders headers, @PathVariable String tenant, @PathVariable String agentId) {
        try { validateInternalToken(headers); TenantContext.setCurrentTenant(tenant); posPrintingService.revokeAgent(agentId); return ResponseEntity.ok("{\"success\":true}"); }
        catch (SecurityException ex) { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage()); }
        catch (Exception ex) { return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage()); }
    }

    @RequestMapping(value = "/internal/admin/tenant/{tenant}/pos-printing/terminals/{terminalId}/enabled", method = RequestMethod.POST)
    public ResponseEntity<?> setPosTerminalEnabled(
            @RequestHeader HttpHeaders headers,
            @PathVariable String tenant,
            @PathVariable String terminalId,
            @RequestBody TerminalEnabledRequest request
    ) {
        try {
            validateInternalToken(headers);
            TenantContext.setCurrentTenant(tenant);
            return ResponseEntity.ok(posPrintingService.setTerminalEnabled(terminalId, request));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @RequestMapping(value = "/internal/admin/tenant/{tenant}/pos-printing/jobs/{jobId}/retry", method = RequestMethod.POST)
    public ResponseEntity<?> retryPosPrintJob(@RequestHeader HttpHeaders headers, @PathVariable String tenant, @PathVariable String jobId) {
        try { validateInternalToken(headers); TenantContext.setCurrentTenant(tenant); return ResponseEntity.ok(posPrintingService.retry(jobId)); }
        catch (SecurityException ex) { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage()); }
        catch (Exception ex) { return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage()); }
    }

    @RequestMapping(value = "/v2/admin-handoff/exchange", method = RequestMethod.POST)
    public ResponseEntity<?> exchangeHandoff(@RequestBody AdminHandoffExchangeRequestDto requestDto, HttpServletRequest servletRequest) {
        try {
            AuthenticationResponseDto responseDto = adminHandoffService.exchange(requestDto == null ? null : requestDto.getToken(), servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent"));
            return ResponseEntity.ok(gson.toJson(responseDto));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }


    private int syncPropertiesFromAdminConsole(String tenant) {
        String propertyJson = tenantAdminService.getTenantProperty(tenant);
        if (propertyJson == null || propertyJson.isBlank()) {
            return 0;
        }
        JSONObject properties = new JSONObject(propertyJson);
        int updated = 0;
        Iterator<String> keys = properties.keys();
        while (keys.hasNext()) {
            String property = keys.next();
            Object value = properties.opt(property);
            if (property == null || value == null) {
                continue;
            }
            tenantService.addProperty(new TenantPropertyDto(tenant, property, value.toString()));
            updated++;
        }
        return updated;
    }

    private void validateInternalToken(HttpHeaders headers) {
        String token = headers.getFirst(INTERNAL_TOKEN_HEADER);
        adminHandoffService.validateInternalToken(token);
    }
}
