package za.co.mawa.bes.configuration.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.service.JwtUserDetailsService;

import java.util.HashMap;
import java.util.Map;

@Service
public class JwtRefreshService {
    private final JwtTokenUtil jwtTokenUtil;
    private final JwtUserDetailsService jwtUserDetailsService;
    public JwtRefreshService(
            JwtTokenUtil jwtTokenUtil,
            JwtUserDetailsService jwtUserDetailsService
    ) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    public JwtResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }

        // Expired, malformed and incorrectly signed refresh tokens are rejected.
        Claims existingClaims = jwtTokenUtil.getClaimFromToken(refreshToken, claims -> claims);

        String username = existingClaims.getSubject();
        String tenantId = existingClaims.get("tenant-id", String.class);
        if (tenantId == null || tenantId.isBlank()) {
            throw new JwtException("Refresh token does not contain a tenant");
        }

        String previousTenant = TenantContext.getCurrentTenant();
        try {
            // Refresh is a public endpoint, so JwtRequestFilter intentionally does not
            // establish tenant context. Set it before loading the tenant-scoped user.
            TenantContext.setCurrentTenant(tenantId);
            JwtUserDetailsService.AccessTokenUser authenticatedUser =
                    jwtUserDetailsService.loadAccessTokenUser(username);
            if (!jwtTokenUtil.validateRefreshTokenClaims(
                            existingClaims, authenticatedUser.userDetails())
                    || !jwtTokenUtil.isIssuedAfterPasswordChange(
                            existingClaims, authenticatedUser.passwordChangedAt())) {
                throw new JwtException("Invalid refresh token");
            }
            Map<String, Object> sessionClaims = new HashMap<>(existingClaims);
            sessionClaims.remove(Claims.SUBJECT);
            sessionClaims.remove(Claims.AUDIENCE);
            sessionClaims.remove(Claims.ISSUED_AT);
            sessionClaims.remove(Claims.EXPIRATION);
            sessionClaims.remove(Claims.NOT_BEFORE);
            sessionClaims.remove(Claims.ID);
            sessionClaims.remove("token_type");
            sessionClaims.remove("tenant-id");

            // Preserve platform/test access policy claims when rotating tokens.
            // Without this, a platform or QA session could silently become an
            // unrestricted SYSTEM session after the first token refresh.
            String newAccessToken = jwtTokenUtil.generateToken(username, tenantId, sessionClaims);
            String newRefreshToken = jwtTokenUtil.generateRefreshToken(username, tenantId, sessionClaims);
            return new JwtResponse(newAccessToken, newRefreshToken);
        } finally {
            if (previousTenant == null || previousTenant.isBlank()) {
                TenantContext.clear();
            } else {
                TenantContext.setCurrentTenant(previousTenant);
            }
        }
    }
}
