package za.co.mawa.bes.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.dao.TenantDao;
import za.co.mawa.bes.dto.JwtRequest;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.dto.TenantPropertyDto;
import za.co.mawa.bes.entity.TenantEntity;
import za.co.mawa.bes.entity.TenantPropertyEntity;
import za.co.mawa.bes.entity.TenantPropertyPKEntity;
import za.co.mawa.bes.repository.TenantPropertyRepository;
import za.co.mawa.bes.repository.TenantRepository;
import za.co.mawa.bes.utils.Status;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Reads authoritative tenant metadata from mawa-admin-bes.
 *
 * Machine-to-machine calls prefer the shared internal service token. The old
 * admin username/password flow remains as a temporary compatibility fallback,
 * but it is no longer required when MAWA_INTERNAL_SERVICE_TOKEN is configured.
 */
@Slf4j
@Component
public class TenantAdminService implements TenantDao {

    private static final String INTERNAL_TOKEN_HEADER = "X-Mawa-Internal-Token";
    private static final String INTERNAL_TENANTS_PATH = "/internal/erp/tenants";

    @Autowired private TenantRepository tenantRepository;
    @Autowired private TenantPropertyRepository tenantPropertyRepository;

    @Value("${mawa.admin.api.url}")
    private String adminApiUrl;
    @Value("${mawa.admin.api.username:admin}")
    private String adminApiUsername;
    @Value("${mawa.admin.api.password:}")
    private String adminApiPassword;
    @Value("${mawa.internal.service-token:}")
    private String internalServiceToken;
    @Value("${mawa.admin.api.connect-timeout-ms:5000}")
    private int connectTimeoutMs;
    @Value("${mawa.admin.api.read-timeout-ms:15000}")
    private int readTimeoutMs;
    @Value("${mawa.admin.api.retry-attempts:3}")
    private int retryAttempts;
    @Value("${mawa.admin.api.retry-backoff-ms:250}")
    private long retryBackoffMs;
    @Value("${mawa.admin.api.tenant-cache-ttl-ms:60000}")
    private long tenantCacheTtlMs;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private volatile List<TenantDto> cachedTenants = List.of();
    private volatile long cachedTenantsAt;
    private volatile long lastFailureLoggedAt;

    @Override
    public TenantDto create(TenantDto tenantDto) throws Exception {
        try {
            TenantEntity tenantEntity = new TenantEntity();
            tenantEntity.setName(tenantDto.getName());
            tenantEntity.setHost(tenantDto.getHost());
            tenantEntity.setUrl(tenantDto.getUrl());
            tenantEntity.setStatus(Status.ACTIVE);
            tenantDto.setId(tenantRepository.save(tenantEntity).getId());
            return tenantDto;
        } catch (Exception exception) {
            throw new Exception("Unable to create tenant", exception);
        }
    }

    /**
     * Retained for compatibility with older callers. New internal calls do not
     * need a human/admin JWT.
     */
    public String getAdminToken() {
        if (!StringUtils.hasText(adminApiUsername) || !StringUtils.hasText(adminApiPassword)) {
            throw new AdminApiException("Legacy admin credentials are not configured");
        }
        JwtRequest tokenRequest = new JwtRequest(adminApiUsername, adminApiPassword);
        String body = execute("POST", "/v2/authenticate", tokenRequest, false);
        try {
            JsonNode json = objectMapper.readTree(body);
            String token = firstText(json, "accessToken", "token");
            if (!StringUtils.hasText(token)) {
                throw new AdminApiException("Admin authentication response did not contain an access token");
            }
            return token;
        } catch (IOException ex) {
            // Compatibility with very old admin deployments that returned the
            // token as a plain string rather than a JSON object.
            String token = stripJsonQuotes(body);
            if (StringUtils.hasText(token) && token.split("\\.").length == 3) {
                return token;
            }
            throw new AdminApiException("Unable to parse admin authentication response", ex);
        }
    }

