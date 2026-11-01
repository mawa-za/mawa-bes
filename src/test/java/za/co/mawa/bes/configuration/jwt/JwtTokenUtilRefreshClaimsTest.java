package za.co.mawa.bes.configuration.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import za.co.mawa.bes.configuration.context.TenantContext;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenUtilRefreshClaimsTest {
    @AfterEach void clearTenant() { TenantContext.clear(); }

    @Test void expiredRefreshClaimsAreAlwaysRejected() {
        JwtTokenUtil tokens = new JwtTokenUtil();
        Claims claims = Jwts.claims();
        claims.setSubject("cashier");
        claims.setAudience("tenant-a");
        claims.put("tenant-id", "tenant-a");
        claims.put("token_type", "refresh");
        claims.setExpiration(new Date(System.currentTimeMillis() - 1000L));
        var user = new User("cashier", "", List.of());
        TenantContext.setCurrentTenant("tenant-a");

        assertFalse(tokens.validateRefreshTokenClaims(claims, user));
        TenantContext.setCurrentTenant("tenant-b");
        assertFalse(tokens.validateRefreshTokenClaims(claims, user));
        TenantContext.setCurrentTenant("tenant-a");
        claims.put("token_type", "access");
        assertFalse(tokens.validateRefreshTokenClaims(claims, user));
    }

    @Test void validRefreshClaimsRequireMatchingTenantTypeAndUser() {
        JwtTokenUtil tokens = new JwtTokenUtil();
        Claims claims = Jwts.claims();
        claims.setSubject("cashier");
        claims.setAudience("tenant-a");
        claims.put("tenant-id", "tenant-a");
        claims.put("token_type", "refresh");
        claims.setExpiration(new Date(System.currentTimeMillis() + 30_000L));
        TenantContext.setCurrentTenant("tenant-a");
        assertTrue(tokens.validateRefreshTokenClaims(claims, new User("cashier", "", List.of())));
        assertFalse(tokens.validateRefreshTokenClaims(claims, new User("other", "", List.of())));
    }
}
