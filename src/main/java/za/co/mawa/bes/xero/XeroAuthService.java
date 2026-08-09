package za.co.mawa.bes.xero;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.gcp.GcpTenantSecretService;
import za.co.mawa.bes.dto.TenantPropertyDto;
import za.co.mawa.bes.dto.v2.integration.XeroConnectionDto;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.service.TenantAdminService;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class XeroAuthService {
    private static final ConcurrentMap<String, Object> REFRESH_LOCKS = new ConcurrentHashMap<>();
    @Autowired
    SettingService settingService;
    @Autowired
    TenantAdminService tenantAdminService;
    @Autowired
    GcpTenantSecretService gcpTenantSecretService;

    @Getter
    private static final String TOKEN_URL = "https://identity.xero.com/connect/token";
    @Getter
    private static final String AUTH_URL = "https://login.xero.com/identity/connect/authorize";
    @Getter
    private static final String SCOPES = "offline_access accounting.transactions accounting.contacts.read";

    public String getInitialTokens(String authorizationCode) throws IOException {
        return getInitialTokens(TenantContext.getCurrentTenant(), authorizationCode);
    }

    public String getInitialTokens(String tenant, String authorizationCode) throws IOException {
        return completeInitialAuthorisation(tenant, authorizationCode).getAccessToken();
    }

    public XeroOAuthResult completeInitialAuthorisation(String tenant, String authorizationCode) throws IOException {
        try {
            if (isBlank(tenant)) {
                throw new IllegalStateException("Xero tenant is required to complete OAuth callback");
            }
            TenantContext.setCurrentTenant(tenant);
            JSONObject jsonObject = tenantProperties(tenant);
            String clientId = requiredXeroProperty(jsonObject, XeroUtils.XERO_CLIENT_ID);
            String clientSecret = requiredXeroProperty(jsonObject, XeroUtils.XERO_CLIENT_SECRET);
            String redirectUrl = requiredXeroProperty(jsonObject, XeroUtils.XERO_REDIRECT_URL);

            String requestBody = "grant_type=authorization_code" +
                    "&code=" + formEncode(authorizationCode) +
                    "&redirect_uri=" + formEncode(redirectUrl);

            String response = sendTokenRequest(requestBody, clientId, clientSecret);

            String refreshToken = extractRefreshToken(response);
            createProperty(tenant, XeroUtils.XERO_REFRESH_TOKEN, refreshToken);

            String accessToken = extractAccessToken(response);
            createProperty(tenant, XeroUtils.XERO_ACCESS_TOKEN, accessToken);

            List<XeroConnectionDto> connections = getConnections(accessToken);
            XeroConnectionDto selectedConnection = selectConnectionForTenant(connections);
            boolean selectionRequired = false;

            if (selectedConnection == null) {
                if (connections.isEmpty()) {
                    throw new IllegalStateException("No Xero organisations were returned for the authorised user.");
                }
                selectionRequired = true;
                settingService.upsertSetting("INTEGRATION-STATUS", "XERO", "PENDING_ORGANISATION_SELECTION");
                settingService.upsertSetting("INVOICE-INTEGRATION-ENABLED", "XERO", "false");
            } else {
                createProperty(tenant, XeroUtils.XERO_TENANT_ID, selectedConnection.getTenantId());
                settingService.upsertSetting("INTEGRATION-STATUS", "XERO", "AUTHORISED");
            }

            String expiresAt = String.valueOf(System.currentTimeMillis() + (1800 * 1000));
            createProperty(tenant, XeroUtils.XERO_EXPIRE_AT, expiresAt);

            return XeroOAuthResult.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .selectedTenantId(selectedConnection == null ? null : selectedConnection.getTenantId())
                    .selectedTenantName(selectedConnection == null ? null : selectedConnection.getTenantName())
                    .connections(connections)
                    .organisationSelectionRequired(selectionRequired)
                    .message(selectionRequired
                            ? "Multiple Xero organisations were returned. Return to MAWA Settings > XERO and choose the correct organisation."
                            : "Xero authorisation completed successfully.")
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String refreshAccessToken(String tenant) throws IOException {
        if (!isBlank(tenant)) {
            TenantContext.setCurrentTenant(tenant);
        }
        String lockKey = isBlank(tenant) ? "__default__" : tenant.trim().toLowerCase();
        Object lock = REFRESH_LOCKS.computeIfAbsent(lockKey, key -> new Object());

        synchronized (lock) {
            JSONObject jsonObject = tenantProperties(tenant);
            String currentAccessToken = getXeroProperty(jsonObject, XeroUtils.XERO_ACCESS_TOKEN);
            String currentExpiresAt = getXeroProperty(jsonObject, XeroUtils.XERO_EXPIRE_AT);
            if (!isBlank(currentAccessToken) && !isExpiredOrNearExpiry(currentExpiresAt)) {
                return currentAccessToken;
            }

            String refreshToken = requiredXeroProperty(jsonObject, XeroUtils.XERO_REFRESH_TOKEN);
            String clientId = requiredXeroProperty(jsonObject, XeroUtils.XERO_CLIENT_ID);
            String clientSecret = requiredXeroProperty(jsonObject, XeroUtils.XERO_CLIENT_SECRET);

            try {
                return exchangeAndPersistRefreshToken(tenant, refreshToken, clientId, clientSecret);
            } catch (IOException firstFailure) {
                if (!containsInvalidGrant(firstFailure)) {
                    throw firstFailure;
                }

                // Another request or Cloud Run instance may have rotated the one-time Xero
                // refresh token while this request was in flight. Re-read the latest values
                // and reuse the new access token, or retry once with the newly stored token.
                JSONObject latestProperties = tenantProperties(tenant);
                String latestAccessToken = getXeroProperty(latestProperties, XeroUtils.XERO_ACCESS_TOKEN);
                String latestExpiresAt = getXeroProperty(latestProperties, XeroUtils.XERO_EXPIRE_AT);
                if (!isBlank(latestAccessToken) && !isExpiredOrNearExpiry(latestExpiresAt)) {
                    return latestAccessToken;
                }

                String latestRefreshToken = requiredXeroProperty(latestProperties, XeroUtils.XERO_REFRESH_TOKEN);
                if (!refreshToken.equals(latestRefreshToken)) {
                    return exchangeAndPersistRefreshToken(tenant, latestRefreshToken, clientId, clientSecret);
                }
                throw firstFailure;
            }
        }
    }

    private String exchangeAndPersistRefreshToken(String tenant,
                                                  String refreshToken,
                                                  String clientId,
                                                  String clientSecret) throws IOException {
        String requestBody = "grant_type=refresh_token&refresh_token=" + formEncode(refreshToken);
        String response = sendTokenRequest(requestBody, clientId, clientSecret);

        String rotatedRefreshToken = extractRefreshToken(response);
        createProperty(tenant, XeroUtils.XERO_REFRESH_TOKEN, rotatedRefreshToken);

        String accessToken = extractAccessToken(response);
        createProperty(tenant, XeroUtils.XERO_ACCESS_TOKEN, accessToken);

        String expiresAt = String.valueOf(System.currentTimeMillis() + (1800 * 1000));
        createProperty(tenant, XeroUtils.XERO_EXPIRE_AT, expiresAt);
        return accessToken;
    }

    private boolean containsInvalidGrant(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("invalid_grant")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String formEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Returns the still-valid access token captured during OAuth before using the refresh token.
     *
     * This is important during the multi-organisation selection flow. The OAuth callback has just
     * received a valid access token and refresh token. If the UI first loads /connections and then
     * saves the selected organisation, refreshing on both calls can rotate the Xero refresh token
     * unnecessarily and can fail with invalid_grant when an old/PENDING token is still configured.
     */
    private String getValidAccessToken(String tenant) throws IOException {
        if (!isBlank(tenant)) {
            TenantContext.setCurrentTenant(tenant);
        }
        JSONObject jsonObject = tenantProperties(tenant);
        String accessToken = getXeroProperty(jsonObject, XeroUtils.XERO_ACCESS_TOKEN);
        String expiresAt = getXeroProperty(jsonObject, XeroUtils.XERO_EXPIRE_AT);

        if (!isBlank(accessToken) && !isExpiredOrNearExpiry(expiresAt)) {
            return accessToken;
        }

        return refreshAccessToken(tenant);
    }

    private boolean isExpiredOrNearExpiry(String expiresAt) {
        if (isBlank(expiresAt)) {
            return true;
        }
        try {
            long expiry = Long.parseLong(expiresAt.trim());
            return expiry <= (System.currentTimeMillis() + 60_000L);
        } catch (NumberFormatException ex) {
            return true;
        }
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

        int responseCode = connection.getResponseCode();
        if (responseCode >= 300) {
            String errorResponse = readErrorStream(connection);
            throw new IOException(String.format("Request failed with code: %d. Response: %s", responseCode, errorResponse));
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private static String extractAccessToken(String jsonResponse) {
        try {
            JsonNode jsonNode = new ObjectMapper().readTree(jsonResponse);
            return jsonNode.get("access_token").asText();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to parse Xero access token", e);
        }
    }

    private static String extractRefreshToken(String jsonResponse) {
        try {
            JsonNode jsonNode = new ObjectMapper().readTree(jsonResponse);
            return jsonNode.get("refresh_token").asText();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to parse Xero refresh token", e);
        }
    }

    public static String sendGetXeroTenantIdRequest(String accessToken) {
        try {
            List<XeroConnectionDto> connections = parseConnections(sendGetXeroConnectionsRequest(accessToken));
            return connections.isEmpty() ? null : connections.get(0).getTenantId();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<XeroConnectionDto> getConnectionsForCurrentTenant() throws IOException {
        String tenant = TenantContext.getCurrentTenant();
        String accessToken = getValidAccessToken(tenant);
        return getConnections(accessToken);
    }

    public List<XeroConnectionDto> getConnections(String accessToken) throws IOException {
        return parseConnections(sendGetXeroConnectionsRequest(accessToken));
    }

    public XeroConnectionDto selectXeroTenant(String tenant, String selectedTenantId) throws IOException {
        if (isBlank(tenant)) {
            throw new IllegalStateException("Tenant is required to select Xero organisation");
        }
        if (isBlank(selectedTenantId)) {
            throw new IllegalArgumentException("Xero tenantId is required");
        }
        TenantContext.setCurrentTenant(tenant);
        List<XeroConnectionDto> connections = getConnectionsForCurrentTenant();
        XeroConnectionDto selected = connections.stream()
                .filter(connection -> selectedTenantId.equalsIgnoreCase(connection.getTenantId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Selected Xero organisation is not available for the authorised Xero user"));

        createProperty(tenant, XeroUtils.XERO_TENANT_ID, selected.getTenantId());
        settingService.upsertSetting("INTEGRATION-STATUS", "XERO", "AUTHORISED");
        settingService.upsertSetting("INVOICE-INTEGRATION-ENABLED", "XERO", "true");
        return selected;
    }

    private XeroConnectionDto selectConnectionForTenant(List<XeroConnectionDto> connections) {
        if (connections == null || connections.isEmpty()) {
            return null;
        }

        String expectedTenantId = firstNonBlank(
                settingService.getSetting("TENANT-ID-EXPECTED", "XERO"),
                settingService.getSetting("XERO-TENANT-ID-EXPECTED", "XERO")
        );
        if (!isBlank(expectedTenantId)) {
            return connections.stream()
                    .filter(connection -> expectedTenantId.equalsIgnoreCase(connection.getTenantId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Configured XERO TENANT-ID-EXPECTED was not found in the authorised Xero organisations"));
        }

        return connections.size() == 1 ? connections.get(0) : null;
    }

    private static String sendGetXeroConnectionsRequest(String accessToken) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL("https://api.xero.com/connections");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode >= 300) {
                throw new IOException("Failed to retrieve Xero connections. HTTP Code: " + responseCode + ". Response: " + readErrorStream(connection));
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static List<XeroConnectionDto> parseConnections(String jsonResponse) throws IOException {
        List<XeroConnectionDto> connections = new ArrayList<>();
        JsonNode root = new ObjectMapper().readTree(jsonResponse);
        if (!root.isArray()) {
            return connections;
        }
        for (JsonNode node : root) {
            connections.add(XeroConnectionDto.builder()
                    .id(text(node, "id"))
                    .tenantId(text(node, "tenantId"))
                    .tenantName(text(node, "tenantName"))
                    .tenantType(text(node, "tenantType"))
                    .createdDateUtc(text(node, "createdDateUtc"))
                    .updatedDateUtc(text(node, "updatedDateUtc"))
                    .build());
        }
        return connections;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    public void createProperty(String tenant, String property, String value) {
        if (isBlank(tenant)) {
            throw new IllegalStateException("Tenant is required when storing Xero property " + property);
        }
        TenantContext.setCurrentTenant(tenant);

        JSONObject properties = tenantProperties(tenant);
        String secretReference = findSecretReferenceForProperty(properties, property);
        if (!isBlank(secretReference)) {
            gcpTenantSecretService.addSecretVersion(secretReference, value);
            return;
        }

        TenantPropertyDto tenantPropertyDto = new TenantPropertyDto();
        tenantPropertyDto.setTenant(tenant);
        tenantPropertyDto.setProperty(property);
        tenantPropertyDto.setValue(value);
        tenantAdminService.addTenantProperty(tenant, tenantPropertyDto);
    }

    public String updateProperty(String tenant, String setting) {
        tenantAdminService.getTenantProperties(tenant);
        return null;
    }

    public String checkXeroInfo() {
        String tenant = TenantContext.getCurrentTenant();
        JSONObject currentTenantProperties = tenantProperties(tenant);

        String refreshToken = getXeroProperty(currentTenantProperties, XeroUtils.XERO_REFRESH_TOKEN);
        String tenantId = getXeroProperty(currentTenantProperties, XeroUtils.XERO_TENANT_ID);
        if (!isBlank(refreshToken) && !isBlank(tenantId)) {
            return tenant;
        }

        String serviceProviderTenant = getXeroProperty(currentTenantProperties, XeroUtils.XERO_MAWA_SERVICE_PROVIDER_LINK);
        if (!isBlank(serviceProviderTenant)) {
            JSONObject serviceProviderProperties = tenantProperties(serviceProviderTenant);
            String serviceProviderRefreshToken = getXeroProperty(serviceProviderProperties, XeroUtils.XERO_REFRESH_TOKEN);
            String serviceProviderTenantId = getXeroProperty(serviceProviderProperties, XeroUtils.XERO_TENANT_ID);
            if (!isBlank(serviceProviderRefreshToken) && !isBlank(serviceProviderTenantId)) {
                return serviceProviderTenant;
            }
        }

        return null;
    }

    public String getXeroProperty(String tenant, String property) {
        if (!isBlank(tenant)) {
            TenantContext.setCurrentTenant(tenant);
        }
        return getXeroProperty(tenantProperties(tenant), property);
    }

    public String getXeroProperty(String tenant, String property, String defaultValue) {
        if (!isBlank(tenant)) {
            TenantContext.setCurrentTenant(tenant);
        }
        String value = getXeroProperty(tenantProperties(tenant), property);
        return isBlank(value) ? defaultValue : value;
    }

    private String getXeroProperty(JSONObject properties, String property) {
        String value = gcpTenantSecretService.resolveTenantProperty(properties, property);
        if (!isBlank(value)) {
            return value;
        }

        value = gcpTenantSecretService.resolveSetting(toXeroSettingAttribute(property), "XERO");
        if (!isBlank(value)) {
            return value;
        }

        return gcpTenantSecretService.resolveSetting(property, "XERO");
    }

    private String findSecretReferenceForProperty(JSONObject properties, String property) {
        String secretReference = gcpTenantSecretService.findTenantPropertySecretReference(properties, property);
        String directReference = properties == null ? null : properties.optString(property, null);
        if (isBlank(secretReference) && gcpTenantSecretService.isSecretReference(directReference)) {
            secretReference = directReference;
        }
        if (!isBlank(secretReference)) {
            return secretReference;
        }

        secretReference = gcpTenantSecretService.findSettingSecretReference(toXeroSettingAttribute(property), "XERO");
        if (!isBlank(secretReference)) {
            return secretReference;
        }

        secretReference = gcpTenantSecretService.findSettingSecretReference(property, "XERO");
        if (!isBlank(secretReference)) {
            return secretReference;
        }

        String settingValue = settingService.getSetting(toXeroSettingAttribute(property), "XERO");
        if (gcpTenantSecretService.isSecretReference(settingValue)) {
            return settingValue;
        }

        settingValue = settingService.getSetting(property, "XERO");
        if (gcpTenantSecretService.isSecretReference(settingValue)) {
            return settingValue;
        }

        return null;
    }

    private String toXeroSettingAttribute(String property) {
        if (isBlank(property)) {
            return property;
        }
        return property.startsWith("XERO-") ? property.substring("XERO-".length()) : property;
    }

    private String requiredXeroProperty(JSONObject properties, String property) {
        String value = getXeroProperty(properties, property);
        if (isBlank(value)) {
            throw new IllegalStateException("Missing required Xero configuration: " + property + ". Store a Google Secret Manager reference in Group XERO or configure the tenant property.");
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

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim());
    }

    private static String readErrorStream(HttpURLConnection connection) throws IOException {
        InputStream stream = connection.getErrorStream();
        if (stream == null) {
            return "";
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }

    @Data
    @Builder
    public static class XeroOAuthResult {
        private String accessToken;
        private String refreshToken;
        private String selectedTenantId;
        private String selectedTenantName;
        private boolean organisationSelectionRequired;
        private List<XeroConnectionDto> connections;
        private String message;
    }
}
