package za.co.mawa.bes.configuration.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.service.JwtUserDetailsService;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JwtRefreshServiceEmergencyTest {
    @AfterEach void clearTenant() { TenantContext.clear(); }

    @Test void expiredSignedRefreshNeedsExplicitPolicyApproval() {
        var tokens = mock(JwtTokenUtil.class);
        var users = mock(JwtUserDetailsService.class);
        var policy = mock(MawaPayEmergencyRefreshPolicy.class);
        Claims claims = expiredClaims();
        when(tokens.getClaimFromToken(eq("expired"), any()))
                .thenThrow(new ExpiredJwtException(Jwts.header(), claims, "expired"));
        assertThrows(ExpiredJwtException.class,
                () -> new JwtRefreshService(tokens, users, policy).refresh("expired"));
        verifyNoInteractions(users);
    }

    @Test void approvedRecoveryStillRequiresActiveAccountAndPasswordCheck() {
        var tokens = mock(JwtTokenUtil.class);
        var users = mock(JwtUserDetailsService.class);
        var policy = mock(MawaPayEmergencyRefreshPolicy.class);
        Claims claims = expiredClaims();
        when(tokens.getClaimFromToken(eq("expired"), any()))
                .thenThrow(new ExpiredJwtException(Jwts.header(), claims, "expired"));
        when(policy.permits(claims)).thenReturn(true);
        var user = new User("cashier", "", List.of());
        when(users.loadAccessTokenUser("cashier"))
                .thenReturn(new JwtUserDetailsService.AccessTokenUser(user, "id", "partner", new Date()));
        when(tokens.validateRefreshTokenClaims(eq(claims), eq(user), eq(true))).thenReturn(true);
        // Password was changed after issuance: the emergency path may not renew this session.
        assertThrows(io.jsonwebtoken.JwtException.class,
                () -> new JwtRefreshService(tokens, users, policy).refresh("expired"));
        verify(tokens, never()).generateRefreshToken(anyString(), anyString(), anyMap());
    }

    @Test void approvedRecoveryRotatesCredentialsUsingNormalTokenIssuer() {
        var tokens = mock(JwtTokenUtil.class);
        var users = mock(JwtUserDetailsService.class);
        var policy = mock(MawaPayEmergencyRefreshPolicy.class);
        Claims claims = expiredClaims();
        when(tokens.getClaimFromToken(eq("expired"), any()))
                .thenThrow(new ExpiredJwtException(Jwts.header(), claims, "expired"));
        when(policy.permits(claims)).thenReturn(true);
        var user = new User("cashier", "", List.of());
        when(users.loadAccessTokenUser("cashier"))
                .thenReturn(new JwtUserDetailsService.AccessTokenUser(user, "id", "partner", null));
        when(tokens.validateRefreshTokenClaims(eq(claims), eq(user), eq(true))).thenReturn(true);
        when(tokens.isIssuedAfterPasswordChange(eq(claims), isNull())).thenReturn(true);
        when(tokens.generateToken(eq("cashier"), eq("tenant-a"), anyMap())).thenReturn("access");
        when(tokens.generateRefreshToken(eq("cashier"), eq("tenant-a"), anyMap())).thenReturn("refresh");

        var response = new JwtRefreshService(tokens, users, policy).refresh("expired");
        assertEquals("access", response.getAccessToken());
        assertEquals("refresh", response.getRefreshToken());
        verify(tokens).validateRefreshTokenClaims(claims, user, true);
        assertEquals(null, TenantContext.getCurrentTenant());
    }

    private static Claims expiredClaims() {
        Claims claims = Jwts.claims();
        claims.setSubject("cashier");
        claims.setAudience("tenant-a");
        claims.put("tenant-id", "tenant-a");
        claims.put("token_type", "refresh");
        claims.setIssuedAt(new Date(System.currentTimeMillis() - 86_400_000L));
        claims.setExpiration(new Date(System.currentTimeMillis() - 1000L));
        return claims;
    }
}
