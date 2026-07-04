package za.co.mawa.bes.xero;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.gcp.GcpTenantSecretService;
import za.co.mawa.bes.dto.TenantPropertyDto;
import za.co.mawa.bes.entity.SettingEntity;
import za.co.mawa.bes.entity.SettingPKEntity;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.service.TenantAdminService;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@Service
public class XeroAuthService {
    @Autowired
    SettingService settingService;
    @Autowired
    TenantAdminService tenantAdminService;
    @Autowired
    GcpTenantSecretService gcpTenantSecretService;

//    @Getter
//    private static final String CLIENT_ID = "71674DC318314EBAAF07D16186E42D02";
//    @Getter
//    private static final String CLIENT_SECRET = "c90a_5H72_f0DXSnQroKDcp1pedaI9nVpOk4NayCi7viLyRO";

    // change the redirect uri
//    @Getter
//    private static final String REDIRECT_URI = "http://localhost:8080/xero/callback";

    @Getter
    private static final String TOKEN_URL = "https://identity.xero.com/connect/token";
    @Getter
    private static final String AUTH_URL = "https://login.xero.com/identity/connect/authorize";
    @Getter
    private static final String SCOPES = "offline_access accounting.transactions accounting.contacts.read";

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
            String tenant = TenantContext.getCurrentTenant();
            JSONObject jsonObject = tenantProperties(tenant);
            String client_id = requiredXeroProperty(jsonObject, XeroUtils.XERO_CLIENT_ID);
            String client_secret = requiredXeroProperty(jsonObject, XeroUtils.XERO_CLIENT_SECRET);
            String redirectUrl = requiredXeroProperty(jsonObject, XeroUtils.XERO_REDIRECT_URL);

            String requestBody = "grant_type=authorization_code" +
                    "&code=" + authorizationCode +
                    "&redirect_uri=" + redirectUrl;

            //all info about token is returned and to be store
            String response = sendTokenRequest(requestBody, client_id,client_secret);

//            createInvoice(accessToken);
//            System.out.println("Initial Token Response: " + response);
//            System.out.println("Token " + accessToken);

            String refreshToken = extractRefreshToken(response);
//            settingService.createSetting(XeroUtils.XERO_REFRESH_TOKEN,XeroUtils.XERO_INVOICE,refreshToken);
            createProperty(tenant,XeroUtils.XERO_REFRESH_TOKEN,refreshToken);

            String accessToken = extractAccessToken(response);
//            settingService.createSetting(XeroUtils.XERO_ACCESS_TOKEN, XeroUtils.XERO_INVOICE, accessToken);
            createProperty(tenant,XeroUtils.XERO_ACCESS_TOKEN,accessToken);

            String xeroTenantId = sendGetXeroTenantIdRequest(accessToken);
//            settingService.createSetting(XeroUtils.XERO_TENANT_ID, XeroUtils.XERO_INVOICE, xeroTenantId);
            createProperty(tenant,XeroUtils.XERO_TENANT_ID,xeroTenantId);

            String expiresAt = String.valueOf(System.currentTimeMillis() + (1800 * 1000));
//            settingService.createSetting(XeroUtils.XERO_EXPIRE_AT, XeroUtils.XERO_INVOICE, expiresAt);
            createProperty(tenant,XeroUtils.XERO_EXPIRE_AT,expiresAt);

