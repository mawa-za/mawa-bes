package za.co.mawa.bes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class PurplePlatformClient {
    private static final String INTERNAL_TOKEN_HEADER = "X-Mawa-Internal-Token";

    private final ObjectMapper objectMapper;

    @Value("${purple.api.url:}")
    private String purpleApiUrl;
    @Value("${mawa.internal.service-token:}")
    private String internalServiceToken;

    public PurplePlatformClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String upsertProvider(String tenant, Object request) {
        if (!StringUtils.hasText(tenant)) throw new IllegalArgumentException("Tenant is required");
        if (!StringUtils.hasText(purpleApiUrl)) throw new IllegalStateException("purple.api.url is not configured");
        if (!StringUtils.hasText(internalServiceToken)) throw new IllegalStateException("mawa.internal.service-token is not configured");

        HttpURLConnection connection = null;
        try {
            String base = purpleApiUrl.endsWith("/") ? purpleApiUrl.substring(0, purpleApiUrl.length() - 1) : purpleApiUrl;
            String path = "/internal/erp/purple/providers/" + URLEncoder.encode(tenant, StandardCharsets.UTF_8);
            connection = (HttpURLConnection) new URL(base + path).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty(INTERNAL_TOKEN_HEADER, internalServiceToken);
            byte[] payload = objectMapper.writeValueAsBytes(request);
            connection.setFixedLengthStreamingMode(payload.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload);
            }
            int status = connection.getResponseCode();
            String response = readBody(status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream());
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("purple-bes provider sync failed with HTTP " + status + ": " + response);
            }
            return response;
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to synchronize Purple provider directory: " + ex.getMessage(), ex);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String readBody(InputStream inputStream) throws Exception {
        if (inputStream == null) return "";
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) builder.append(line);
        }
        return builder.toString();
    }
}
