package za.co.mawa.bes.billing;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
public class CloudRunIdTokenProvider {
    private final boolean enabled;
    private final String audience;
    private volatile IdTokenCredentials credentials;

    public CloudRunIdTokenProvider(
            @Value("${mawa.billing.cloud-run-id-token-enabled:false}") boolean enabled,
            @Value("${mawa.billing.base-url:http://localhost:8085}") String audience) {
        this.enabled = enabled;
        this.audience = stripTrailingSlash(audience);
    }

    public void apply(HttpHeaders headers) {
        if (!enabled) return;
        if (!StringUtils.hasText(audience) || audience.startsWith("http://localhost")) return;
        try {
            IdTokenCredentials current = credentials;
            if (current == null) {
                synchronized (this) {
                    current = credentials;
                    if (current == null) {
                        GoogleCredentials applicationDefault = GoogleCredentials.getApplicationDefault();
                        if (!(applicationDefault instanceof IdTokenProvider provider)) {
                            throw new IllegalStateException("The runtime credentials cannot issue Cloud Run ID tokens");
                        }
                        current = IdTokenCredentials.newBuilder()
                                .setIdTokenProvider(provider)
                                .setTargetAudience(audience)
                                .build();
                        credentials = current;
                    }
                }
            }
            current.refreshIfExpired();
            headers.setBearerAuth(current.getAccessToken().getTokenValue());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to obtain a Cloud Run identity token for billing", exception);
        }
    }

    private static String stripTrailingSlash(String value) {
        if (value == null) return "";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
