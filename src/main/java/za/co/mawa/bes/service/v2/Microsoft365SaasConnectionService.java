package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.gcp.GcpTenantSecretService;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.service.TenantAdminService;
import za.co.mawa.bes.dto.TenantDto;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * One MAWA-owned multitenant Entra application. A customer authorizes the application's
 * service principal in their own directory, then their Exchange admin scopes Application Mail.Read
 * to the selected bank mailbox using Exchange Online application RBAC.
 *
 * IMPORTANT: Do NOT grant the central app unscoped Graph Mail.Read application consent.
 * All OAuth state and Microsoft directory connections are stored in the CURRENT MAWA TENANT schema.
 */
@Service @RequiredArgsConstructor
public class Microsoft365SaasConnectionService {
    private static final String GROUP = BankStatementMailConfigurationService.GROUP;
    private static final String PENDING_HASH = "MS365-PENDING-STATE-SHA256";
    private static final String PENDING_NONCE = "MS365-PENDING-NONCE";
    private static final String PENDING_EXPIRY = "MS365-PENDING-EXPIRES-AT";
    private static final String RETURN_URL = "MS365-PENDING-RETURN-URL";
    private static final String DIRECTORY_ID = "MS365-CONNECTED-DIRECTORY-ID";
    private static final String STATUS = "MS365-CONNECTION-STATUS";
    private static final String VERIFIED_MAILBOX = "MS365-VERIFIED-MAILBOX";
    private static final String CONNECTED_AT = "MS365-CONNECTED-AT";
    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private final SettingService settings;
    private final TenantAdminService tenantAdminService;
    private final GcpTenantSecretService secrets;
    private final BankStatementMailConfigurationService configuration;
    private final ObjectMapper mapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    @Value("${MAWA_MS365_SAAS_CLIENT_ID:}") private String clientId;
    @Value("${MAWA_MS365_SAAS_CLIENT_SECRET_REF:}") private String clientSecretRef;
    @Value("${MAWA_MS365_SAAS_REDIRECT_URI:}") private String redirectUri;
    @Value("${MAWA_MS365_SAAS_ALLOW_LOCALHOST:false}") private boolean allowLocalhost;

    public record ConnectionStart(String authorizationUrl, String status) { }
    public record ApplicationInfo(String applicationId, String redirectUri) { }
    public record ConnectionResult(String returnUrl, String status) { }
    public record Verification(String status, String directoryTenantId, String mailbox) { }

    public ConnectionStart start() {
        requireSaasConfiguration();
        requireAppConfigured();
        String mawaTenant = TenantContext.getCurrentTenant();
        if (mawaTenant == null || !mawaTenant.matches("[A-Za-z0-9_-]{1,128}"))
            throw new IllegalStateException("MAWA tenant context is missing");
        String returnUrl = allowedTenantReturnUrl(returnHost(mawaTenant));
        byte[] random = new byte[32]; secureRandom.nextBytes(random);
        String state = mawaTenant + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        byte[] nonceBytes = new byte[32]; secureRandom.nextBytes(nonceBytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);

        put(PENDING_HASH, sha256(state));
        put(PENDING_NONCE, nonce);
        put(PENDING_EXPIRY, String.valueOf(Instant.now().plus(STATE_TTL).getEpochSecond()));
        put(RETURN_URL, returnUrl);
        put(STATUS, "AUTHORIZATION_PENDING");
        put("ENABLED", "false");
        String url = "https://login.microsoftonline.com/organizations/oauth2/v2.0/authorize"
                + "?client_id=" + encode(clientId)
                + "&response_type=code&response_mode=query"
                + "&redirect_uri=" + encode(redirectUri)
                + "&scope=" + encode("openid profile")
                + "&prompt=admin_consent"
                + "&state=" + encode(state)
                + "&nonce=" + encode(nonce);
        return new ConnectionStart(url, "AUTHORIZATION_PENDING");
    }

