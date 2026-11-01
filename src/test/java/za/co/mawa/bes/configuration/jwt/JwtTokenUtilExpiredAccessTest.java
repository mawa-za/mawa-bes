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

class JwtTokenUtilExpiredAccessTest {
    @AfterEach void clearTenant() { TenantContext.clear(); }

    @Test void expiredClaimsAreRejectedWithoutAnAuthenticationBypass() {
        Claims claims = Jwts.claims();
        claims.put("token_type", "access");
        claims.put("tenant-id", "tenant-a");
        claims.setAudience("tenant-a");
        claims.setSubject("cashier");
        claims.setIssuedAt(new Date(System.currentTimeMillis() - 86_400_000L));
        claims.setExpiration(new Date(System.currentTimeMillis() - 1000));
        var user = new User("cashier", "", List.of());
        TenantContext.setCurrentTenant("tenant-a");

        JwtTokenUtil tokens = new JwtTokenUtil();
        assertFalse(tokens.validateAccessToken(claims, user));
        var locked = new User("cashier", "", true, true, true, false, List.of());
        assertFalse(tokens.validateAccessToken(claims, locked));
        assertFalse(tokens.isIssuedAfterPasswordChange(
                claims, new Date(System.currentTimeMillis())));
        TenantContext.setCurrentTenant("tenant-b");
        assertFalse(tokens.validateAccessToken(claims, user));
        TenantContext.setCurrentTenant("tenant-a");
        claims.put("token_type", "refresh");
        assertFalse(tokens.validateAccessToken(claims, user));
    }
}
