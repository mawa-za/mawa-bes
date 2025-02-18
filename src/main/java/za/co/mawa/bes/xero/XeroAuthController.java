package za.co.mawa.bes.xero;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.service.TenantAdminService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

    @GetMapping("/xero/connect")
    public ResponseEntity<String> redirectToXero() throws IOException {

        String tenant = TenantContext.getCurrentTenant();
        String tenantProperty = tenantAdminService.getTenantProperty(tenant);
        JSONObject jsonObject = new JSONObject(tenantProperty);
        String clientId = jsonObject.getString(XeroUtils.XERO_CLIENT_ID);


        String authUrl = XeroAuthService.getAUTH_URL() + "?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + URLEncoder.encode(XeroAuthService.getREDIRECT_URI(), StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(XeroAuthService.getSCOPES(), StandardCharsets.UTF_8);

            return ResponseEntity.ok("Please open this URL manually: " + authUrl);
    }

    @GetMapping("/xero/callback")
    public String callback(@RequestParam String code, @RequestParam(required = false) String state) {
        // Store the code for later use in token exchange
        try {
            xeroAuthService.getInitialTokens(code);

            XeroAccountingService xeroAccountingService = new XeroAccountingService();
//            return xeroAccountingService.createInvoice(XeroAuthService.getAccessToken(), XeroAuthService.getXeroTenantId());
            return "successful";

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    @GetMapping("/xero/createInvoice")
    public String createInvoice() {
        // Store the code for later use in token exchange
        try {
//            String accessToken = settingService.getSetting(XeroUtils.XERO_ACCESS_TOKEN ,XeroUtils.XERO_INVOICE);
//            String tenantId = settingService.getSetting(XeroUtils.XERO_TENANT_ID ,XeroUtils.XERO_INVOICE);
//
//            System.out.println(accessToken);
//            System.out.println(tenantId);
//            XeroAccountingService xeroAccountingService = new XeroAccountingService();

            return xeroAccountingService.createInvoice("ff808081932a428001932a4b1b520005", "claim-id","BOOK");
//            return xeroAccountingService.createInvoice("ff80808191c16c680191c17d16830000","ff808081932a428001932a4b1b520005", "claim-id","BOOK");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
