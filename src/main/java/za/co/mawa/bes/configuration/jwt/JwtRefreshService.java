package za.co.mawa.bes.configuration.jwt;

import io.jsonwebtoken.JwtException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
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

        UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(username);

        if (!jwtTokenUtil.validateRefreshToken(refreshToken, userDetails)) {
            throw new JwtException("Invalid refresh token");
        }

        String newAccessToken = jwtTokenUtil.generateToken(username, tenantId);

        // optional rotation
        String newRefreshToken = jwtTokenUtil.generateRefreshToken(username, tenantId);

        return new JwtResponse(newAccessToken, newRefreshToken);
    }
}