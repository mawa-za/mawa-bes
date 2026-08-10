package za.co.mawa.bes.billing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BillingEntitlementClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Mawa-Internal-Token";

    private final RestTemplate restTemplate;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final String baseUrl;
    private final String internalServiceToken;
    private final Duration cacheDuration;
    private final CloudRunIdTokenProvider cloudRunIdTokenProvider;

    public BillingEntitlementClient(
            RestTemplateBuilder restTemplateBuilder,
            CloudRunIdTokenProvider cloudRunIdTokenProvider,
            @Value("${mawa.billing.base-url:http://localhost:8085}") String baseUrl,
            @Value("${mawa.internal.service-token:}") String internalServiceToken,
            @Value("${mawa.billing.entitlement-cache-seconds:30}") long cacheSeconds,
            @Value("${mawa.billing.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${mawa.billing.read-timeout-ms:3000}") long readTimeoutMs) {
        this.cloudRunIdTokenProvider = cloudRunIdTokenProvider;
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.internalServiceToken = internalServiceToken;
        this.cacheDuration = Duration.ofSeconds(Math.max(0, cacheSeconds));
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(Math.max(100, connectTimeoutMs)))
                .setReadTimeout(Duration.ofMillis(Math.max(100, readTimeoutMs)))
                .build();
    }

    public boolean isEnabled(String tenantId, String moduleCode) {
        String cacheKey = tenantId + "|" + moduleCode;
        CacheEntry cached = cache.get(cacheKey);
        Instant now = Instant.now();
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.enabled();
        }

        HttpHeaders headers = new HttpHeaders();
        cloudRunIdTokenProvider.apply(headers);
        if (StringUtils.hasText(internalServiceToken)) {
            headers.set(INTERNAL_TOKEN_HEADER, internalServiceToken);
        }

        try {
            String tenant = UriUtils.encodePathSegment(tenantId, StandardCharsets.UTF_8);
            String module = UriUtils.encodePathSegment(moduleCode, StandardCharsets.UTF_8);
            Map<?, ?> body = restTemplate.exchange(
                    baseUrl + "/tenants/" + tenant + "/entitlements/" + module,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            ).getBody();

            boolean enabled = body != null && Boolean.parseBoolean(String.valueOf(body.get("enabled")));
            cache.put(cacheKey, new CacheEntry(enabled, now.plus(cacheDuration)));
            return enabled;
        } catch (Exception exception) {
            // Use the last known decision when available. Otherwise remain fail-open
            // so a billing outage cannot take down tenant operations.
            return cached == null || cached.enabled();
        }
    }

    private static String stripTrailingSlash(String value) {
        if (value == null) return "";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record CacheEntry(boolean enabled, Instant expiresAt) {
    }
}
