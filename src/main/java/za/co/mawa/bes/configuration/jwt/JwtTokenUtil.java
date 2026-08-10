package za.co.mawa.bes.configuration.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.security.model.JwtClaim;

import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenUtil implements Serializable {

    private static final long serialVersionUID = -2550185165626007488L;

    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String ACCESS_TOKEN = "access";
    private static final String REFRESH_TOKEN = "refresh";
    private static final String ADMIN_HANDOFF_TOKEN = "admin_handoff";

    private long jwtExpirationInMs;
    private long refreshExpirationDateInMs;
    private String secret;
    private Key signingKey;

    @Value("${jwt.secret}")
    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Value("${jwt.expirationDateInMs}")
    public void setJwtExpirationInMs(long jwtExpirationInMs) {
        this.jwtExpirationInMs = jwtExpirationInMs;
    }

    @Value("${jwt.refreshExpirationDateInMs}")
    public void setRefreshExpirationDateInMs(long refreshExpirationDateInMs) {
        this.refreshExpirationDateInMs = refreshExpirationDateInMs;
    }

    @PostConstruct
    public void init() {
        this.signingKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                SignatureAlgorithm.HS512.getJcaName()
        );
    }

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Date getIssuedAtDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getIssuedAt);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public String getTenantIdFromToken(String token) {
        return getClaimFromToken(token, claims ->
                claims.get(JwtClaim.TENANT_ID.getValue(), String.class));
    }

    public String getAudienceFromToken(String token) {
        return getClaimFromToken(token, Claims::getAudience);
    }

    public String getTokenType(String token) {
        return getClaimFromToken(token, claims ->
                claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public boolean isAccessToken(String token) {
        return ACCESS_TOKEN.equals(getTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return REFRESH_TOKEN.equals(getTokenType(token));
    }

    public boolean isIssuedAfterPasswordChange(String token, Date passwordChangedAt) {
        final Claims claims = getAllClaimsFromToken(token);
        return isIssuedAfterPasswordChange(claims, passwordChangedAt);
    }

    public boolean isIssuedAfterPasswordChange(Claims claims, Date passwordChangedAt) {
        if (passwordChangedAt == null) {
            return true;
        }
        Object value = claims.get("issued_at_ms");
        Long issuedAtMs = null;
        if (value instanceof Number number) {
            issuedAtMs = number.longValue();
        } else if (value != null) {
            try {
                issuedAtMs = Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                // Fall back to the standard JWT issued-at value below.
            }
        }
        if (issuedAtMs != null) {
            return issuedAtMs >= passwordChangedAt.getTime();
        }
        Date issuedAt = claims.getIssuedAt();
        return issuedAt != null && !issuedAt.before(passwordChangedAt);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    public Claims getAllClaimsFromTokenAllowExpired(String token) {
        try {
            return getAllClaimsFromToken(token);
        } catch (ExpiredJwtException ex) {
            return ex.getClaims();
        }
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(signingKey)
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public String generateToken(String username) {
        String tenantId = TenantContext.getCurrentTenant();
        return generateToken(username, tenantId);
    }

    public String generateToken(String username, String tenantId) {
        return generateToken(username, tenantId, null);
    }

    public String generateToken(String username, String tenantId, Map<String, Object> additionalClaims) {
        Map<String, Object> claims = new HashMap<>();
        if (additionalClaims != null) claims.putAll(additionalClaims);
        claims.put(JwtClaim.TENANT_ID.getValue(), tenantId);
        claims.put(CLAIM_TOKEN_TYPE, ACCESS_TOKEN);
        return doGenerateToken(claims, username, tenantId, jwtExpirationInMs);
    }

    public String generateRefreshToken(String username) {
        String tenantId = TenantContext.getCurrentTenant();
        return generateRefreshToken(username, tenantId);
    }

    public String generateRefreshToken(String username, String tenantId) {
        return generateRefreshToken(username, tenantId, null);
    }

    public String generateRefreshToken(String username, String tenantId, Map<String, Object> additionalClaims) {
        Map<String, Object> claims = new HashMap<>();
        if (additionalClaims != null) claims.putAll(additionalClaims);
        claims.put(JwtClaim.TENANT_ID.getValue(), tenantId);
        claims.put(CLAIM_TOKEN_TYPE, REFRESH_TOKEN);
        return doGenerateToken(claims, username, tenantId, refreshExpirationDateInMs);
    }

    public String generateAdminHandoffToken(
            String tenantId,
            String tenantHost,
            String tenantUrl,
            String adminUsername,
            String platformUserId,
            String displayName,
            String email,
            String accountType,
            String platformScope,
            Boolean testUser,
            Boolean protectedUser,
            Boolean externalTransactionsBlocked,
            Date accessExpiresAt,
            java.util.List<String> roleIds,
            String accessReason,
            String ticketReference,
            String redirectPath,
            long expiryInMs
    ) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaim.TENANT_ID.getValue(), tenantId);
        claims.put(CLAIM_TOKEN_TYPE, ADMIN_HANDOFF_TOKEN);
        claims.put("tenant_host", tenantHost == null ? "" : tenantHost);
        claims.put("tenant_url", tenantUrl == null ? "" : tenantUrl);
        claims.put("admin_username", adminUsername == null ? "" : adminUsername);
        claims.put("platform_user_id", platformUserId == null ? "" : platformUserId);
        claims.put("platform_display_name", displayName == null ? adminUsername : displayName);
        claims.put("platform_email", email == null ? "" : email);
        claims.put("account_type", accountType == null ? "STANDARD" : accountType);
        claims.put("access_scope", platformScope == null ? "STANDARD" : platformScope);
        claims.put("is_test_user", Boolean.TRUE.equals(testUser));
        claims.put("is_protected_user", Boolean.TRUE.equals(protectedUser));
        claims.put("external_transactions_blocked", Boolean.TRUE.equals(externalTransactionsBlocked));
        claims.put("access_expires_at", accessExpiresAt == null ? null : accessExpiresAt.getTime());
        claims.put("platform_roles", roleIds == null ? java.util.List.of() : roleIds);
        claims.put("access_reason", accessReason == null ? "" : accessReason);
        claims.put("ticket_reference", ticketReference == null ? "" : ticketReference);
        claims.put("redirect_path", redirectPath == null ? "/home" : redirectPath);
        claims.put("handoff_id", java.util.UUID.randomUUID().toString());
        return doGenerateToken(claims, "admin-handoff", tenantId, expiryInMs);
    }

    public Claims getAdminHandoffClaims(String token) {
        Claims claims = getAllClaimsFromToken(token);
        String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!ADMIN_HANDOFF_TOKEN.equals(tokenType)) {
            throw new IllegalArgumentException("Token is not an admin handoff token");
        }
        return claims;
    }

    private String doGenerateToken(
            Map<String, Object> claims,
            String subject,
            String tenantId,
            long expiryInMs
    ) {
        long now = System.currentTimeMillis();
        Map<String, Object> effectiveClaims = new HashMap<>(claims);
        effectiveClaims.put("issued_at_ms", now);

        return Jwts.builder()
                .setClaims(effectiveClaims)
                .setAudience(tenantId)
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expiryInMs))
                .signWith(SignatureAlgorithm.HS512, signingKey)
                .compact();
    }

    public boolean validateAccessToken(String token, UserDetails userDetails) {
        final Claims claims = getAllClaimsFromToken(token);
        return validateAccessToken(claims, userDetails);
    }

    public boolean validateAccessToken(Claims claims, UserDetails userDetails) {
        final String username = claims.getSubject();
        final String tokenTenant = claims.get(JwtClaim.TENANT_ID.getValue(), String.class);
        final String currentTenant = TenantContext.getCurrentTenant();
        final String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
        final Date expiration = claims.getExpiration();

        return ACCESS_TOKEN.equals(tokenType)
                && username.equals(userDetails.getUsername())
                && expiration != null
                && expiration.after(new Date())
                && tokenTenant != null
                && tokenTenant.equals(currentTenant);
    }

    public boolean validateRefreshToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        final String tokenTenant = getTenantIdFromToken(token);

        return isRefreshToken(token)
                && username.equals(userDetails.getUsername())
                && !isTokenExpired(token)
                && tokenTenant != null;
    }
}