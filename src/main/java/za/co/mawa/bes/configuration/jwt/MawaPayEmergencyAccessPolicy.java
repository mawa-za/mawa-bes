package za.co.mawa.bes.configuration.jwt;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Operator-controlled, temporary acceptance of signed expired access JWTs on
 * MawaPay-only operations. No token / invalid signature never enters this path.
 */
@Component
public class MawaPayEmergencyAccessPolicy {
    private static final long MAX_GRACE_HOURS = 48;

    private final boolean enabled;
    private final Set<String> allowedTenantIds;
    private final Instant sunset;
    private final Duration grace;

    public MawaPayEmergencyAccessPolicy(
            @Value("${mawa.pay.emergency.access.grace.enabled:false}") boolean enabled,
            @Value("${mawa.pay.emergency.access.grace.tenant.ids:}") String tenantIds,
            @Value("${mawa.pay.emergency.access.grace.until:}") String sunset,
            @Value("${mawa.pay.emergency.access.grace.hours:48}") long graceHours) {
        this.enabled = enabled;
        this.allowedTenantIds = Arrays.stream(tenantIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        this.sunset = parseInstant(sunset);
        this.grace = graceHours > 0 && graceHours <= MAX_GRACE_HOURS
                ? Duration.ofHours(graceHours) : Duration.ZERO;
    }

    public boolean permits(Claims claims, String method, String path) {
        Instant now = Instant.now();
        if (!enabled || claims == null || sunset == null || !now.isBefore(sunset)
                || allowedTenantIds.isEmpty() || grace.isZero()
                || !isMawaPayOperation(method, path)) {
            return false;
        }

        String tenant = claims.get("tenant-id", String.class);
        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();
        if (!"access".equals(claims.get("token_type", String.class))
                || tenant == null || !allowedTenantIds.contains(tenant)
                || !tenant.equals(claims.getAudience())
                || claims.getSubject() == null || claims.getSubject().isBlank()
                || issuedAt == null || expiration == null
                || !issuedAt.before(expiration)
                || issuedAt.toInstant().isAfter(now)
                || !expiration.toInstant().isBefore(now)
                || expiration.toInstant().isBefore(now.minus(grace))) {
            return false;
        }
        // Admin handoff/platform sessions cannot acquire extra lifetime.
        return !Boolean.TRUE.equals(claims.get("platform_session", Boolean.class))
                && claims.get("platform_user_id") == null;
    }

    /**
     * Exact method/path exceptions for operations called by the installed APK.
     * In particular, nothing under /v2/admin or general payment administration.
     */
    static boolean isMawaPayOperation(String method, String path) {
        if (method == null || path == null) return false;
        String verb = method.toUpperCase(Locale.ROOT);
        if ("GET".equals(verb)) {
            return path.equals("/setting")
                    || path.equals("/v2/pay-app/members")
                    || path.equals("/v2/pay-app/master-data/members")
                    || path.equals("/v2/membership/master-data")
                    || path.equals("/v2/membership/master-data/snapshot")
                    || path.equals("/v2/membership/master-data/changes")
                    || path.equals("/v2/group-society/master-data")
                    || path.equals("/v2/membership/plans")
                    || path.equals("/v2/membership/all")
                    || path.equals("/v2/membership/by-number")
                    || path.equals("/v2/membership")
                    || path.matches("/v2/membership/[^/]+")
                    || path.equals("/v2/partner")
                    || path.equals("/v2/partner/identity")
                    || path.matches("/v2/partner/[^/]+")
                    || path.equals("/v2/field/option")
                    || path.equals("/v2/cashup")
                    || path.equals("/v2/cashup/active")
                    || path.matches("/v2/cashup/device/[^/]+")
                    || path.matches("/v2/cashup/[^/]+")
                    || path.equals("/v2/number-allocation/active")
                    || path.matches("/v2/memberships/[^/]+/premiums(/unpaid)?")
                    || path.matches("/v2/device-sync/user-payment-profile/[^/]+")
                    || path.equals("/v2/device-sync/submissions/reconcile")
                    || path.matches("/v2/payment-batches/[^/]+/cancellation-status")
                    || path.equals("/v2/pay-app/manual-actions/reconcile")
                    || path.equals("/v2/card-terminals");
        }
        if ("POST".equals(verb)) {
            return path.equals("/v2/partner")
                    || path.equals("/v2/partner/identity")
                    || path.equals("/v2/membership")
                    || path.equals("/v2/cashup")
                    || path.equals("/v2/number-allocation/allocate")
                    || path.equals("/v2/sync/payment-batches/membership-premiums")
                    || path.equals("/v2/sync/payment-batches/group-society")
                    || path.equals("/v2/device-sync/submissions")
                    || path.equals("/v2/device-sync/submissions/reconcile")
                    || path.equals("/v2/device-sync/submissions/cancel-by-key")
                    || path.matches("/v2/device-sync/submissions/[^/]+/process")
                    || path.equals("/v2/pay-app/manual-actions")
                    || path.matches("/v2/device-sync/submissions/[^/]+/cancel")
                    || path.matches("/v2/payment-batches/[^/]+/cancellation-request")
                    || path.equals("/pay-app/receipt-sync")
                    || path.equals("/v2/device-crash-logs");
        }
        return false;
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Instant.parse(value.trim()); }
        catch (DateTimeException ignored) { return null; }
    }
}
