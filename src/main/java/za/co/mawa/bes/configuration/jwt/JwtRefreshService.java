package za.co.mawa.bes.configuration.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.service.JwtUserDetailsService;

import java.util.HashMap;
import java.util.Map;

@Service
public class JwtRefreshService {
    private static final Logger log = LoggerFactory.getLogger(JwtRefreshService.class);

    private final JwtTokenUtil jwtTokenUtil;
    private final JwtUserDetailsService jwtUserDetailsService;
    private final MawaPayEmergencyRefreshPolicy emergencyRefreshPolicy;

    public JwtRefreshService(
            JwtTokenUtil jwtTokenUtil,
            JwtUserDetailsService jwtUserDetailsService,
            MawaPayEmergencyRefreshPolicy emergencyRefreshPolicy
    ) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.jwtUserDetailsService = jwtUserDetailsService;
        this.emergencyRefreshPolicy = emergencyRefreshPolicy;
    }

    public JwtResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }

        // JJWT checks the signature before reporting expiry. Only a correctly
        // signed, recently expired refresh JWT can enter the emergency path.
        Claims existingClaims;
        boolean emergencyRecovery = false;
        try {
            existingClaims = jwtTokenUtil.getClaimFromToken(refreshToken, claims -> claims);
        } catch (ExpiredJwtException ex) {
            existingClaims = ex.getClaims();
            if (!emergencyRefreshPolicy.permits(existingClaims)) {
                throw ex;
            }
            emergencyRecovery = true;
        }

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
                            existingClaims, authenticatedUser.userDetails(), emergencyRecovery)
                    || !jwtTokenUtil.isIssuedAfterPasswordChange(
                            existingClaims, authenticatedUser.passwordChangedAt())) {
                throw new JwtException("Invalid refresh token");
            }

            if (emergencyRecovery) {
                // Audit metadata only; never log the JWT itself.
                log.warn("MawaPay emergency refresh grace used: tenant={}, user={}",
                        tenantId, username);
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
