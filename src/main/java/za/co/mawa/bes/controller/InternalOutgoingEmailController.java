package za.co.mawa.bes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.service.OutgoingGatewaySecurity;
import za.co.mawa.bes.service.OutgoingGatewaySubmissionService;

/** Private application-to-application gateway. No tenant user JWT or generic internal token. */
@RestController
public class InternalOutgoingEmailController {
    private final OutgoingGatewaySecurity authenticator;
    private final OutgoingGatewaySubmissionService submission;
    private final ObjectMapper mapper;
    private final boolean enabled;
    private final boolean queueEnabled;

    public InternalOutgoingEmailController(OutgoingGatewaySecurity authenticator,
                                           OutgoingGatewaySubmissionService submission,
                                           ObjectMapper mapper,
                                           @Value("${mawa.outgoing.gateway.enabled:false}") boolean enabled,
                                           @Value("${mawa.outgoing.queue.enabled:false}") boolean queueEnabled) {
        this.authenticator = authenticator;
        this.submission = submission;
        this.mapper = mapper;
        this.enabled = enabled;
        this.queueEnabled = queueEnabled;
    }

    @PostMapping(value = "/internal/admin/tenant/{tenant}/outgoing-email",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> submit(@PathVariable String tenant, @RequestBody byte[] payload,
                                    HttpServletRequest httpRequest,
                                    @RequestHeader(value = "X-Mawa-Mail-Client", required = false) String client,
                                    @RequestHeader(value = "X-Mawa-Mail-Timestamp", required = false) String timestamp,
                                    @RequestHeader(value = "X-Mawa-Mail-Nonce", required = false) String nonce,
                                    @RequestHeader(value = "X-Mawa-Mail-Signature", required = false) String signature) {
        if (!enabled || !queueEnabled) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        if (payload == null || payload.length == 0 || payload.length > 16_384) {
            return ResponseEntity.badRequest().body("Invalid mail request size");
        }
        try {
            String verified = authenticator.authenticate(client, timestamp, nonce, signature,
                    httpRequest.getMethod(), httpRequest.getRequestURI(), tenant, payload);
            if (!tenant.equals(TenantContext.getCurrentTenant())) {
                throw new SecurityException("Tenant context mismatch");
            }
            var request = mapper.readValue(payload, OutgoingGatewaySubmissionService.Request.class);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(submission.submit(verified, nonce, request));
        } catch (za.co.mawa.bes.service.OutgoingGatewayQuotaService.QuotaExceededException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Outgoing email submission limit reached; try again after UTC midnight");
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised outgoing mail request");
        } catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("Invalid outgoing mail request");
        }
        // Do not catch and disguise data-layer failures: they must roll back the nonce + outbox.
    }

    /** Signed read-only status request; source is never client-supplied. */
    @PostMapping(value = "/internal/admin/tenant/{tenant}/outgoing-email/status",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> status(@PathVariable String tenant, @RequestBody byte[] payload,
                                    HttpServletRequest httpRequest,
                                    @RequestHeader(value = "X-Mawa-Mail-Client", required = false) String client,
                                    @RequestHeader(value = "X-Mawa-Mail-Timestamp", required = false) String timestamp,
                                    @RequestHeader(value = "X-Mawa-Mail-Nonce", required = false) String nonce,
                                    @RequestHeader(value = "X-Mawa-Mail-Signature", required = false) String signature) {
        if (!enabled || !queueEnabled) return ResponseEntity.notFound().build();
        if (payload == null || payload.length == 0 || payload.length > 1_024) {
            return ResponseEntity.badRequest().body("Invalid status request size");
        }
        try {
            String verified = authenticator.authenticate(client, timestamp, nonce, signature,
                    httpRequest.getMethod(), httpRequest.getRequestURI(), tenant, payload);
            if (!tenant.equals(TenantContext.getCurrentTenant())) {
                throw new SecurityException("Tenant context mismatch");
            }
            var request = mapper.readValue(payload, OutgoingGatewaySubmissionService.StatusRequest.class);
            var found = submission.status(verified, request);
            if (found.isEmpty()) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(found.get());
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised outgoing mail request");
        } catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("Invalid outgoing mail status request");
        }
    }

}
