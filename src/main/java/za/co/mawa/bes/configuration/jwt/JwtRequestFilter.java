package za.co.mawa.bes.configuration.jwt;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.configuration.security.model.JwtClaim;
import za.co.mawa.bes.service.JwtUserDetailsService;
import za.co.mawa.bes.service.MawaPayDeviceIdentityService;

import java.io.IOException;
import java.util.Date;

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtRequestFilter.class);

    @Autowired
    private JwtUserDetailsService jwtUserDetailsService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private MawaPayDeviceIdentityService deviceIdentityService;
    @Autowired
    private MawaPayEmergencyAccessPolicy emergencyAccessPolicy;
    @Autowired
    private MawaPayEmergencyBearerPolicy emergencyBearerPolicy;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "/authenticate".equals(path)
                || "/v2/authenticate".equals(path)
                || "/forgot-password".equals(path)
                || "/v2/forgot-password".equals(path)
                || "/reset-password".equals(path)
                || "/v2/reset-password".equals(path)
                || "/v2/refresh-token".equals(path)
                || "/v2/company-logo/content".equals(path)
                || "/v2/admin-handoff/exchange".equals(path)
                || path.startsWith("/internal/admin/")
                || "/xero/callback".equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        try {
            final String authHeader = request.getHeader("Authorization");
            // Temporary operator-approved bridge for installed APKs whose
            // refresh tokens have already been rejected. A tenant header is
            // not identity verification; this branch requires a Bearer JWT
            // but accepts expired/invalid ones only on the MawaPay allowlist.
            // Keep normally-valid tokens on the real user/device path so that
            // server-side audit and cashier attribution are preserved.
            String emergencyTenant = emergencyBearerPolicy.approvedTenant(request);
            if (emergencyTenant != null && !hasValidBearer(authHeader)) {
                forwardEmergency(request, response, chain, emergencyTenant);
                return;
            }

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwtToken = authHeader.substring(7);

                Claims tokenClaims;
                boolean emergencyAccess = false;
                try {
                    tokenClaims = jwtTokenUtil.getClaimFromToken(jwtToken, claims -> claims);
                } catch (ExpiredJwtException ex) {
                    // JJWT has already verified the JWT signature before it
                    // reports expiration. Unsigned, malformed and bad-signature
                    // tokens never reach this controlled recovery path.
                    tokenClaims = ex.getClaims();
                    if (!emergencyAccessPolicy.permits(
                            tokenClaims, request.getMethod(), request.getServletPath())) {
                        throw ex;
                    }
                    emergencyAccess = true;
                }
                String username = tokenClaims.getSubject();
                String tenantId = tokenClaims.get(JwtClaim.TENANT_ID.getValue(), String.class);

                TenantContext.setCurrentTenant(tenantId);
                UserContext.setCurrentUser(username);
                if (jwtTokenUtil.isDeviceSyncToken(tokenClaims)) {
                    String deviceId = tokenClaims.get("device_id", String.class);
                    Integer tokenVersion = tokenClaims.get("token_version", Integer.class);
                    try {
                        deviceIdentityService.requireActive(deviceId, tokenVersion == null ? 0 : tokenVersion);
                    } catch (IllegalArgumentException ex) {
                        if (emergencyTenant != null) {
                            forwardEmergency(request, response, chain, emergencyTenant);
                            return;
                        }
                        log.warn("Device sync authentication rejected: deviceId={}, path={}, reason={}",
                                deviceId, request.getServletPath(), ex.getMessage());
                        SecurityContextHolder.clearContext();
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                "Device sync identity must be re-enrolled");
                        return;
                    }
                    if (!isDeviceSyncRequest(request.getMethod(), request.getServletPath())) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                                "Device identity is restricted to MawaPay synchronization");
                        return;
                    }
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null,
                                    java.util.List.of(new SimpleGrantedAuthority("ROLE_DEVICE_SYNC")));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    UserContext.setBackgroundSession(true);
                    chain.doFilter(request, response);
                    return;
                }
                boolean platformSession = Boolean.TRUE.equals(tokenClaims.get("platform_session", Boolean.class));
                UserContext.setPlatformSession(platformSession);
                if (platformSession) {
                    UserContext.setPlatformUserId(textClaim(tokenClaims, "platform_user_id"));
                    UserContext.setPlatformUsername(textClaim(tokenClaims, "platform_username"));
                    UserContext.setPlatformDisplayName(textClaim(tokenClaims, "platform_display_name"));
                    UserContext.setPlatformEmail(textClaim(tokenClaims, "platform_email"));
                    UserContext.setAccountType(textClaim(tokenClaims, "account_type"));
                    UserContext.setAccessScope(textClaim(tokenClaims, "access_scope"));
                    UserContext.setTestUser(booleanClaim(tokenClaims, "is_test_user"));
                    UserContext.setProtectedUser(booleanClaim(tokenClaims, "is_protected_user"));
                    UserContext.setExternalTransactionsBlocked(booleanClaim(tokenClaims, "external_transactions_blocked"));
                    Long expiry = longClaim(tokenClaims, "access_expires_at");
                    UserContext.setAccessExpiresAt(expiry == null || expiry <= 0 ? null : new Date(expiry));
                    UserContext.setHandoffId(textClaim(tokenClaims, "handoff_id"));
                    UserContext.setAccessReason(textClaim(tokenClaims, "access_reason"));
                    UserContext.setTicketReference(textClaim(tokenClaims, "ticket_reference"));
                    UserContext.setHandoffRoleId(textClaim(tokenClaims, "handoff_role_id"));
                    UserContext.setHandoffRoleDescription(textClaim(tokenClaims, "handoff_role_description"));
                }

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    JwtUserDetailsService.AccessTokenUser accessTokenUser =
                            jwtUserDetailsService.loadAccessTokenUser(username);
                    UserDetails userDetails = accessTokenUser.userDetails();

                    if (jwtTokenUtil.validateAccessToken(tokenClaims, userDetails, emergencyAccess)
                            && jwtTokenUtil.isIssuedAfterPasswordChange(
                                    tokenClaims,
                                    accessTokenUser.passwordChangedAt()
                            )) {
                        if (emergencyAccess) {
                            log.warn("Temporary MawaPay expired-access grace used: tenant={}, user={}, method={}, path={}",
                                    tenantId, username, request.getMethod(), request.getServletPath());
                        }
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        UserContext.setCurrentUserPartner(accessTokenUser.partnerId());
                        UserContext.setCurrentUserId(accessTokenUser.userId());
                    }
                }
            }

            if (emergencyTenant != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                forwardEmergency(request, response, chain, emergencyTenant);
                return;
            }

            chain.doFilter(request, response);

        } catch (AuthenticationException ex) {
            String emergencyTenant = emergencyBearerPolicy.approvedTenant(request);
            if (emergencyTenant != null) {
                forwardEmergency(request, response, chain, emergencyTenant);
                return;
            }
            log.warn("JWT user authentication failed: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired session");
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT authentication failed: {}", ex.getMessage());
            request.setAttribute("exception", ex);
            chain.doFilter(request, response);
        } finally {
            clearContexts();
        }
    }

    private void forwardEmergency(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain chain, String tenant)
            throws ServletException, IOException {
        // Previous JWT attempts may have set a different tenant/user context.
        clearContexts();
        SecurityContextHolder.clearContext();
        TenantContext.setCurrentTenant(tenant);
        UserContext.setCurrentUser("mawapay-emergency");
        UserContext.setBackgroundSession(true);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "mawapay-emergency", null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_DEVICE_SYNC")));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        response.setHeader("Cache-Control", "no-store");
        log.warn("TEMPORARY MawaPay emergency bearer used: tenant={}, method={}, path={}",
                tenant, request.getMethod(), request.getServletPath());
        chain.doFilter(request, response);
    }

    private boolean hasValidBearer(String header) {
        if (header == null || !header.startsWith("Bearer ")) return false;
        try {
            jwtTokenUtil.getClaimFromToken(header.substring(7), claims -> claims);
            return true;
        } catch (JwtException | IllegalArgumentException ignored) {
            return false;
        }
    }


    private String textClaim(Claims claims, String key) {
        Object value = claims.get(key);
        return value == null ? null : String.valueOf(value);
    }

    static boolean isDeviceSyncPath(String path) {
        if (path == null) return false;
        return path.equals("/v2/pay-app/device-identity/renew")
                || path.startsWith("/v2/sync/")
                || path.startsWith("/v2/device-sync/")
                || path.startsWith("/v2/cashup")
                || path.startsWith("/v2/partner")
                || path.startsWith("/v2/membership")
                || path.equals("/v2/number-allocation/allocate")
                || path.equals("/v2/number-allocation/active")
                || path.startsWith("/v2/number-allocations")
                || path.matches("/v2/payment-batches/[^/]+/cancellation-status")
                || path.startsWith("/v2/receipt")
                || path.equals("/pay-app/receipt-sync");
    }

    static boolean isDeviceSyncRequest(String method, String path) {
        // Master-data reads can use the tenant-bound, revocable device credential,
        // but never let that credential perform pay-app management writes.
        return ("GET".equalsIgnoreCase(method) && "/v2/pay-app/members".equals(path))
                || isDeviceSyncPath(path);
    }

    private Boolean booleanClaim(Claims claims, String key) {
        Object value = claims.get(key);
        return value instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(value));
    }

    private Long longClaim(Claims claims, String key) {
        Object value = claims.get(key);
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        try { return Long.parseLong(String.valueOf(value)); } catch (Exception ignored) { return null; }
    }
    private void clearContexts() {
        try {
            TenantContext.clear();
        } catch (Exception e) {
            log.debug("Unable to clear TenantContext", e);
        }

        try {
            UserContext.clear();
        } catch (Exception e) {
            log.debug("Unable to clear UserContext", e);
        }
    }
}
