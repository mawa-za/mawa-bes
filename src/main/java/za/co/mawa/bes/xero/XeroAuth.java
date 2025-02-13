package za.co.mawa.bes.xero;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class XeroAuth {
    private static final String CLIENT_ID = "71674DC318314EBAAF07D16186E42D02";
    private static final String CLIENT_SECRET = "c90a_5H72_f0DXSnQroKDcp1pedaI9nVpOk4NayCi7viLyRO";
    private static final String REDIRECT_URI = "http://localhost:8080/xero/callback";
    private static final String TOKEN_URL = "https://identity.xero.com/connect/token";
    private static final String API_URL = "https://api.xero.com/api.xro/2.0/Invoices";
    private static String refreshToken = "your_stored_refresh_token"; // Store securely


    public static String getInitialTokens(String authorizationCode) throws IOException {
        try {
            String requestBody = "grant_type=authorization_code" +
                    "&code=" + authorizationCode +
                    "&redirect_uri=" + REDIRECT_URI;

            String response = sendTokenRequest(requestBody);
            //            createInvoice(accessToken);
//            System.out.println("Initial Token Response: " + response);
//            System.out.println("Token " + accessToken);
            return extractAccessToken(response);
        } catch (Exception e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }

    public static String refreshAccessToken() throws IOException {
        String requestBody = "grant_type=refresh_token&refresh_token=" + refreshToken;

        String response = sendTokenRequest(requestBody);
        System.out.println("New Token Response: " + response);

        String accessToken = extractAccessToken(response);
        refreshToken = extractRefreshToken(response); // Update refresh token

        return accessToken;
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

    public static String getXeroTenantId(String accessToken) {
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