    @Override
    public List<TenantDto> getAll() {
        long now = System.currentTimeMillis();
        List<TenantDto> current = cachedTenants;
        if (cachedTenantsAt > 0L && now - cachedTenantsAt < Math.max(1000L, tenantCacheTtlMs)) {
            return current;
        }

        synchronized (this) {
            now = System.currentTimeMillis();
            current = cachedTenants;
            if (cachedTenantsAt > 0L && now - cachedTenantsAt < Math.max(1000L, tenantCacheTtlMs)) {
                return current;
            }
            try {
                String body = internalIntegrationEnabled()
                        ? execute("GET", INTERNAL_TENANTS_PATH, null, true)
                        : executeLegacy("GET", "/tenant", null);
                List<TenantDto> tenants = objectMapper.readValue(body, new TypeReference<List<TenantDto>>() {});
                List<TenantDto> immutable = Collections.unmodifiableList(new ArrayList<>(tenants));
                cachedTenants = immutable;
                cachedTenantsAt = now;
                return immutable;
            } catch (Exception ex) {
                logAdminFailure(ex);
                if (cachedTenantsAt > 0L) {
                    return cachedTenants;
                }
                List<TenantDto> localFallback = loadLocalTenants();
                if (!localFallback.isEmpty()) {
                    return localFallback;
                }
                throw ex instanceof RuntimeException runtime
                        ? runtime
                        : new AdminApiException("Unable to load tenants from admin service", ex);
            }
        }
    }

    @Override
    public Properties getTenantProperties(String tenant) {
        String body = internalIntegrationEnabled()
                ? execute("GET", internalTenantPath(tenant) + "/properties", null, true)
                : executeLegacy("GET", "/tenant/" + encode(tenant) + "/property", null);
        try {
            Map<String, Object> values = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
            Properties properties = new Properties();
            values.forEach((key, value) -> {
                if (key != null && value != null) properties.setProperty(key, String.valueOf(value));
            });
            return properties;
        } catch (IOException ex) {
            throw new AdminApiException("Unable to parse tenant properties for " + tenant, ex);
        }
    }

    @Override
    public void addProperty(TenantPropertyDto propertyDto) {
        if (propertyDto == null) return;
        TenantPropertyPKEntity id = new TenantPropertyPKEntity();
        id.setTenant(propertyDto.getTenant());
        id.setProperty(propertyDto.getProperty());
        TenantPropertyEntity entity = new TenantPropertyEntity();
        entity.setTenantPropertyPKEntity(id);
        entity.setValue(propertyDto.getValue());
        tenantPropertyRepository.save(entity);
    }

    public String addTenantProperty(String tenant, TenantPropertyDto propertyDto) {
        if (propertyDto == null) throw new IllegalArgumentException("Property is required");
        propertyDto.setTenant(tenant);
        return internalIntegrationEnabled()
                ? execute("POST", internalTenantPath(tenant) + "/properties", propertyDto, true)
                : executeLegacy("POST", "/tenant/" + encode(tenant) + "/property", propertyDto);
    }

    public String getTenantProperty(String tenant) {
        return internalIntegrationEnabled()
                ? execute("GET", internalTenantPath(tenant) + "/properties", null, true)
                : executeLegacy("GET", "/tenant/" + encode(tenant) + "/property", null);
    }

    public String upsertPurpleProvider(String tenant, Object request) {
        if (!StringUtils.hasText(tenant)) throw new IllegalArgumentException("Tenant is required");
        return execute("POST", "/internal/erp/purple/providers/" + encode(tenant), request, true);
    }

    public void invalidateTenantCache() {
        cachedTenantsAt = 0L;
    }

    private String internalTenantPath(String tenant) {
        return INTERNAL_TENANTS_PATH + "/" + encode(tenant);
    }

    private boolean internalIntegrationEnabled() {
        return StringUtils.hasText(internalServiceToken);
    }

    private String executeLegacy(String method, String path, Object body) {
        String token = getAdminToken();
        return execute(method, path, body, false, token);
    }

    private String execute(String method, String path, Object body, boolean internal) {
        return execute(method, path, body, internal, null);
    }