            return accessToken ;
        } catch (Exception e) {
//            System.out.println(e);
            throw new RuntimeException(e);
        }
    }

    public  String refreshAccessToken(String tenant) throws IOException {
        // check if access token expired

//        String tenant = TenantContext.getCurrentTenant();

//        String refreshToken = settingService.getSetting(XeroUtils.XERO_REFRESH_TOKEN ,XeroUtils.XERO_INVOICE);
        JSONObject jsonObject = tenantProperties(tenant);
        String refreshToken = requiredXeroProperty(jsonObject, XeroUtils.XERO_REFRESH_TOKEN);
        String client_id = requiredXeroProperty(jsonObject, XeroUtils.XERO_CLIENT_ID);
        String client_secret = requiredXeroProperty(jsonObject, XeroUtils.XERO_CLIENT_SECRET);

        String requestBody = "grant_type=refresh_token&refresh_token=" + refreshToken;

        String response = sendTokenRequest(requestBody, client_id,client_secret);
//        System.out.println("New Token Response: " + response);

         refreshToken = extractRefreshToken(response);
//            settingService.createSetting(XeroUtils.XERO_REFRESH_TOKEN,XeroUtils.XERO_INVOICE,refreshToken);
        createProperty(tenant,XeroUtils.XERO_REFRESH_TOKEN,refreshToken);

        String accessToken = extractAccessToken(response);
        createProperty(tenant,XeroUtils.XERO_ACCESS_TOKEN,accessToken);

//        String xeroTenantId = sendGetXeroTenantIdRequest(accessToken);
//        createProperty(tenant,XeroUtils.XERO_TENANT_ID,xeroTenantId);

        String expiresAt = String.valueOf(System.currentTimeMillis() + (1800 * 1000));
        createProperty(tenant,XeroUtils.XERO_EXPIRE_AT,expiresAt);


        return accessToken;
    }

    private static String sendTokenRequest(String requestBody, String clientId, String clientSecret) throws IOException {

        String authString = clientId + ":" + clientSecret;
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

        // Read and handle response
        int responseCode = connection.getResponseCode();
        if (responseCode >= 300) {
            String errorResponse = readErrorStream(connection);
            throw new IOException(String.format("Request failed with code: %d. Response: %s",
                    responseCode, errorResponse));
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

    public void createProperty(String tenant, String property, String value) {
        JSONObject properties = tenantProperties(tenant);
        String secretReference = gcpTenantSecretService.findTenantPropertySecretReference(properties, property);
        String directReference = properties.optString(property, null);
        if ((secretReference == null || secretReference.isBlank())
                && gcpTenantSecretService.isSecretReference(directReference)) {
            secretReference = directReference;
        }
        if (secretReference != null && !secretReference.isBlank()) {
            gcpTenantSecretService.addSecretVersion(secretReference, value);
            return;
        }

        // Backward compatibility only: if a secret reference is not configured yet,
        // continue writing to tenant properties so existing tenants do not break.
        TenantPropertyDto tenantPropertyDto = new TenantPropertyDto();
        tenantPropertyDto.setTenant(tenant);
        tenantPropertyDto.setProperty(property);
        tenantPropertyDto.setValue(value);
        tenantAdminService.addTenantProperty(tenant , tenantPropertyDto);
    }

    public String updateProperty(String tenant, String setting ) {
        tenantAdminService.getTenantProperties(tenant);
        return null;
    }

    public String checkXeroInfo(){
        String tenant = TenantContext.getCurrentTenant();
        JSONObject currentTenantProperties = tenantProperties(tenant);

        String refreshToken = getXeroProperty(currentTenantProperties, XeroUtils.XERO_REFRESH_TOKEN);
        if (!isBlank(refreshToken)) {
            return tenant;
        }

        String serviceProviderTenant = getXeroProperty(currentTenantProperties, XeroUtils.XERO_MAWA_SERVICE_PROVIDER_LINK);
        if (!isBlank(serviceProviderTenant)) {
            JSONObject serviceProviderProperties = tenantProperties(serviceProviderTenant);
            String serviceProviderRefreshToken = getXeroProperty(serviceProviderProperties, XeroUtils.XERO_REFRESH_TOKEN);
            if (!isBlank(serviceProviderRefreshToken)) {
                return serviceProviderTenant;
            }
        }

        return null;
    }

    public String getXeroProperty(String tenant, String property) {
        return getXeroProperty(tenantProperties(tenant), property);
    }

    public String getXeroProperty(String tenant, String property, String defaultValue) {
        String value = getXeroProperty(tenantProperties(tenant), property);
        return isBlank(value) ? defaultValue : value;
    }

    private String getXeroProperty(JSONObject properties, String property) {
        return gcpTenantSecretService.resolveTenantProperty(properties, property);
    }

    private String requiredXeroProperty(JSONObject properties, String property) {
        String value = getXeroProperty(properties, property);
        if (isBlank(value)) {
            throw new IllegalStateException("Missing required Xero configuration: " + property + ". Store a Google Secret Manager reference in " + property + "-SECRET or configure the tenant property.");
        }
        return value;
    }

    private JSONObject tenantProperties(String tenant) {
        if (isBlank(tenant)) {
            return new JSONObject();
        }
        String tenantProperty = tenantAdminService.getTenantProperty(tenant);
        if (isBlank(tenantProperty)) {
            return new JSONObject();
        }
        return new JSONObject(tenantProperty);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim());
    }

    private static String readErrorStream(HttpURLConnection connection) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }

//    public void updateSetting(String attribute, String setting, String newValue) {
//        SettingPKEntity settingPKEntity = new SettingPKEntity();
//        settingPKEntity.setSetting(setting);
//        settingPKEntity.setAttribute(attribute);
//
//        Optional<SettingEntity> settingEntity = settingRepository.findById(settingPKEntity);
//        if (settingEntity.isPresent()) {
//            SettingEntity existingSetting = settingEntity.get();
//            existingSetting.setValue(newValue);
//            settingRepository.save(existingSetting);
//        }
//    }
//
//    public void deleteSetting(String attribute, String setting) {
//        SettingPKEntity settingPKEntity = new SettingPKEntity();
//        settingPKEntity.setSetting(setting);
//        settingPKEntity.setAttribute(attribute);
//
//        settingRepository.deleteById(settingPKEntity);
//    }

}

