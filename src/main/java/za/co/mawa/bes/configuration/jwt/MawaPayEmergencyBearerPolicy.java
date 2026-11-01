package za.co.mawa.bes.configuration.jwt;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Time-bounded incident bridge for already-installed MawaPay clients that
 * still send a Bearer JWT, including expired ones. It must remain
 * disabled except during the explicitly authorized emergency period.
 */
@Component
public class MawaPayEmergencyBearerPolicy {
    private final boolean enabled;
    private final Set<String> tenantIds;
    private final Instant until;

    public MawaPayEmergencyBearerPolicy(
            @Value("${mawa.pay.emergency.bearer.enabled:false}") boolean enabled,
            @Value("${mawa.pay.emergency.bearer.tenant.ids:}") String tenantIds,
            @Value("${mawa.pay.emergency.bearer.until:}") String until) {
        this.enabled = enabled;
        this.tenantIds = Arrays.stream(tenantIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        this.until = parseInstant(until);
    }

    public String approvedTenant(HttpServletRequest request) {
        if (request == null || !MawaPayEmergencyAccessPolicy.isMawaPayOperation(
                request.getMethod(), request.getServletPath())
                || !looksLikeJwt(bearerToken(request))) {
            return null;
        }

        return eligibleTenant(request);
    }

    /** Allows the refresh endpoint to keep a failed refresh non-destructive. */
    public boolean protectRefreshFailure(HttpServletRequest request, String refreshToken) {
        return request != null && "POST".equalsIgnoreCase(request.getMethod())
                && "/v2/refresh-token".equals(request.getServletPath())
                && looksLikeJwt(refreshToken) && eligibleTenant(request) != null;
    }

    private static String bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        return header.substring(7).trim();
    }

    private static boolean looksLikeJwt(String token) {
        if (token == null || token.isBlank()) return false;
        String[] parts = token.trim().split("\\.", -1);
        return parts.length == 3 && !parts[0].isBlank()
                && !parts[1].isBlank() && !parts[2].isBlank();
    }

    private String eligibleTenant(HttpServletRequest request) {
        if (!enabled || until == null || !Instant.now().isBefore(until)
                || tenantIds.isEmpty()) return null;

        // MawaPay's existing Dio client sends the tenant ID on every request.
        // This is a selector, NOT authentication: any caller can supply it.
        String tenant = request.getHeader("X-TenantID");
        if (tenant == null || tenant.isBlank()) {
            tenant = request.getHeader("X-Tenant-Id");
        }
        if (tenant == null) return null;
        tenant = tenant.trim();
        return tenantIds.contains(tenant) ? tenant : null;
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Instant.parse(value.trim()); }
        catch (DateTimeException ignored) { return null; }
    }
}
