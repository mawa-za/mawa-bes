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
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaim.TENANT_ID.getValue(), tenantId);
        claims.put(CLAIM_TOKEN_TYPE, ACCESS_TOKEN);
        return doGenerateToken(claims, username, tenantId, jwtExpirationInMs);
    }

    public String generateRefreshToken(String username) {
        String tenantId = TenantContext.getCurrentTenant();
        return generateRefreshToken(username, tenantId);
    }

    public String generateRefreshToken(String username, String tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaim.TENANT_ID.getValue(), tenantId);
        claims.put(CLAIM_TOKEN_TYPE, REFRESH_TOKEN);
        return doGenerateToken(claims, username, tenantId, refreshExpirationDateInMs);
    }

    private String doGenerateToken(
            Map<String, Object> claims,
            String subject,
            String tenantId,
            long expiryInMs
    ) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setClaims(claims)
                .setAudience(tenantId)
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expiryInMs))
                .signWith(SignatureAlgorithm.HS512, signingKey)
                .compact();
    }

    public boolean validateAccessToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        final String tokenTenant = getTenantIdFromToken(token);
        final String currentTenant = TenantContext.getCurrentTenant();

        return isAccessToken(token)
                && username.equals(userDetails.getUsername())
                && !isTokenExpired(token)
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