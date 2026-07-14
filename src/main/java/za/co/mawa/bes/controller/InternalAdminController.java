package za.co.mawa.bes.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import org.json.JSONObject;
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
import za.co.mawa.bes.service.TenantAdminService;
import za.co.mawa.bes.service.TenantService;

import java.util.Iterator;

@RestController
@CrossOrigin
public class InternalAdminController {

    private static final String INTERNAL_TOKEN_HEADER = "X-Mawa-Internal-Token";

    @Autowired
    private AdminHandoffService adminHandoffService;

    @Autowired
    private TenantAdminService tenantAdminService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private AttachmentService attachmentService;

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

    @RequestMapping(value = "/internal/admin/tenant/{tenant}/attachments/migrate-to-gcp", method = RequestMethod.POST)
    public ResponseEntity<?> migrateTenantAttachments(@RequestHeader HttpHeaders headers, @PathVariable String tenant) {
        try {
            validateInternalToken(headers);
            TenantContext.setCurrentTenant(tenant);
            AttachmentService.MigrationResult result = attachmentService.migrateLegacyDatabaseFilesToGcpWithResult();
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("tenant", tenant);
            payload.put("attempted", result.attempted());
            payload.put("migrated", result.migrated());
            payload.put("failed", result.failed());
            payload.put("remaining", result.remaining());
            payload.put("completed", result.completed());
            payload.put("failures", result.failures());

            if (result.failed() > 0 && result.migrated() == 0) {
                payload.put("message", "No attachments were migrated. Review the GCS/IAM error details in failures.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(gson.toJson(payload));
            }
            payload.put("message", result.completed()
                    ? "Attachment migration completed"
                    : "Attachment migration completed with remaining legacy records");
            return ResponseEntity.ok(gson.toJson(payload));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    @RequestMapping(value = "/v2/admin-handoff/exchange", method = RequestMethod.POST)
    public ResponseEntity<?> exchangeHandoff(@RequestBody AdminHandoffExchangeRequestDto requestDto) {
        try {
            AuthenticationResponseDto responseDto = adminHandoffService.exchange(requestDto == null ? null : requestDto.getToken());
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
