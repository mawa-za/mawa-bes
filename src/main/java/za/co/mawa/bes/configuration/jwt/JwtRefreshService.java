package za.co.mawa.bes.configuration.jwt;

import io.jsonwebtoken.JwtException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.service.JwtUserDetailsService;

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

        String username = jwtTokenUtil.getUsernameFromToken(refreshToken);
        String tenantId = jwtTokenUtil.getTenantIdFromToken(refreshToken);
        if (tenantId == null || tenantId.isBlank()) {
            throw new JwtException("Refresh token does not contain a tenant");
        }

        String previousTenant = TenantContext.getCurrentTenant();
        try {
            // Refresh is a public endpoint, so JwtRequestFilter intentionally does not
            // establish tenant context. Set it before loading the tenant-scoped user.
            TenantContext.setCurrentTenant(tenantId);
            UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(username);

            if (!jwtTokenUtil.validateRefreshToken(refreshToken, userDetails)) {
                throw new JwtException("Invalid refresh token");
            }

            String newAccessToken = jwtTokenUtil.generateToken(username, tenantId);
            String newRefreshToken = jwtTokenUtil.generateRefreshToken(username, tenantId);
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