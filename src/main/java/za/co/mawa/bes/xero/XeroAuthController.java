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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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

        if(xeroAuthDto.getRedirectUrl() != null){
            String redirectUrl = xeroAuthDto.getRedirectUrl() + "/xero/callback";
           xeroAuthService.createProperty(tenant,XeroUtils.XERO_REDIRECT_URL,redirectUrl);
        }
        if(xeroAuthDto.getClientId() != null){
            xeroAuthService.createProperty(tenant,XeroUtils.XERO_CLIENT_ID,xeroAuthDto.getClientId());
        }
        if(xeroAuthDto.getClientSecret() != null){
            xeroAuthService.createProperty(tenant,XeroUtils.XERO_CLIENT_SECRET,xeroAuthDto.getClientSecret());
        }

        String tenantProperty = tenantAdminService.getTenantProperty(tenant);
        JSONObject jsonObject = new JSONObject(tenantProperty);
        String clientId = jsonObject.getString(XeroUtils.XERO_CLIENT_ID);
        String redirectUrl = jsonObject.getString(XeroUtils.XERO_REDIRECT_URL);


        String authUrl = XeroAuthService.getAUTH_URL() + "?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(XeroAuthService.getSCOPES(), StandardCharsets.UTF_8);

        return ResponseEntity.ok(Map.of("authenticationUrl", authUrl));
    }


    @RequestMapping(value="/xero/callback" , method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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
