package za.co.mawa.bes.service;

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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AdminHandoffService {

    private final ConcurrentHashMap<String, HandoffEntry> handoffTokens = new ConcurrentHashMap<>();

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
        if (!internalServiceToken.equals(token)) {
            throw new SecurityException("Invalid internal service token");
        }
    }

    public AdminHandoffResponseDto createHandoff(AdminHandoffRequestDto request) {
        if (request == null || !StringUtils.hasText(request.getTenant())) {
            throw new IllegalArgumentException("Tenant is required");
        }

        cleanupExpiredTokens();

        long expiresAt = System.currentTimeMillis() + handoffTtlMs;
        String token = UUID.randomUUID().toString().replace("-", "");
        String redirectPath = StringUtils.hasText(request.getRedirectPath()) ? request.getRedirectPath() : "/home";
        handoffTokens.put(token, new HandoffEntry(
                request.getTenant(),
                request.getTenantHost(),
                request.getTenantUrl(),
                request.getAdminUsername(),
                redirectPath,
                expiresAt
        ));

        AdminHandoffResponseDto response = new AdminHandoffResponseDto();
        response.setTenant(request.getTenant());
        response.setTenantHost(request.getTenantHost());
        response.setTenantUrl(request.getTenantUrl());
        response.setHandoffToken(token);
        response.setExpiresAt(expiresAt);
        response.setTargetUrl(buildTargetUrl(request, token, redirectPath));
        return response;
    }

    public AuthenticationResponseDto exchange(String token) throws Exception {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Handoff token is required");
        }

        HandoffEntry entry = handoffTokens.remove(token);
        if (entry == null) {
            throw new SecurityException("Handoff token is invalid or already used");
        }
        if (entry.expiresAt < System.currentTimeMillis()) {
            throw new SecurityException("Handoff token has expired");
        }

        try {
            TenantContext.setCurrentTenant(entry.tenant);
            TenantContext.setCurrentTenantURL(resolveBaseUrl(entry));

            String username = StringUtils.hasText(handoffUsername) ? handoffUsername : UserService.SYSTEM_USER;
            UserDto userDto = userService.getUserByName(username);
            if (userDto == null || !StringUtils.hasText(userDto.getId())) {
                throw new IllegalStateException("Admin handoff user does not exist for tenant " + entry.tenant);
            }

            AuthenticationResponseDto response = new AuthenticationResponseDto();
            response.setAccessToken(jwtTokenUtil.generateToken(username, entry.tenant));
            response.setRefreshToken(jwtTokenUtil.generateRefreshToken(username, entry.tenant));
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

    private String buildTargetUrl(AdminHandoffRequestDto request, String token, String redirectPath) {
        String baseUrl = resolveBaseUrl(request.getTenantUrl(), request.getTenantHost());
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String encodedRedirect = URLEncoder.encode(redirectPath, StandardCharsets.UTF_8);
        return trimTrailingSlash(baseUrl) + "/admin-handoff?token=" + encodedToken + "&redirect=" + encodedRedirect;
    }

    private String resolveBaseUrl(HandoffEntry entry) {
        return resolveBaseUrl(entry.tenantUrl, entry.tenantHost);
    }

    private String resolveBaseUrl(String tenantUrl, String tenantHost) {
        if (StringUtils.hasText(tenantUrl)) {
            return normalizeUrl(tenantUrl);
        }
        if (StringUtils.hasText(tenantHost)) {
            return normalizeUrl(tenantHost);
        }
        if (StringUtils.hasText(defaultErpAppUrl)) {
            return normalizeUrl(defaultErpAppUrl);
        }
        throw new IllegalStateException("Tenant URL/host or mawa.erp.app.url is required for admin handoff");
    }

    private String normalizeUrl(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return "https://" + trimmed;
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void cleanupExpiredTokens() {
        long now = Instant.now().toEpochMilli();
        handoffTokens.entrySet().removeIf(entry -> entry.getValue().expiresAt < now);
    }

    private static class HandoffEntry {
        private final String tenant;
        private final String tenantHost;
        private final String tenantUrl;
        private final String adminUsername;
        private final String redirectPath;
        private final long expiresAt;

        private HandoffEntry(String tenant, String tenantHost, String tenantUrl, String adminUsername, String redirectPath, long expiresAt) {
            this.tenant = tenant;
            this.tenantHost = tenantHost;
            this.tenantUrl = tenantUrl;
            this.adminUsername = adminUsername;
            this.redirectPath = redirectPath;
            this.expiresAt = expiresAt;
        }
    }
}