    private String execute(String method, String path, Object body, boolean internal, String bearerToken) {
        int attempts = Math.max(1, retryAttempts);
        AdminApiException lastFailure = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return executeOnce(method, path, body, internal, bearerToken);
            } catch (AdminApiException ex) {
                lastFailure = ex;
                if (attempt >= attempts || !isRetryable(ex)) throw ex;
                log.warn("Transient admin API failure on {} {} (attempt {}/{}): {}",
                        method, path, attempt, attempts, ex.getMessage());
                sleepBeforeRetry(attempt);
            }
        }
        throw lastFailure == null
                ? new AdminApiException("Unable to call admin API " + method + " " + path)
                : lastFailure;
    }

    private String executeOnce(String method, String path, Object body, boolean internal, String bearerToken) {
        HttpURLConnection connection = null;
        try {
            URI base = URI.create(adminApiUrl.endsWith("/")
                    ? adminApiUrl.substring(0, adminApiUrl.length() - 1)
                    : adminApiUrl);
            connection = (HttpURLConnection) base.resolve(path).toURL().openConnection();
            connection.setConnectTimeout(Math.max(1000, connectTimeoutMs));
            connection.setReadTimeout(Math.max(1000, readTimeoutMs));
            connection.setRequestMethod(method);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Connection", "close");
            connection.setUseCaches(false);
            if (internal) {
                connection.setRequestProperty(INTERNAL_TOKEN_HEADER, internalServiceToken);
            } else if (StringUtils.hasText(bearerToken)) {
                connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
            }
            if (body != null) {
                connection.setDoOutput(true);
                byte[] payload = objectMapper.writeValueAsBytes(body);
                connection.setFixedLengthStreamingMode(payload.length);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(payload);
                }
            }

            int status = connection.getResponseCode();
            String responseBody = readBody(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
            if (status < 200 || status >= 300) {
                throw new AdminApiException("Admin API " + method + " " + path
                        + " failed with HTTP " + status
                        + (StringUtils.hasText(responseBody) ? ": " + responseBody : ""));
            }
            return responseBody;
        } catch (AdminApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AdminApiException("Unable to call admin API " + method + " " + path + ": " + ex.getMessage(), ex);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private boolean isRetryable(AdminApiException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof IOException) return true;
            cause = cause.getCause();
        }
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
        return message.contains("broken pipe")
                || message.contains("connection reset")
                || message.contains("timed out")
                || message.contains("unexpected end of file")
                || message.contains("http 429")
                || message.contains("http 502")
                || message.contains("http 503")
                || message.contains("http 504");
    }

    private void sleepBeforeRetry(int attempt) {
        long delay = Math.max(0L, retryBackoffMs) * Math.max(1, attempt);
        if (delay <= 0L) return;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AdminApiException("Interrupted while retrying the admin API", interrupted);
        }
    }

    private List<TenantDto> loadLocalTenants() {
        try {
            List<TenantDto> result = new ArrayList<>();
            for (TenantEntity entity : tenantRepository.findAll()) {
                TenantDto dto = new TenantDto();
                dto.setId(entity.getId());
                dto.setName(entity.getName());
                dto.setHost(entity.getHost());
                dto.setUrl(entity.getUrl());
                dto.setStatus(entity.getStatus());
                result.add(dto);
            }
            return Collections.unmodifiableList(result);
        } catch (RuntimeException ex) {
            log.warn("Unable to use local tenant fallback: {}", ex.getMessage());
            return List.of();
        }
    }

    private void logAdminFailure(Exception ex) {
        long now = System.currentTimeMillis();
        // Avoid flooding Cloud Logging from the 30-second scheduler while still
        // retaining a useful recurring signal during a prolonged outage.
        if (now - lastFailureLoggedAt > Duration.ofMinutes(5).toMillis()) {
            lastFailureLoggedAt = now;
            log.error("Unable to refresh tenant metadata from mawa-admin-bes; using cached/local data where available: {}",
                    ex.getMessage());
        } else {
            log.debug("Tenant metadata refresh still unavailable: {}", ex.getMessage());
        }
    }

    private String readBody(InputStream stream) throws IOException {
        if (stream == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
            return result.toString();
        }
    }

    private String firstText(JsonNode json, String... names) {
        if (json == null) return null;
        for (String name : names) {
            JsonNode node = json.get(name);
            if (node != null && StringUtils.hasText(node.asText())) return node.asText().trim();
        }
        return null;
    }

    private String stripJsonQuotes(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public static class AdminApiException extends RuntimeException {
        public AdminApiException(String message) { super(message); }
        public AdminApiException(String message, Throwable cause) { super(message, cause); }
    }
}
