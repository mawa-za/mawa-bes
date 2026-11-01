package za.co.mawa.bes.configuration.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.service.JwtUserDetailsService;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import jakarta.servlet.FilterChain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtRequestFilterEmergencyAccessTest {
    private static final String SECRET = "mawapay-emergency-filter-test-secret-should-be-at-least-sixty-four-bytes-long-and-not-used-in-production";
    private static final String SUNSET = Instant.now().plus(2, ChronoUnit.DAYS).toString();

    @BeforeEach void resetSecurityContext() { SecurityContextHolder.clearContext(); }

    @AfterEach void clearContexts() {
        TenantContext.clear();
        UserContext.clear();
        SecurityContextHolder.clearContext();
    }

    private String signedExpiredAccess() {
        Instant now = Instant.now();
        var key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8),
                SignatureAlgorithm.HS512.getJcaName());
        return Jwts.builder()
                .claim("tenant-id", "tenant-a")
                .claim("token_type", "access")
                .setAudience("tenant-a")
                .setSubject("cashier")
                .setIssuedAt(Date.from(now.minus(2, ChronoUnit.DAYS)))
                .setExpiration(Date.from(now.minus(1, ChronoUnit.HOURS)))
                .signWith(SignatureAlgorithm.HS512, key)
                .compact();
    }

    private JwtRequestFilter filter(boolean enabled) {
        JwtTokenUtil tokens = new JwtTokenUtil();
        tokens.setSecret(SECRET);
        tokens.init();
        JwtUserDetailsService users = mock(JwtUserDetailsService.class);
        when(users.loadAccessTokenUser("cashier"))
                .thenReturn(new JwtUserDetailsService.AccessTokenUser(
                        new User("cashier", "", List.of()), "user-1", "partner-1", null));
        JwtRequestFilter filter = new JwtRequestFilter();
        ReflectionTestUtils.setField(filter, "jwtTokenUtil", tokens);
        ReflectionTestUtils.setField(filter, "jwtUserDetailsService", users);
        ReflectionTestUtils.setField(filter, "emergencyAccessPolicy",
                new MawaPayEmergencyAccessPolicy(enabled, "tenant-a", SUNSET, 48));
        ReflectionTestUtils.setField(filter, "emergencyBearerPolicy",
                new MawaPayEmergencyBearerPolicy(false, "", ""));
        return filter;
    }

    private MockHttpServletRequest request(String path, String token) {
        var request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    @Test void signedExpiredAccessAuthenticatesForApprovedMawaPayCashup() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        filter(true).doFilter(request("/v2/cashup", signedExpiredAccess()),
                new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
    }

    @Test void disabledGraceDoesNotAuthenticateAnExpiredAccessToken() throws Exception {
        filter(false).doFilter(request("/v2/cashup", signedExpiredAccess()),
                new MockHttpServletResponse(), mock(FilterChain.class));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test void signedExpiredAccessCannotAuthenticateOtherFinancialRoutes() throws Exception {
        filter(true).doFilter(request("/v2/payment-batches/b-1/deletion-request", signedExpiredAccess()),
                new MockHttpServletResponse(), mock(FilterChain.class));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test void InvalidSignatureNeverAuthenticatesEvenDuringGrace() throws Exception {
        String signed = signedExpiredAccess();
        String tampered = signed.substring(0, signed.length() - 2)
                + (signed.endsWith("AA") ? "BB" : "AA");
        filter(true).doFilter(request("/v2/cashup", tampered),
                new MockHttpServletResponse(), mock(FilterChain.class));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
