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
import za.co.mawa.bes.entity.RoleEntity;
import za.co.mawa.bes.entity.access.PlatformPrincipalAuditEntity;
import za.co.mawa.bes.repository.RoleRepository;
import za.co.mawa.bes.repository.access.PlatformPrincipalAuditRepository;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AdminHandoffService {

    @Autowired private JwtTokenUtil jwtTokenUtil;
    @Autowired private UserService userService;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PlatformPrincipalAuditRepository platformAuditRepository;

    @Value("${mawa.internal.service-token:}") private String internalServiceToken;
    @Value("${mawa.erp.app.url:}") private String defaultErpAppUrl;
    @Value("${mawa.admin.handoff.username:system}") private String handoffUsername;
    @Value("${mawa.admin.handoff.ttl-ms:300000}") private long handoffTtlMs;

    public void validateInternalToken(String token) {
        if (!StringUtils.hasText(internalServiceToken)) throw new IllegalStateException("mawa.internal.service-token is not configured");
        if (!constantTimeEquals(internalServiceToken, token)) throw new SecurityException("Invalid internal service token");
    }

    public AdminHandoffResponseDto createHandoff(AdminHandoffRequestDto request) {
        if (request == null || !StringUtils.hasText(request.getTenant())) throw new IllegalArgumentException("Tenant is required");
        String redirectPath = normalizeRedirectPath(request.getRedirectPath());
        long expiresAt = System.currentTimeMillis() + handoffTtlMs;
        String token = jwtTokenUtil.generateAdminHandoffToken(
                request.getTenant().trim(), request.getTenantHost(), request.getTenantUrl(), request.getAdminUsername(),
                request.getPlatformUserId(), request.getDisplayName(), request.getEmail(), request.getAccountType(),
                request.getPlatformScope(), request.getTestUser(), request.getProtectedUser(),
                request.getExternalTransactionsBlocked(), request.getExpiresAt(), request.getRoleIds(),
                request.getAccessReason(), request.getTicketReference(), redirectPath, handoffTtlMs);
        AdminHandoffResponseDto response = new AdminHandoffResponseDto();
        response.setTenant(request.getTenant()); response.setTenantHost(request.getTenantHost()); response.setTenantUrl(request.getTenantUrl());
        response.setHandoffToken(token); response.setExpiresAt(expiresAt);
        response.setTargetUrl(buildTargetUrl(request.getTenantUrl(), request.getTenantHost(), token, redirectPath));
        return response;
    }

    public AuthenticationResponseDto exchange(String token) throws Exception {
        return exchange(token, null, null);
    }

    public AuthenticationResponseDto exchange(String token, String sourceIp, String userAgent) throws Exception {
        if (!StringUtils.hasText(token)) throw new IllegalArgumentException("Handoff token is required");
        final Claims claims;
        try { claims = jwtTokenUtil.getAdminHandoffClaims(token.trim()); }
        catch (Exception ex) { throw new SecurityException("Handoff token is invalid or expired", ex); }

        String tenant = text(claims, "tenant-id");
        if (!StringUtils.hasText(tenant)) tenant = claims.getAudience();
        if (!StringUtils.hasText(tenant)) throw new SecurityException("Handoff token does not contain a tenant");
        Long accessExpiresAt = number(claims, "access_expires_at");
        if (accessExpiresAt != null && accessExpiresAt > 0 && accessExpiresAt < System.currentTimeMillis()) {
            throw new SecurityException("Platform user access has expired");
        }

        String tenantHost = text(claims, "tenant_host");
        String tenantUrl = text(claims, "tenant_url");
        String handoffId = text(claims, "handoff_id");
        String platformUsername = text(claims, "admin_username");
        String platformUserId = text(claims, "platform_user_id");
        String displayName = text(claims, "platform_display_name");
        String email = text(claims, "platform_email");
        String accountType = defaultText(text(claims, "account_type"), "STANDARD");
        String accessScope = defaultText(text(claims, "access_scope"), "STANDARD");
        boolean testUser = bool(claims, "is_test_user");
        boolean protectedUser = bool(claims, "is_protected_user");
        boolean externalBlocked = bool(claims, "external_transactions_blocked");
        String accessReason = text(claims, "access_reason");
        String ticketReference = text(claims, "ticket_reference");

        try {
            TenantContext.setCurrentTenant(tenant);
            TenantContext.setCurrentTenantURL(resolveBaseUrl(tenantUrl, tenantHost));
            String username = StringUtils.hasText(handoffUsername) ? handoffUsername.trim() : UserService.SYSTEM_USER;
            UserDto systemUser = userService.getUserByName(username);
            if (systemUser == null || !StringUtils.hasText(systemUser.getId())) {
                throw new IllegalStateException("Admin handoff user '" + username + "' does not exist for tenant " + tenant);
            }
            RoleEntity handoffRole = resolveHandoffRole(accessScope, claims);

            Map<String,Object> sessionClaims = new HashMap<>();
            sessionClaims.put("platform_session", true);
            sessionClaims.put("platform_user_id", platformUserId);
            sessionClaims.put("platform_username", platformUsername);
            sessionClaims.put("platform_display_name", displayName);
            sessionClaims.put("platform_email", email);
            sessionClaims.put("account_type", accountType);
            sessionClaims.put("access_scope", accessScope);
            sessionClaims.put("is_test_user", testUser);
            sessionClaims.put("is_protected_user", protectedUser);
            sessionClaims.put("external_transactions_blocked", externalBlocked);
            sessionClaims.put("access_expires_at", accessExpiresAt);
            sessionClaims.put("handoff_id", handoffId);
            sessionClaims.put("access_reason", accessReason);
            sessionClaims.put("ticket_reference", ticketReference);
            sessionClaims.put("handoff_role_id", handoffRole.getId());
            sessionClaims.put("handoff_role_description", handoffRole.getDescription());

            AuthenticationResponseDto response = new AuthenticationResponseDto();
            response.setAccessToken(jwtTokenUtil.generateToken(username, tenant, sessionClaims));
            response.setRefreshToken(jwtTokenUtil.generateRefreshToken(username, tenant, sessionClaims));
            response.setUsername(platformUsername);
            response.setUserId(systemUser.getId());
            response.setDisplayName(StringUtils.hasText(displayName) ? displayName : platformUsername);
            response.setAccountType(accountType); response.setTestUser(testUser); response.setProtectedUser(protectedUser);
            response.setAccessScope(accessScope); response.setPlatformSession(true); response.setPlatformUserId(platformUserId);
            response.setTenantId(tenant); response.setRoleId(handoffRole.getId());
            response.setRoleDescription(handoffRole.getDescription());
            response.setExternalTransactionsBlocked(externalBlocked);
            response.setExpiresAt(accessExpiresAt == null || accessExpiresAt <= 0 ? null : new Date(accessExpiresAt));
            response.setHandoffId(handoffId); response.setAccessReason(accessReason); response.setTicketReference(ticketReference);

            platformAuditRepository.save(PlatformPrincipalAuditEntity.builder()
                    .handoffId(handoffId).platformUserId(platformUserId).username(platformUsername).displayName(displayName)
                    .email(email).tenantId(tenant).erpRoleId(handoffRole.getId()).accessScope(accessScope)
                    .accountType(accountType).testUser(testUser)
                    .externalTransactionsBlocked(externalBlocked).accessReason(accessReason).ticketReference(ticketReference)
                    .sessionId(java.util.UUID.randomUUID().toString()).sourceIp(sourceIp).userAgent(userAgent).enteredAt(new Date()).build());
            return response;
        } finally { TenantContext.clear(); }
    }


    private RoleEntity resolveHandoffRole(String accessScope, Claims claims) {
        if ("PLATFORM_ALL".equalsIgnoreCase(accessScope)) {
            return roleRepository.findById("SYSTEM").orElseThrow(() ->
                    new IllegalStateException("ERP SYSTEM role is not configured for this tenant"));
        }
        Object rawRoles = claims.get("platform_roles");
        if (rawRoles instanceof Iterable<?> values) {
            for (Object value : values) {
                if (value == null) continue;
                String roleId = String.valueOf(value).trim().toUpperCase(java.util.Locale.ROOT);
                RoleEntity role = roleRepository.findById(roleId).orElse(null);
                if (role != null && !Boolean.TRUE.equals(role.getAccessAllWorkcentres())) return role;
            }
        }
        throw new IllegalStateException(
                "No matching ERP handoff role is configured. Assign PLATFORM_QA_TESTER or "
                        + "SUPPORT_VERIFICATION workcentres through ERP Role Maintenance.");
    }

    private String text(Claims claims, String key) { Object value = claims.get(key); return value == null ? null : String.valueOf(value); }
    private Long number(Claims claims, String key) { Object v=claims.get(key); if(v==null) return null; if(v instanceof Number n) return n.longValue(); try{return Long.parseLong(String.valueOf(v));}catch(Exception e){return null;} }
    private boolean bool(Claims claims, String key) { Object v=claims.get(key); return v instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(v)); }
    private String defaultText(String value, String fallback) { return StringUtils.hasText(value) ? value : fallback; }

    private String buildTargetUrl(String tenantUrl, String tenantHost, String token, String redirectPath) {
        String baseUrl = resolveBaseUrl(tenantUrl, tenantHost);
        return trimTrailingSlash(baseUrl) + "/#/admin-handoff?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&redirect=" + URLEncoder.encode(redirectPath, StandardCharsets.UTF_8);
    }
    private String resolveBaseUrl(String tenantUrl, String tenantHost) {
        if (StringUtils.hasText(tenantUrl)) return normalizeAppOrigin(tenantUrl);
        if (StringUtils.hasText(tenantHost)) return normalizeAppOrigin(tenantHost);
        if (StringUtils.hasText(defaultErpAppUrl)) return normalizeAppOrigin(defaultErpAppUrl);
        throw new IllegalStateException("Tenant URL/host or mawa.erp.app.url is required for admin handoff");
    }
    private String normalizeAppOrigin(String value) {
        String trimmed=value.trim(); String withScheme=trimmed.startsWith("http://")||trimmed.startsWith("https://")?trimmed:"https://"+trimmed;
        URI uri=URI.create(withScheme); if(!StringUtils.hasText(uri.getScheme())||!StringUtils.hasText(uri.getHost())) throw new IllegalArgumentException("Invalid tenant ERP app URL: "+value);
        return uri.getScheme()+"://"+uri.getHost()+(uri.getPort()>=0?":"+uri.getPort():"");
    }
    private String normalizeRedirectPath(String value) { String path=StringUtils.hasText(value)?value.trim():"/home"; return !path.startsWith("/")||path.startsWith("//")||path.contains("://")?"/home":path; }
    private String trimTrailingSlash(String value) { return value.endsWith("/")?value.substring(0,value.length()-1):value; }
    private boolean constantTimeEquals(String expected,String supplied){ if(supplied==null)return false; return java.security.MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8)); }
}
