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
    private static final String CLIENT_ID = "71674DC318314EBAAF07D16186E42D02";
    private static final String REDIRECT_URI = "http://localhost:8080/xero/callback";
    private static final String AUTH_URL = "https://login.xero.com/identity/connect/authorize";
    private static final String SCOPES = "offline_access accounting.transactions";



    @GetMapping("/xero/connect")
    public ResponseEntity<String> redirectToXero() throws IOException {
        String authUrl = AUTH_URL + "?response_type=code" +
                "&client_id=" + CLIENT_ID +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(SCOPES, StandardCharsets.UTF_8);

            return ResponseEntity.ok("Please open this URL manually: " + authUrl);
    }

    @GetMapping("/xero/callback")
    public boolean callback(@RequestParam String code, @RequestParam(required = false) String state) {
        // Store the code for later use in token exchange
        try {
            String accessToken =  XeroAuth.getInitialTokens(code);
            XeroAccountingService xeroAccountingService = new XeroAccountingService();
            String xeroTenantId = XeroAuth.getXeroTenantId(accessToken);

            return xeroAccountingService.createInvoice(accessToken, xeroTenantId);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    @GetMapping("/xero/createInvoice")
//    public String getToken() throws IOException {
//        try{
//            XeroAccountingService xeroAccountingService = new XeroAccountingService();
//            boolean xeroCreate = xeroAccountingService.createInvoice();
//            return "created";
//        } catch (Exception e) {
//            return "errer"+ e ;
//        }
//    }
}
