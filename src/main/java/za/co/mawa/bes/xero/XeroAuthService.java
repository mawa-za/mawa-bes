package za.co.mawa.bes.xero;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.service.SettingService;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class XeroAuthService {
    @Autowired
    SettingService settingService;
    @Getter
    private static final String CLIENT_ID = "71674DC318314EBAAF07D16186E42D02";
    @Getter
    private static final String CLIENT_SECRET = "c90a_5H72_f0DXSnQroKDcp1pedaI9nVpOk4NayCi7viLyRO";

    // change the redirect uri
    @Getter
    private static final String REDIRECT_URI = "http://localhost:8080/xero/callback";

    @Getter
    private static final String TOKEN_URL = "https://identity.xero.com/connect/token";
    @Getter
    private static final String AUTH_URL = "https://login.xero.com/identity/connect/authorize";
    @Getter
    private static final String SCOPES = "offline_access accounting.transactions";

    private static final String API_URL = "https://api.xero.com/api.xro/2.0/Invoices";
//    @Getter
//    private static String refreshToken = "stored_refresh_token";
//    @Getter
//    private static String accessToken = "stored_access_token";
//    @Getter
//    private static String xeroTenantId = "stored_xero_tenant_id";
//    @Getter
//    private static long expiresAt = System.currentTimeMillis() + (1800 * 1000);


    public  String getInitialTokens(String authorizationCode) throws IOException {
        try {
            String requestBody = "grant_type=authorization_code" +
                    "&code=" + authorizationCode +
                    "&redirect_uri=" + REDIRECT_URI;

            //all info about token is returned and to be store
            String response = sendTokenRequest(requestBody);

            //            createInvoice(accessToken);
            System.out.println("Initial Token Response: " + response);
//            System.out.println("Token " + accessToken);

            String refreshToken = extractRefreshToken(response);
            settingService.createSetting(XeroUtils.XERO_REFRESH_TOKEN,XeroUtils.XERO_INVOICE,refreshToken);

            String accessToken = extractAccessToken(response);
            settingService.createSetting(XeroUtils.XERO_ACCESS_TOKEN, XeroUtils.XERO_INVOICE, accessToken);

            String xeroTenantId = sendGetXeroTenantIdRequest(accessToken);
            settingService.createSetting(XeroUtils.XERO_TENANT_ID, XeroUtils.XERO_INVOICE, xeroTenantId);
            ;
            String expiresAt = String.valueOf(System.currentTimeMillis() + (1800 * 1000));
            settingService.createSetting(XeroUtils.XERO_EXPIRE_AT, XeroUtils.XERO_INVOICE, expiresAt);

            return accessToken ;
        } catch (Exception e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }

    public  String refreshAccessToken() throws IOException {
        // check if access token expired

        String refreshToken = settingService.getSetting(XeroUtils.XERO_REFRESH_TOKEN ,XeroUtils.XERO_INVOICE);

        String requestBody = "grant_type=refresh_token&refresh_token=" + refreshToken;

        String response = sendTokenRequest(requestBody);
        System.out.println("New Token Response: " + response);

        refreshToken = extractRefreshToken(response);
        settingService.updateSetting(XeroUtils.XERO_REFRESH_TOKEN ,XeroUtils.XERO_INVOICE,refreshToken);

        String accessToken = extractAccessToken(response);
        settingService.updateSetting(XeroUtils.XERO_ACCESS_TOKEN, XeroUtils.XERO_INVOICE,accessToken);

        String xeroTenantId = sendGetXeroTenantIdRequest(accessToken);
        settingService.updateSetting(XeroUtils.XERO_TENANT_ID, XeroUtils.XERO_INVOICE,xeroTenantId);

        String expiresAt = String.valueOf(System.currentTimeMillis() + (1800 * 1000));
        settingService.updateSetting(XeroUtils.XERO_EXPIRE_AT, XeroUtils.XERO_INVOICE,expiresAt);

        return response;
    }

    private static String sendTokenRequest(String requestBody) throws IOException {
        String authString = CLIENT_ID + ":" + CLIENT_SECRET;
        String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));

        URL url = new URL(TOKEN_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        br.close();

        return response.toString();
    }

    private static String extractAccessToken(String jsonResponse){

            String accessToken = null ;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);

            accessToken = jsonNode.get("access_token").asText();
            String refreshToken = jsonNode.get("refresh_token").asText();
            int expiresIn = jsonNode.get("expires_in").asInt();
            String tokenType = jsonNode.get("token_type").asText();
            String scope = jsonNode.get("scope").asText();
            return accessToken;
        } catch (Exception e) {
            e.printStackTrace();
        }
       return accessToken;

    }

    private static String extractRefreshToken(String jsonResponse){

        String refreshToken = null ;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            refreshToken = jsonNode.get("refresh_token").asText();
            int expiresIn = jsonNode.get("expires_in").asInt();
            String tokenType = jsonNode.get("token_type").asText();
            String scope = jsonNode.get("scope").asText();
            return refreshToken;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return refreshToken;

    }

    private static int checkExpire(String jsonResponse){

        int expire = Integer.parseInt(null);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            String refreshToken = jsonNode.get("refresh_token").asText();
            int expiresIn = jsonNode.get("expires_in").asInt();
            String tokenType = jsonNode.get("token_type").asText();
            String scope = jsonNode.get("scope").asText();
            return expiresIn;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return expire;

    }

    public static String sendGetXeroTenantIdRequest(String accessToken) {
        HttpURLConnection connection = null;
        String XERO_CONNECTIONS_URL = "https://api.xero.com/connections";
        try {
            URL url = new URL(XERO_CONNECTIONS_URL);
            connection = (HttpURLConnection) url.openConnection();

            // Set request properties
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                System.err.println("Failed to retrieve tenant ID. HTTP Code: " + responseCode);
                return null;
            }

            // Read response
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                // Extract tenantId from JSON response
                String jsonResponse = response.toString();

                return extractTenantId(jsonResponse);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String extractTenantId(String jsonResponse) {
        int index = jsonResponse.indexOf("\"tenantId\":\"");
        if (index != -1) {
            int start = index + 12;
            int end = jsonResponse.indexOf("\"", start);
            return jsonResponse.substring(start, end);
        }
        return null;
    }


}

