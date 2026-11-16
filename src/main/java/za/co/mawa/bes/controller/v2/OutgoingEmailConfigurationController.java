package za.co.mawa.bes.controller.v2;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import za.co.mawa.bes.service.OutgoingEmailConfigurationService;
import za.co.mawa.bes.service.SharedOutgoingEmailService;
import za.co.mawa.bes.service.UserAccessService;
import za.co.mawa.bes.configuration.context.UserContext;

import java.util.List;
import java.util.Map;

/** Protected tenant-level settings; no credential is ever returned in an API response. */
@RestController
@RequestMapping("/v2/outgoing-email")
public class OutgoingEmailConfigurationController {
    private final OutgoingEmailConfigurationService configuration;
    private final SharedOutgoingEmailService sender;
    private final UserAccessService access;

    public OutgoingEmailConfigurationController(OutgoingEmailConfigurationService configuration,
                                                SharedOutgoingEmailService sender, UserAccessService access) {
        this.configuration = configuration;
        this.sender = sender;
        this.access = access;
    }

    private void authorize(String role) {
        if (UserContext.isBackgroundSession() || !access.hasInteractiveSession()
                || !access.isProtectedAdministrator())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only an authorized tenant administrator can configure outgoing email");
        access.assertWorkcentreAccess("system-configuration", role);
    }

    @GetMapping("/configuration")
    public OutgoingEmailConfigurationService.Configuration configuration(
            @RequestHeader(value = "X-Role", required = false) String role) {
        authorize(role);
        return configuration.get();
    }

    @PutMapping("/configuration")
    public OutgoingEmailConfigurationService.Configuration save(
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestBody OutgoingEmailConfigurationService.Update request) {
        authorize(role);
        var saved = configuration.save(request);
        access.audit("OUTGOING_EMAIL_CONFIGURATION_UPDATED", "TENANT", "OUTGOING-EMAIL",
                "ERP outgoing email settings changed",
                "provider=" + saved.provider() + "; enabled=" + saved.enabled());
        return saved;
    }

    public record TestRequest(String recipient) { }

    @PostMapping("/test")
    public Map<String, String> test(@RequestHeader(value = "X-Role", required = false) String role,
                                     @RequestBody TestRequest request) {
        authorize(role);
        access.assertExternalTransactionAllowed("outgoing email test");
        String recipient = SharedOutgoingEmailService.validatedSingleAddress(
                request == null ? null : request.recipient());
        String id = sender.submit(recipient, "MAWA outgoing email test",
                "<p>This is a test email sent from your MAWA ERP outgoing email configuration.</p>",
                List.of(), "ERP_OUTGOING_EMAIL_TEST");
        access.audit("OUTGOING_EMAIL_TEST_SUBMITTED", "TENANT", "OUTGOING-EMAIL",
                "Outgoing email test requested", "requestId=" + id);
        return Map.of("requestId", id, "status", "ACCEPTED",
                "message", "Microsoft 365 accepted the email for delivery; inbox receipt is not guaranteed.");
    }
}
