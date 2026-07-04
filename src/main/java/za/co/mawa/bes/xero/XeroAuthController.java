package za.co.mawa.bes.xero;

import com.nimbusds.jose.shaded.gson.Gson;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.service.TenantAdminService;
import za.co.mawa.bes.dto.v2.integration.XeroConnectionDto;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import static io.swagger.v3.core.util.PrimitiveType.createProperty;

@RestController
@CrossOrigin
public class XeroAuthController {

    @Autowired
    XeroAuthService xeroAuthService;
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
        String tenantState = org.springframework.util.StringUtils.hasText(TenantContext.getCurrentTenantURL())
                ? TenantContext.getCurrentTenantURL()
                : tenant;

        if(xeroAuthDto.getRedirectUrl() != null){
            String redirectUrl = xeroAuthDto.getRedirectUrl() + "/xero/callback";
           xeroAuthService.createProperty(tenant,XeroUtils.XERO_REDIRECT_URL,redirectUrl);
        }
        // Client ID and Client Secret must be captured through /v2/integrations/xero/activate.
        // That endpoint writes the values to Google Secret Manager and stores only secret references
        // in Group: XERO settings. Do not persist raw Xero credentials from /xero/connect.

        String clientId = xeroAuthService.getXeroProperty(tenant, XeroUtils.XERO_CLIENT_ID);
        String redirectUrl = xeroAuthService.getXeroProperty(tenant, XeroUtils.XERO_REDIRECT_URL);
        if (clientId == null || clientId.isBlank() || redirectUrl == null || redirectUrl.isBlank()) {
            throw new IllegalStateException("Xero client id and redirect URL must be configured. Use Google Secret Manager references for sensitive Xero values.");
        }


        String authUrl = XeroAuthService.getAUTH_URL() + "?response_type=code" +
                "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(XeroAuthService.getSCOPES(), StandardCharsets.UTF_8) +
                "&state=" + URLEncoder.encode(tenantState, StandardCharsets.UTF_8);

        return ResponseEntity.ok(Map.of("authenticationUrl", authUrl));
    }


    @RequestMapping(value="/xero/callback" , method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String callback(@RequestParam String code, @RequestParam(required = false) String state) {
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
            TenantContext.setCurrentTenantURL(state);
            XeroAuthService.XeroOAuthResult result = xeroAuthService.completeInitialAuthorisation(resolvedTenant, code);

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

        } catch (IOException e) {
            throw new RuntimeException(e);
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
    public String getToken() throws IOException {
        try{
            String tenant = TenantContext.getCurrentTenant();
            return xeroAuthService.refreshAccessToken(tenant);
        } catch (Exception e) {
            return "errer"+ e ;
        }
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
