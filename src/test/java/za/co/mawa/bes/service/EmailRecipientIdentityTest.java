package za.co.mawa.bes.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmailRecipientIdentityTest {
    private static final String KEY = "0123456789abcdef0123456789abcdef";

    @Test void stableCaseInsensitiveHmacAndNoCleartextRecipient() {
        EmailRecipientIdentity identity = new EmailRecipientIdentity(KEY);
        String a = identity.fingerprint("User@Example.com");
        assertTrue(a.matches("[0-9a-f]{64}"));
        assertEquals(a, identity.fingerprint("user@example.com"));
        assertNotEquals(a, identity.fingerprint("other@example.com"));
        assertFalse(a.contains("example.com"));
    }

    @Test void invalidOrMissingKeyIsSafe() {
        assertThrows(IllegalArgumentException.class, () -> new EmailRecipientIdentity("short"));
        assertNull(new EmailRecipientIdentity("").fingerprint("user@example.com"));
        assertThrows(IllegalArgumentException.class,
                () -> new EmailRecipientIdentity(KEY).fingerprint("bad\r\n@example.com"));
    }
}
