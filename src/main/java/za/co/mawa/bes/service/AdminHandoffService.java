package za.co.mawa.bes.service;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.jwt.JwtTokenUtil;
import za.co.mawa.bes.dto.AuthenticationResponseDto;
import za.co.mawa.bes.dto.admin.AdminHandoffRequestDto;
import za.co.mawa.bes.dto.admin.AdminHandoffResponseDto;
import za.co.mawa.bes.dto.user.UserDto;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class AdminHandoffService {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserService userService;

    @Value("${mawa.internal.service-token:}")
    private String internalServiceToken;

    @Value("${mawa.erp.app.url:}")
    private String defaultErpAppUrl;

    @Value("${mawa.admin.handoff.username:system}")
    private String handoffUsername;

    @Value("${mawa.admin.handoff.ttl-ms:300000}")
    private long handoffTtlMs;

    public void validateInternalToken(String token) {
        if (!StringUtils.hasText(internalServiceToken)) {
            throw new IllegalStateException("mawa.internal.service-token is not configured");
        }
        if (!constantTimeEquals(internalServiceToken, token)) {
            throw new SecurityException("Invalid internal service token");
        }
    }

    public AdminHandoffResponseDto createHandoff(AdminHandoffRequestDto request) {
        if (request == null || !StringUtils.hasText(request.getTenant())) {
            throw new IllegalArgumentException("Tenant is required");
        }

        String redirectPath = normalizeRedirectPath(request.getRedirectPath());
        long expiresAt = System.currentTimeMillis() + handoffTtlMs;
        String token = jwtTokenUtil.generateAdminHandoffToken(
                request.getTenant().trim(),
                request.getTenantHost(),
                request.getTenantUrl(),
                request.getAdminUsername(),
                redirectPath,
                handoffTtlMs
        );

        AdminHandoffResponseDto response = new AdminHandoffResponseDto();
        response.setTenant(request.getTenant());
        response.setTenantHost(request.getTenantHost());
        response.setTenantUrl(request.getTenantUrl());
        response.setHandoffToken(token);
        response.setExpiresAt(expiresAt);
        response.setTargetUrl(buildTargetUrl(request.getTenantUrl(), request.getTenantHost(), token, redirectPath));
        return response;
    }

    public AuthenticationResponseDto exchange(String token) throws Exception {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Handoff token is required");
        }

        final Claims claims;
        try {
            claims = jwtTokenUtil.getAdminHandoffClaims(token.trim());
        } catch (Exception ex) {
            throw new SecurityException("Handoff token is invalid or expired", ex);
        }

        String tenant = claims.get("tenant-id", String.class);
        if (!StringUtils.hasText(tenant)) {
            tenant = claims.getAudience();
        }
        if (!StringUtils.hasText(tenant)) {
            throw new SecurityException("Handoff token does not contain a tenant");
        }

        String tenantHost = claims.get("tenant_host", String.class);
        String tenantUrl = claims.get("tenant_url", String.class);

        try {
            TenantContext.setCurrentTenant(tenant);
            TenantContext.setCurrentTenantURL(resolveBaseUrl(tenantUrl, tenantHost));

            String username = StringUtils.hasText(handoffUsername) ? handoffUsername.trim() : UserService.SYSTEM_USER;
            UserDto userDto = userService.getUserByName(username);
            if (userDto == null || !StringUtils.hasText(userDto.getId())) {
                throw new IllegalStateException("Admin handoff user '" + username + "' does not exist for tenant " + tenant);
            }

            AuthenticationResponseDto response = new AuthenticationResponseDto();
            response.setAccessToken(jwtTokenUtil.generateToken(username, tenant));
            response.setRefreshToken(jwtTokenUtil.generateRefreshToken(username, tenant));
            response.setUsername(username);
            response.setUserId(userDto.getId());
            if (userDto.getPartner() != null) {
                response.setDisplayName((safe(userDto.getPartner().getName2()) + " " + safe(userDto.getPartner().getName1())).trim());
            } else {
                response.setDisplayName("Support Admin");
            }
            return response;
        } finally {
            TenantContext.clear();
        }
    }

    private String buildTargetUrl(String tenantUrl, String tenantHost, String token, String redirectPath) {
        String baseUrl = resolveBaseUrl(tenantUrl, tenantHost);
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String encodedRedirect = URLEncoder.encode(redirectPath, StandardCharsets.UTF_8);
        // MAWA ERP currently uses Flutter's hash URL strategy.
        return trimTrailingSlash(baseUrl) + "/#/admin-handoff?token=" + encodedToken + "&redirect=" + encodedRedirect;
    }

    private String resolveBaseUrl(String tenantUrl, String tenantHost) {
        if (StringUtils.hasText(tenantUrl)) {
            return normalizeAppOrigin(tenantUrl);
        }
        if (StringUtils.hasText(tenantHost)) {
            return normalizeAppOrigin(tenantHost);
        }
        if (StringUtils.hasText(defaultErpAppUrl)) {
            return normalizeAppOrigin(defaultErpAppUrl);
        }
        throw new IllegalStateException("Tenant URL/host or mawa.erp.app.url is required for admin handoff");
    }

    private String normalizeAppOrigin(String value) {
        String trimmed = value.trim();
        String withScheme = trimmed.startsWith("http://") || trimmed.startsWith("https://")
                ? trimmed
                : "https://" + trimmed;
        URI uri = URI.create(withScheme);
        if (!StringUtils.hasText(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
            throw new IllegalArgumentException("Invalid tenant ERP app URL: " + value);
        }
        int port = uri.getPort();
        return uri.getScheme() + "://" + uri.getHost() + (port >= 0 ? ":" + port : "");
    }

    private String normalizeRedirectPath(String value) {
        String path = StringUtils.hasText(value) ? value.trim() : "/home";
        if (!path.startsWith("/") || path.startsWith("//") || path.contains("://")) {
            return "/home";
        }
        return path;
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private boolean constantTimeEquals(String expected, String supplied) {
        if (supplied == null) {
            return false;
        }
        byte[] left = expected.getBytes(StandardCharsets.UTF_8);
        byte[] right = supplied.getBytes(StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(left, right);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
