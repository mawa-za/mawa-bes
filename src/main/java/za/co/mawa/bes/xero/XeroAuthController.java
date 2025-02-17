package za.co.mawa.bes.xero;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.mawa.bes.service.SettingService;

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

    @GetMapping("/xero/connect")
    public ResponseEntity<String> redirectToXero() throws IOException {
        String authUrl = XeroAuthService.getAUTH_URL() + "?response_type=code" +
                "&client_id=" + XeroAuthService.getCLIENT_ID() +
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
            return xeroAuthService.refreshAccessToken();
        } catch (Exception e) {
            return "errer"+ e ;
        }
    }

    @GetMapping("/xero/createInvoice")
    public String createInvoice() {
        // Store the code for later use in token exchange
        try {
            String accessToken = settingService.getSetting(XeroUtils.XERO_ACCESS_TOKEN ,XeroUtils.XERO_INVOICE);
            String tenantId = settingService.getSetting(XeroUtils.XERO_TENANT_ID ,XeroUtils.XERO_INVOICE);

            System.out.println(accessToken);
            System.out.println(tenantId);
            XeroAccountingService xeroAccountingService = new XeroAccountingService();

            return xeroAccountingService.createInvoice(accessToken, tenantId);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
