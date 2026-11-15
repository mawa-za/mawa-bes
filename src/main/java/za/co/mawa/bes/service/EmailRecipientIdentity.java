package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Locale;

/** Per-environment HMAC recipient identity for tenant-local bounce/complaint suppression.
 *  No raw recipient addresses or unsalted dictionary-searchable digests are persisted.
 *  Configure MAWA_OUTGOING_FEEDBACK_RECIPIENT_KEY via Secret Manager, not in source control.
 */
@Component
public class EmailRecipientIdentity {
    private final byte[] secret;

    public EmailRecipientIdentity(@Value("${mawa.outgoing.feedback.recipient-key:}") String key) {
        if (key != null && !key.isEmpty() && key.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("Outgoing email recipient HMAC key must contain at least 32 bytes");
        }
        this.secret = key == null || key.isBlank() ? null : key.getBytes(StandardCharsets.UTF_8);
    }

    public boolean configured() { return secret != null; }

    /** For now a single verified recipient; multi-recipient delivery events require per-recipient fan-out. */
    public String fingerprint(String recipient) {
        if (secret == null) return null;
        String email = SharedOutgoingEmailService.validatedSingleAddress(recipient)
                .toLowerCase(Locale.ROOT);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(email.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to hash outgoing mail recipient", e);
        }
    }
}
