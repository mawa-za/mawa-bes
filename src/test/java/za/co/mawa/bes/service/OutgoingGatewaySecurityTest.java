package za.co.mawa.bes.service;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class OutgoingGatewaySecurityTest {
    private static final String SECRET = "test-secret-with-at-least-32-bytes-abc";
    private static final String TENANT = "tenant_qa";
    private static final String PATH = "/internal/admin/tenant/tenant_qa/outgoing-email";
    private static final byte[] BODY = "{\"recipient\":\"qa@example.org\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    private static final String NONCE = "12345678-1234-1234-1234-123456789abc";

    private OutgoingGatewaySecurity verifier() {
        return new OutgoingGatewaySecurity(SECRET, SECRET, SECRET, TENANT, "billing_qa", "reporting_qa");
    }

    @Test void allowsOnlyServiceKeyAndAllowlistedTenant() {
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        String signature = OutgoingGatewaySecurity.sign(SECRET, "admin", timestamp, NONCE, PATH, BODY);
        assertEquals("admin", verifier().authenticate("admin", timestamp, NONCE, signature, "POST", PATH, TENANT, BODY));
        assertThrows(SecurityException.class, () -> verifier().authenticate("billing", timestamp, NONCE, signature, "POST", PATH, TENANT, BODY));
        assertThrows(SecurityException.class, () -> verifier().authenticate("admin", timestamp, NONCE, signature, "POST", PATH, "tenant_other", BODY));
    }

    @Test void rejectsBodyTamperWrongPathAndOldTimestamp() {
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        String signature = OutgoingGatewaySecurity.sign(SECRET, "admin", timestamp, NONCE, PATH, BODY);
        assertThrows(SecurityException.class, () -> verifier().authenticate("admin", timestamp, NONCE, signature, "POST", PATH, TENANT, "wrong".getBytes()));
        assertThrows(SecurityException.class, () -> verifier().authenticate("admin", timestamp, NONCE, signature, "POST", PATH + "/other", TENANT, BODY));
        assertThrows(SecurityException.class, () -> verifier().authenticate("admin", Long.toString(Instant.now().minusSeconds(600).getEpochSecond()), NONCE, signature, "POST", PATH, TENANT, BODY));
    }

    @Test void acceptsOnlySignedReadOnlyStatusEndpointAndBindsTenant() {
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        String statusPath = PATH + "/status";
        byte[] body = "{\"reference\":\"INV-100\",\"requestId\":\"12345678-1234-1234-1234-123456789abc\"}"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String signature = OutgoingGatewaySecurity.sign(SECRET, "admin", timestamp, NONCE, statusPath, body);
        assertEquals("admin", verifier().authenticate("admin", timestamp, NONCE, signature,
                "POST", statusPath, TENANT, body));
        assertThrows(SecurityException.class, () -> verifier().authenticate("admin", timestamp,
                NONCE, signature, "GET", statusPath, TENANT, body));
        assertThrows(SecurityException.class, () -> verifier().authenticate("admin", timestamp,
                NONCE, signature, "POST", PATH, TENANT, body));
        assertThrows(SecurityException.class, () -> verifier().authenticate("admin", timestamp,
                NONCE, signature, "POST", statusPath, "wrong_tenant", body));
        assertThrows(SecurityException.class, () -> verifier().authenticate("billing", timestamp,
                NONCE, signature, "POST", statusPath, TENANT, body));
    }

    @Test void rejectsMissingConfigurationAndWeakSecrets() {
        var insecure = new OutgoingGatewaySecurity("short", "", "", TENANT, "", "");
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        String signature = OutgoingGatewaySecurity.sign("short", "admin", timestamp, NONCE, PATH, BODY);
        assertThrows(SecurityException.class, () -> insecure.authenticate("admin", timestamp, NONCE, signature, "POST", PATH, TENANT, BODY));
    }
}