    /** Invoked only by the unauthenticated Microsoft redirect, after the callback filter has
     * established a tenant from the state prefix. The prefix is NEVER sufficient authorization:
     * the random state hash and expiry must match the pending request in that tenant's schema. */
    public ConnectionResult complete(String state, String code) {
        if (state == null || code == null || code.isBlank() || !validStateFormat(state))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Microsoft authorization response");
        String prefix = state.substring(0, state.indexOf('.'));
        if (!prefix.equals(TenantContext.getCurrentTenant()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Microsoft authorization state");
        String savedHash = get(PENDING_HASH);
        long expiry = epoch(get(PENDING_EXPIRY));
        if (savedHash == null || expiry < Instant.now().getEpochSecond()
                || !constantTimeEquals(savedHash, sha256(state)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Microsoft authorization expired; reconnect");
        String nonce = get(PENDING_NONCE);
        String returnUrl = get(RETURN_URL);
        // Consume BEFORE exchanging the code to prevent state replay even if exchange fails.
        clearPending();
        requireAppConfigured();
        JsonNode tokens = exchangeToken("https://login.microsoftonline.com/organizations/oauth2/v2.0/token",
                Map.of("client_id", clientId, "client_secret", appSecret(), "code", code,
                        "redirect_uri", redirectUri, "grant_type", "authorization_code", "scope", "openid profile"));
        String directory = verifiedDirectoryId(tokens.path("id_token").asText(), nonce);
        put(DIRECTORY_ID, directory);
        put(CONNECTED_AT, Instant.now().toString());
        put(VERIFIED_MAILBOX, "");
        put(STATUS, "MAILBOX_ACCESS_REQUIRED");
        put("ENABLED", "false");
        // The customer may already have assigned mailbox-scoped RBAC; test it now.
        try { verifyMailbox(); } catch (RuntimeException ignored) { /* RBAC may be configured later. */ }
        return new ConnectionResult(returnUrl, get(STATUS));
    }

    public ApplicationInfo appInfo() {
        requireAppConfigured();
        return new ApplicationInfo(clientId, redirectUri);
    }

    public Verification verifyMailbox() {
        requireSaasConfiguration();
        String directory = connectedDirectory();
        String mailbox = configuration.get().mailbox();
        if (mailbox == null || mailbox.isBlank())
            throw new IllegalArgumentException("Save the bank statement mailbox address first");
        try {
            String accessToken = appToken(directory);
            String url = "https://graph.microsoft.com/v1.0/users/" + encodePath(mailbox)
                    + "/mailFolders/inbox/messages?$top=1&$select=id";
            getJson(url, accessToken);
            put(VERIFIED_MAILBOX, mailbox.trim().toLowerCase(java.util.Locale.ROOT));
            put(STATUS, "VERIFIED");
            return new Verification("VERIFIED", directory, mailbox);
        } catch (RuntimeException ex) {
            put(VERIFIED_MAILBOX, "");
            put(STATUS, "MAILBOX_ACCESS_REQUIRED");
            put("ENABLED", "false");
            throw new IllegalStateException("MAWA could not read this mailbox. Confirm Exchange Online scoped Application Mail.Read permissions and retry.");
        }
    }

    public Verification disconnect() {
        put("ENABLED", "false");
        put(DIRECTORY_ID, "");
        put(STATUS, "DISCONNECTED");
        put(VERIFIED_MAILBOX, "");
        put(CONNECTED_AT, "");
        clearPending();
        return new Verification("DISCONNECTED", "", "");
    }

    public String connectionStatus() {
        String value = get(STATUS);
        return value == null || value.isBlank() ? "DISCONNECTED" : value;
    }
    public String directoryIdOrBlank() { String id = get(DIRECTORY_ID); return id == null ? "" : id; }
    public String verifiedMailboxOrBlank() {
        String value = get(VERIFIED_MAILBOX); return value == null ? "" : value;
    }
    public String connectedAtOrBlank() {
        String value = get(CONNECTED_AT); return value == null ? "" : value;
    }
    public void mailboxChanged() {
        if ("VERIFIED".equals(connectionStatus())) put(STATUS, "MAILBOX_ACCESS_REQUIRED");
        put(VERIFIED_MAILBOX, "");
        put("ENABLED", "false");
    }
    public String appTokenForConnectedTenant() {
        if (!"VERIFIED".equals(connectionStatus()))
            throw new IllegalStateException("Microsoft mailbox access has not been verified");
        String mailbox = configuration.get().mailbox();
        if (!verifiedMailboxOrBlank().equalsIgnoreCase(mailbox))
            throw new IllegalStateException("Bank statement mailbox changed; verify access again");
        return appToken(connectedDirectory());
    }

    /** A short-lived, mailbox-scoped client-credentials access token. Never saved in settings. */
    private String appToken(String directory) {
        requireAppConfigured();
        JsonNode tokens = exchangeToken("https://login.microsoftonline.com/" + directory + "/oauth2/v2.0/token",
                Map.of("client_id", clientId, "client_secret", appSecret(),
                        "grant_type", "client_credentials", "scope", "https://graph.microsoft.com/.default"));
        String token = tokens.path("access_token").asText();
        if (token.isBlank()) throw new IllegalStateException("Microsoft did not issue an access token");
        return token;
    }

    /** Validate signature, issuer, expiry, audience and one-time nonce of the ID token.
     * The unverified `tid` is used ONLY to select the JWKS URL; it is checked again after verification. */
    String verifiedDirectoryId(String rawToken, String expectedNonce) {
        try {
            String[] parts = rawToken.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("Invalid Microsoft identity token");
            JsonNode untrusted = mapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            String tenant = untrusted.path("tid").asText();
            UUID.fromString(tenant);
            String issuer = "https://login.microsoftonline.com/" + tenant + "/v2.0";
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(
                    "https://login.microsoftonline.com/" + tenant + "/discovery/v2.0/keys").build();
            decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
            Jwt jwt = decoder.decode(rawToken);
            if (!tenant.equals(jwt.getClaimAsString("tid")) || !jwt.getAudience().contains(clientId)
                    || !constantTimeEquals(expectedNonce, jwt.getClaimAsString("nonce")))
                throw new IllegalArgumentException("Microsoft tenant, audience or nonce does not match");
            return tenant;
        } catch (Exception ex) {
            throw new IllegalStateException("Microsoft organization identity could not be verified");
        }
    }

    public static boolean validStateFormat(String state) {
        return state != null && state.matches("[A-Za-z0-9_-]{1,128}\\.[A-Za-z0-9_-]{43}");
    }
    static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) { throw new IllegalStateException("Digest unavailable", ex); }
    }
    private static boolean constantTimeEquals(String a, String b) {
        return a != null && b != null && MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
    private void requireSaasConfiguration() {
        if (!"MICROSOFT_365_SAAS".equals(configuration.get().provider()))
            throw new IllegalStateException("Select Microsoft 365 (MAWA-managed) in bank mail settings first");
    }
    private String connectedDirectory() {
        String value = directoryIdOrBlank();
        try { UUID.fromString(value); return value; }
        catch (RuntimeException ex) { throw new IllegalStateException("Connect your Microsoft 365 organization first"); }
    }
    private void requireAppConfigured() {
        try { UUID.fromString(clientId); } catch (RuntimeException ex) {
            throw new IllegalStateException("MAWA Microsoft 365 application ID is not configured"); }
        if (redirectUri == null || !redirectUri.startsWith("https://") || !redirectUri.endsWith("/v2/bank-transactions/microsoft/callback"))
            throw new IllegalStateException("MAWA Microsoft 365 HTTPS redirect URI is not configured");
        if (!secrets.isSecretReference(clientSecretRef))
            throw new IllegalStateException("MAWA Microsoft 365 client secret reference is missing");
    }
    private String appSecret() {
        String secret = secrets.accessSecretReference(clientSecretRef);
        if (secret == null || secret.isBlank())
            throw new IllegalStateException("MAWA Microsoft 365 client secret is unavailable");
        return secret;
    }
    private String returnHost(String tenantId) {
        // JWT requests normally establish only TenantContext.currentTenant; use the
        // server-side tenant registry, never a browser-controlled redirect URI.
        TenantDto tenant = tenantAdminService.getAll().stream()
                .filter(t -> t != null && tenantId.equals(t.getId()))
                .findFirst().orElseThrow(() -> new IllegalStateException("Tenant not registered"));
        for (String candidate : new String[]{TenantContext.getCurrentTenantURL(), tenant.getHost(), tenant.getUrl()}) {
            try { allowedTenantReturnUrl(candidate); return candidate; }
            catch (RuntimeException ignored) { /* Try another registered tenant address. */ }
        }
        throw new IllegalStateException("Tenant has no valid MAWA ERP application host");
    }
    private String allowedTenantReturnUrl(String host) {
        if (host == null) throw new IllegalStateException("Cannot determine the MAWA tenant host; sign in through the tenant URL");
        String normalized = host.trim().toLowerCase(java.util.Locale.ROOT)
                .replaceFirst("^https?://", "").replaceFirst("/.*$", "");
        if (!normalized.matches("[a-z0-9.-]{3,200}") || !(normalized.equals("app.mawa.co.za")
                || normalized.endsWith(".app.mawa.co.za")
                || (allowLocalhost && normalized.equals("localhost"))))
            throw new IllegalStateException("Tenant URL is not an approved MAWA application host");
        return "https://" + normalized + "/system-configuration/bank-statement-mail";
    }
    private JsonNode exchangeToken(String url, Map<String,String> params) {
        String body = params.entrySet().stream().map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(java.util.stream.Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return sendJson(request);
    }
    private JsonNode getJson(String url, String bearer) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET()
                .header("Authorization", "Bearer " + bearer)
                .header("Accept", "application/json").timeout(Duration.ofSeconds(30)).build();
        return sendJson(req);
    }
    private JsonNode sendJson(HttpRequest request) {
        try {
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() > 299)
                throw new IllegalStateException("Microsoft API returned HTTP " + response.statusCode());
            if (response.body().length > 1_500_000) throw new IllegalStateException("Microsoft API response too large");
            return mapper.readTree(response.body());
        } catch (IOException ex) { throw new IllegalStateException("Microsoft connection unavailable", ex); }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("Microsoft request interrupted", ex);
        }
    }
    private long epoch(String value) {
        try { return Long.parseLong(value); } catch (RuntimeException ex) { return 0; }
    }
    private void clearPending() {
        put(PENDING_HASH, ""); put(PENDING_NONCE, "");
        put(PENDING_EXPIRY, ""); put(RETURN_URL, "");
    }
    private void put(String key, String value) { settings.upsertSetting(key, GROUP, value); }
    private String get(String key) { return settings.getSetting(key, GROUP); }
    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private String encodePath(String value) { return encode(value).replace("+", "%20"); }
}
