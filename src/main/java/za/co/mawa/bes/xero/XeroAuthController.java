package za.co.mawa.bes.xero;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
public class XeroAuthController {


    @GetMapping("/xero/connect")
    public ResponseEntity<String> redirectToXero() throws IOException {
        String authUrl = XeroAuth.getAUTH_URL() + "?response_type=code" +
                "&client_id=" + XeroAuth.getCLIENT_ID() +
                "&redirect_uri=" + URLEncoder.encode(XeroAuth.getREDIRECT_URI(), StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(XeroAuth.getSCOPES(), StandardCharsets.UTF_8);

            return ResponseEntity.ok("Please open this URL manually: " + authUrl);
    }

    @GetMapping("/xero/callback")
    public boolean callback(@RequestParam String code, @RequestParam(required = false) String state) {
        // Store the code for later use in token exchange
        try {
            XeroAuth.getInitialTokens(code);

            XeroAccountingService xeroAccountingService = new XeroAccountingService();
            return xeroAccountingService.createInvoice(XeroAuth.getAccessToken(), XeroAuth.getXeroTenantId());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/xero/refreshToken")
    public String getToken() throws IOException {
        try{
            return XeroAuth.refreshAccessToken();
        } catch (Exception e) {
            return "errer"+ e ;
        }
    }

    @GetMapping("/xero/createInvoice")
    public boolean createInvoice() {
        // Store the code for later use in token exchange
        try {
            XeroAccountingService xeroAccountingService = new XeroAccountingService();
            return xeroAccountingService.createInvoice(XeroAuth.getAccessToken(), XeroAuth.getXeroTenantId());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
