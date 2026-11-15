package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Per-service HMAC authentication for the *internal* email queue endpoint.
 * The client signs exact raw JSON bytes, path, nonce and UTC timestamp.  Never accept
 * X-Tenant-Id as an identity claim. Tenant permission is a server-side allowlist.
 */
@Component
public class OutgoingGatewaySecurity {
    private final String adminSecret, billingSecret, reportingSecret;
    private final String adminTenants, billingTenants, reportingTenants;

    public OutgoingGatewaySecurity(
            @Value("${mawa.outgoing.gateway.admin.secret:}") String adminSecret,
            @Value("${mawa.outgoing.gateway.billing.secret:}") String billingSecret,
            @Value("${mawa.outgoing.gateway.reporting.secret:}") String reportingSecret,
            @Value("${mawa.outgoing.gateway.admin.tenants:}") String adminTenants,
            @Value("${mawa.outgoing.gateway.billing.tenants:}") String billingTenants,
            @Value("${mawa.outgoing.gateway.reporting.tenants:}") String reportingTenants) {
        this.adminSecret = adminSecret;
        this.billingSecret = billingSecret;
        this.reportingSecret = reportingSecret;
        this.adminTenants = adminTenants;
        this.billingTenants = billingTenants;
        this.reportingTenants = reportingTenants;
    }

    public String authenticate(String client, String timestamp, String nonce, String signature,
                               String method, String requestPath, String tenant, byte[] body) {
        if (client == null || !client.matches("admin|billing|reporting")) {
            throw new SecurityException("Unapproved mail service");
        }
        // Both submission and read-only status lookup use the same signed, tenant-bound protocol.
        String basePath = "/internal/admin/tenant/" + tenant + "/outgoing-email";
        if (tenant == null || !tenant.matches("[A-Za-z0-9_-]{1,128}")
                || !(basePath.equals(requestPath) || (basePath + "/status").equals(requestPath))
                || !"POST".equals(method)) {
            throw new SecurityException("Invalid tenant mail route");
        }
        if (nonce == null || !nonce.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            throw new SecurityException("Invalid mail request nonce");
        }
        final long submitted;
        try { submitted = Long.parseLong(timestamp); }
        catch (Exception ex) { throw new SecurityException("Invalid mail request timestamp"); }
        long now = Instant.now().getEpochSecond();
        if (submitted < now - 300 || submitted > now + 300) {
            throw new SecurityException("Mail request timestamp expired");
        }
        String secret = switch (client) {
            case "admin" -> adminSecret;
            case "billing" -> billingSecret;
            default -> reportingSecret;
        };
        String tenants = switch (client) {
            case "admin" -> adminTenants;
            case "billing" -> billingTenants;
            default -> reportingTenants;
        };
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32
                || tenants == null || Arrays.stream(tenants.split(","))
                         .map(String::trim).noneMatch(t -> t.equals(tenant) || t.equals("*"))) {
            throw new SecurityException("Mail service not configured or tenant not allowed");
        }
        String expected = sign(secret, client, timestamp, nonce, requestPath, body);
        if (signature == null || !signature.matches("[a-f0-9]{64}")
                || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                                          signature.getBytes(StandardCharsets.US_ASCII))) {
            throw new SecurityException("Invalid mail request signature");
        }
        return client;
    }

    public static String sign(String secret, String client, String timestamp, String nonce,
                              String path, byte[] rawBody) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            String hash = HexFormat.of().formatHex(sha.digest(rawBody));
            String canonical = "MAWA-OUTGOING-V1\n" + client + "\n" + timestamp + "\n"
                    + nonce + "\n" + path + "\n" + hash;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Mail signing algorithm unavailable", ex);
        }
    }
}
