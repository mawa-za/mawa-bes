package za.co.mawa.bes.xero;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
public class XeroAuthController {


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
            XeroAuthService.getInitialTokens(code);

            XeroAccountingService xeroAccountingService = new XeroAccountingService();
            return xeroAccountingService.createInvoice(XeroAuthService.getAccessToken(), XeroAuthService.getXeroTenantId());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/xero/refreshToken")
    public String getToken() throws IOException {
        try{
            return XeroAuthService.refreshAccessToken();
        } catch (Exception e) {
            return "errer"+ e ;
        }
    }

    @GetMapping("/xero/createInvoice")
    public String createInvoice() {
        // Store the code for later use in token exchange
        try {
            XeroAccountingService xeroAccountingService = new XeroAccountingService();
            return xeroAccountingService.createInvoice(XeroAuthService.getAccessToken(), XeroAuthService.getXeroTenantId());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
