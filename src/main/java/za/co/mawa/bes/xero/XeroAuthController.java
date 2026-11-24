package za.co.mawa.bes.xero;

import com.nimbusds.jose.shaded.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.service.TenantAdminService;
import za.co.mawa.bes.dto.v2.integration.XeroConnectionDto;
import za.co.mawa.bes.service.v2.integration.XeroActivationService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;


@RestController
@CrossOrigin
public class XeroAuthController {

    @Autowired
    XeroAuthService xeroAuthService;
    @Autowired
    XeroOAuthStateService oauthStateService;
    @Autowired
    XeroActivationService xeroActivationService;
    @Autowired
    SettingService settingService;
    @Autowired
    XeroAccountingService xeroAccountingService;
    @Autowired
    TenantAdminService tenantAdminService;
    Gson gson = new Gson();

    @RequestMapping(value="/xero/connect" , method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> redirectToXero(@RequestBody XeroAuthDto xeroAuthDto) throws IOException {

        String tenant = TenantContext.getCurrentTenant();

        // Activation owns the authoritative callback URL in Group XERO. Do not
        // overwrite it with a competing legacy tenant property.
        // The payload redirect is kept for backwards compatibility but is not
        // allowed to override the activated callback configuration.
        // Client ID and Client Secret must be captured through /v2/integrations/xero/activate.
        // That endpoint writes the values to Google Secret Manager and stores only secret references
        // in Group: XERO settings. Do not persist raw Xero credentials from /xero/connect.

        String clientId = xeroAuthService.getXeroProperty(tenant, XeroUtils.XERO_CLIENT_ID);
        String redirectUrl = xeroAuthService.getXeroProperty(tenant, XeroUtils.XERO_REDIRECT_URL);
        if (clientId == null || clientId.isBlank() || redirectUrl == null || redirectUrl.isBlank()) {
            throw new IllegalStateException("Xero client id and redirect URL must be configured. Use Google Secret Manager references for sensitive Xero values.");
        }


        String tenantState = oauthStateService.issue(TenantContext.getCurrentTenantURL());
        String authUrl = XeroAuthService.getAUTH_URL() + "?response_type=code" +
                "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8) +
                "&scope=" + encodeOAuthScope(XeroAuthService.getSCOPES()) +
                "&state=" + URLEncoder.encode(tenantState, StandardCharsets.UTF_8);

        return ResponseEntity.ok(Map.of("authenticationUrl", authUrl));
    }

    private String encodeOAuthScope(String scopes) {
        return URLEncoder.encode(scopes, StandardCharsets.UTF_8).replace("+", "%20");
    }


    @RequestMapping(value="/xero/callback" , method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String callback(@RequestParam(required = false) String code,
                           @RequestParam(required = false) String state,
                           @RequestParam(required = false) String error) {
        // Xero redirects back without Authorization or X-Tenant-Id headers.
        // The tenant is therefore carried in OAuth state from /xero/connect.
        try {
            if (!StringUtils.hasText(state)) {
                throw new IllegalStateException("Xero callback is missing tenant state. Please start the connection from /xero/connect again.");
            }
            String resolvedTenant = TenantContext.getCurrentTenant();
            if (!StringUtils.hasText(resolvedTenant)) {
                throw new IllegalStateException("Xero callback tenant could not be resolved from state. Please start the connection from /xero/connect again.");
            }
            oauthStateService.validateAndConsume(state, TenantContext.getCurrentTenantURL());
            if (StringUtils.hasText(error)) {
                return "<html><body><h2>Xero authorisation was not completed</h2>"
                        + "<p>Return to MAWA Settings &gt; XERO to reconnect.</p></body></html>";
            }
            if (!StringUtils.hasText(code)) {
                throw new IllegalArgumentException("Xero authorisation code is missing");
            }
            XeroAuthService.XeroOAuthResult result = xeroAuthService.completeInitialAuthorisation(resolvedTenant, code);
            xeroActivationService.completeAuthorisation(result);

            if (result.isOrganisationSelectionRequired()) {
                String organisations = result.getConnections().stream()
                        .map(connection -> escapeHtml(connection.getTenantName()) + " (" + escapeHtml(connection.getTenantType()) + ")")
                        .collect(Collectors.joining("<br/>"));
                return "<html><body>"
                        + "<h2>Xero authorisation completed</h2>"
                        + "<p>Multiple Xero organisations are linked to this Xero user.</p>"
                        + "<p>Return to MAWA Settings &gt; XERO and click <b>Select Organisation</b>.</p>"
                        + "<p>Available organisations:</p><p>" + organisations + "</p>"
                        + "</body></html>";
            }

            return "<html><body>"
                    + "<h2>Xero authorisation completed</h2>"
                    + "<p>Connected organisation: " + escapeHtml(result.getSelectedTenantName()) + "</p>"
                    + "<p>You can close this window and return to MAWA.</p>"
                    + "</body></html>";

        } catch (Exception e) {
            // Never echo OAuth codes, state, tokens or internal exceptions in HTML.
            return "<html><body><h2>Xero connection could not be completed</h2>"
                    + "<p>Return to MAWA Settings &gt; XERO and try reconnecting.</p></body></html>";
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    @GetMapping("/xero/refreshToken")
    public ResponseEntity<Map<String, String>> getToken() {
        // Never return a bearer token to browsers or clients. Refresh is internal.
        return ResponseEntity.status(HttpStatus.GONE)
                .body(Map.of("message", "The Xero token endpoint is retired. Use MAWA Xero connection status."));
    }

//    @GetMapping("/xero/createInvoice")
//    public String createInvoice() {
//        // Store the code for later use in token exchange
//        try {
//
//            return xeroAccountingService.createInvoice("ff808081932a428001932a4b1b520005", "claim-id","BOOK");
////            return xeroAccountingService.createInvoice("ff80808191c16c680191c17d16830000","ff808081932a428001932a4b1b520005", "claim-id","BOOK");
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

    @GetMapping("/xero/contact")
    public ResponseEntity<?> getContact() {
        // Store the code for later use in token exchange
        try {

            return ResponseEntity.ok(xeroAccountingService.getXeroContact());
//            return xeroAccountingService.createInvoice("ff80808191c16c680191c17d16830000","ff808081932a428001932a4b1b520005", "claim-id","BOOK");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
