package za.co.mawa.bes.billing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class BillingEntitlementClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Mawa-Internal-Token";

    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl;
    private final String internalServiceToken;

    public BillingEntitlementClient(
            @Value("${mawa.billing.base-url:http://localhost:8085}") String baseUrl,
            @Value("${mawa.internal.service-token:}") String internalServiceToken) {
        this.baseUrl = baseUrl;
        this.internalServiceToken = internalServiceToken;
    }

    public boolean isEnabled(String tenantId, String moduleCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(INTERNAL_TOKEN_HEADER, internalServiceToken);

        try {
            Map<?, ?> body = restTemplate.exchange(
                    baseUrl + "/tenants/" + tenantId + "/entitlements/" + moduleCode,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            ).getBody();

            return body != null && Boolean.TRUE.equals(body.get("enabled"));
        } catch (Exception exception) {
            // Transitional fail-open behaviour until every tenant has billing entitlements.
            return true;
        }
    }
}
