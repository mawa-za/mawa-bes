package za.co.mawa.bes.configuration.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MawaPayEmergencyAccessPolicyTest {
    private final String futureSunset = Instant.now().plus(2, ChronoUnit.DAYS).toString();

    private Claims expiredAccess(long expiredSeconds) {
        Instant now = Instant.now();
        Claims claims = Jwts.claims();
        claims.setSubject("cashier");
        claims.setAudience("tenant-a");
        claims.setIssuedAt(Date.from(now.minus(3, ChronoUnit.DAYS)));
        claims.setExpiration(Date.from(now.minusSeconds(expiredSeconds)));
        claims.put("tenant-id", "tenant-a");
        claims.put("token_type", "access");
        return claims;
    }

    @Test void featureIsOffWithoutExplicitTenantAndSunset() {
        Claims claims = expiredAccess(3600);
        assertFalse(new MawaPayEmergencyAccessPolicy(false, "tenant-a", futureSunset, 48)
                .permits(claims, "POST", "/v2/cashup"));
        assertFalse(new MawaPayEmergencyAccessPolicy(true, "", futureSunset, 48)
                .permits(claims, "POST", "/v2/cashup"));
        assertFalse(new MawaPayEmergencyAccessPolicy(true, "tenant-a", "", 48)
                .permits(claims, "POST", "/v2/cashup"));
    }

    @Test void permitsExplicitMawaPayReadsAndFinancialSyncOnly() {
        var policy = new MawaPayEmergencyAccessPolicy(true, "tenant-a", futureSunset, 48);
        Claims claims = expiredAccess(3600);
        assertTrue(policy.permits(claims, "GET", "/v2/membership/master-data/snapshot"));
        assertTrue(policy.permits(claims, "GET", "/v2/pay-app/members"));
        assertTrue(policy.permits(claims, "POST", "/v2/sync/payment-batches/membership-premiums"));
        assertTrue(policy.permits(claims, "POST", "/v2/cashup"));
        assertTrue(policy.permits(claims, "POST", "/v2/payment-batches/b-1/cancellation-request"));
        assertTrue(policy.permits(claims, "POST", "/pay-app/receipt-sync"));
        assertTrue(policy.permits(claims, "GET", "/v2/device-sync/submissions/reconcile"));
        assertFalse(policy.permits(claims, "GET", "/v2/admin/users"));
        assertFalse(policy.permits(claims, "POST", "/v2/payment-batches/b-1/deletion-request"));
        assertFalse(policy.permits(claims, "DELETE", "/v2/cashup"));
        assertFalse(policy.permits(claims, "POST", "/v2/pay-app/device-identity/enroll"));
        assertFalse(policy.permits(claims, "GET", "/v2/pay-app/manual-actions"));
    }

    @Test void requiresRecentExpiredAccessTokenAndTenantAudienceMatch() {
        var policy = new MawaPayEmergencyAccessPolicy(true, "tenant-a", futureSunset, 48);
        assertFalse(policy.permits(expiredAccess(49 * 3600), "POST", "/v2/cashup"));
        assertFalse(new MawaPayEmergencyAccessPolicy(true, "tenant-a", futureSunset, 72)
                .permits(expiredAccess(3600), "POST", "/v2/cashup"));

        Claims refresh = expiredAccess(3600);
        refresh.put("token_type", "refresh");
        assertFalse(policy.permits(refresh, "POST", "/v2/cashup"));

        Claims wrongAudience = expiredAccess(3600);
        wrongAudience.setAudience("tenant-b");
        assertFalse(policy.permits(wrongAudience, "POST", "/v2/cashup"));

        Claims platform = expiredAccess(3600);
        platform.put("platform_session", true);
        assertFalse(policy.permits(platform, "POST", "/v2/cashup"));
    }

    @Test void expiredCutoffStopsAllRecovery() {
        var expired = new MawaPayEmergencyAccessPolicy(true, "tenant-a",
                Instant.now().minusSeconds(1).toString(), 48);
        assertFalse(expired.permits(expiredAccess(3600), "GET", "/v2/pay-app/members"));
    }
}
