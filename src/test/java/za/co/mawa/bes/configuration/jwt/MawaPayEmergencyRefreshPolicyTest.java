package za.co.mawa.bes.configuration.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MawaPayEmergencyRefreshPolicyTest {
    private final String futureSunset = Instant.now().plus(2, ChronoUnit.DAYS).toString();

    private Claims expiredRefresh(String tenant, long secondsExpired) {
        Claims claims = Jwts.claims();
        claims.setSubject("cashier");
        claims.setAudience(tenant);
        claims.setIssuedAt(Date.from(Instant.now().minus(30, ChronoUnit.DAYS)));
        claims.setExpiration(Date.from(Instant.now().minusSeconds(secondsExpired)));
        claims.put("tenant-id", tenant);
        claims.put("token_type", "refresh");
        return claims;
    }

    @Test void disabledByDefault() {
        var policy = new MawaPayEmergencyRefreshPolicy(false, "tenant-a", futureSunset, 24);
        assertFalse(policy.permits(expiredRefresh("tenant-a", 3600)));
    }

    @Test void requiresAllowlistedTenantAndExplicitSunset() {
        var policy = new MawaPayEmergencyRefreshPolicy(true, "tenant-a", futureSunset, 24);
        assertTrue(policy.permits(expiredRefresh("tenant-a", 3600)));
        assertFalse(policy.permits(expiredRefresh("tenant-b", 3600)));
        assertFalse(new MawaPayEmergencyRefreshPolicy(true, "tenant-a", "", 24)
                .permits(expiredRefresh("tenant-a", 3600)));
        assertFalse(new MawaPayEmergencyRefreshPolicy(true, "tenant-a", "bad-time", 24)
                .permits(expiredRefresh("tenant-a", 3600)));
    }

    @Test void refusesExpiredSunsetOrOutOfRangeGrace() {
        assertFalse(new MawaPayEmergencyRefreshPolicy(true, "tenant-a",
                Instant.now().minusSeconds(10).toString(), 24)
                .permits(expiredRefresh("tenant-a", 3600)));
        assertFalse(new MawaPayEmergencyRefreshPolicy(true, "tenant-a", futureSunset, 24)
                .permits(expiredRefresh("tenant-a", 25 * 3600)));
        assertFalse(new MawaPayEmergencyRefreshPolicy(true, "tenant-a", futureSunset, 72)
                .permits(expiredRefresh("tenant-a", 3600)));
    }

    @Test void refusesAccessTokenAndElevatedPlatformSession() {
        var policy = new MawaPayEmergencyRefreshPolicy(true, "tenant-a", futureSunset, 24);
        Claims access = expiredRefresh("tenant-a", 3600);
        access.put("token_type", "access");
        assertFalse(policy.permits(access));

        Claims platform = expiredRefresh("tenant-a", 3600);
        platform.put("platform_session", true);
        assertFalse(policy.permits(platform));

        Claims mismatchedAudience = expiredRefresh("tenant-a", 3600);
        mismatchedAudience.setAudience("tenant-b");
        assertFalse(policy.permits(mismatchedAudience));
    }
}
