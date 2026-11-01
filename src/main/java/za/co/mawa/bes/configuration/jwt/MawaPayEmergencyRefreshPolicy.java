package za.co.mawa.bes.configuration.jwt;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Temporary, fail-closed recovery for recently expired *signed refresh* JWTs.
 * This never grants anonymous access to MawaPay endpoints.
 *
 * Enabling requires an explicit tenant allowlist and an absolute UTC sunset.
 * Remove the feature after the installed MawaPay clients have been replaced.
 */
@Component
public class MawaPayEmergencyRefreshPolicy {
    private static final Duration MAX_GRACE = Duration.ofHours(24);

    private final boolean enabled;
    private final Set<String> allowedTenantIds;
    private final Instant sunset;
    private final Duration grace;

    public MawaPayEmergencyRefreshPolicy(
            @Value("${mawa.pay.emergency.refresh.grace.enabled:false}") boolean enabled,
            @Value("${mawa.pay.emergency.refresh.grace.tenant.ids:}") String allowedTenantIds,
            @Value("${mawa.pay.emergency.refresh.grace.until:}") String sunset,
            @Value("${mawa.pay.emergency.refresh.grace.hours:24}") long graceHours) {
        this.enabled = enabled;
        this.allowedTenantIds = Arrays.stream(allowedTenantIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        this.sunset = parseInstant(sunset);
        this.grace = graceHours > 0 && graceHours <= MAX_GRACE.toHours()
                ? Duration.ofHours(graceHours) : Duration.ZERO;
    }

    public boolean permits(Claims claims) {
        Instant now = Instant.now();
        if (!enabled || sunset == null || !now.isBefore(sunset)
                || grace.isZero() || allowedTenantIds.isEmpty() || claims == null) {
            return false;
        }
        String tenant = claims.get("tenant-id", String.class);
        Date expiration = claims.getExpiration();
        Date issuedAt = claims.getIssuedAt();
        if (!"refresh".equals(claims.get("token_type", String.class))
                || tenant == null || !allowedTenantIds.contains(tenant)
                || !tenant.equals(claims.getAudience())
                || claims.getSubject() == null || claims.getSubject().isBlank()
                || expiration == null || issuedAt == null
                || !issuedAt.before(expiration)
                || issuedAt.toInstant().isAfter(now)
                || !expiration.toInstant().isBefore(now)
                || expiration.toInstant().isBefore(now.minus(grace))) {
            return false;
        }
        // Elevated platform handoff sessions must never be extended by emergency recovery.
        return !Boolean.TRUE.equals(claims.get("platform_session", Boolean.class))
                && claims.get("platform_user_id") == null;
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeException ex) {
            return null; // Incorrect operator configuration must fail closed.
        }
    }
}
