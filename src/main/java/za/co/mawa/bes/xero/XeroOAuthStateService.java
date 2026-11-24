package za.co.mawa.bes.xero;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.spring.TenantHostNormalizer;
import za.co.mawa.bes.service.SettingService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One-time OAuth state, persisted per MAWA tenant so the Xero callback works
 * across Cloud Run instances. The host is only a tenant lookup hint; a random
 * nonce must match the server-side hash before an OAuth code is accepted.
 */
@Service
public class XeroOAuthStateService {
    private static final String GROUP = "XERO";
    private static final String HASH = "OAUTH-STATE-HASH";
    private static final String ISSUED_AT = "OAUTH-STATE-ISSUED-AT";
    private static final Pattern STATE = Pattern.compile("^([a-z0-9][a-z0-9.-]{0,250})\\.([a-zA-Z0-9_-]{43})$");
    private static final long TTL_MILLIS = Duration.ofMinutes(10).toMillis();

    private final SettingService settings;
    private final SecureRandom random;
    private final Clock clock;

    @Autowired
    public XeroOAuthStateService(SettingService settings) {
        this(settings, new SecureRandom(), Clock.systemUTC());
    }

    XeroOAuthStateService(SettingService settings, SecureRandom random, Clock clock) {
        this.settings = settings;
        this.random = random;
        this.clock = clock;
    }

    public String issue(String tenantHost) {
        String host = TenantHostNormalizer.normalize(tenantHost);
        if (!StringUtils.hasText(host) || !host.matches("[a-z0-9][a-z0-9.-]{0,250}")) {
            throw new IllegalArgumentException("A valid MAWA tenant host is required for Xero authorisation");
        }
        byte[] nonce = new byte[32];
        random.nextBytes(nonce);
        String state = host + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(nonce);
        settings.upsertSetting(HASH, GROUP, hash(state));
        settings.upsertSetting(ISSUED_AT, GROUP, Long.toString(clock.millis()));
        return state;
    }

    /** Return only the untrusted host lookup hint; never treat it as OAuth validation. */
    public static String tenantHostHint(String state) {
        if (state == null) return null;
        Matcher match = STATE.matcher(state);
        return match.matches() ? match.group(1) : null;
    }

    public void validateAndConsume(String state, String resolvedTenantHost) {
        String host = tenantHostHint(state);
        if (host == null || !host.equals(TenantHostNormalizer.normalize(resolvedTenantHost))) {
            throw new IllegalArgumentException("Invalid Xero authorisation state. Reconnect from MAWA Settings.");
        }
        String expectedHash = settings.getSetting(HASH, GROUP);
        String issued = settings.getSetting(ISSUED_AT, GROUP);
        long issuedAt;
        try {
            issuedAt = Long.parseLong(issued);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Xero authorisation session is missing. Reconnect from MAWA Settings.");
        }
        long age = clock.millis() - issuedAt;
        if (age < 0 || age > TTL_MILLIS || !StringUtils.hasText(expectedHash)
                || !MessageDigest.isEqual(expectedHash.getBytes(StandardCharsets.US_ASCII),
                        hash(state).getBytes(StandardCharsets.US_ASCII))) {
            throw new IllegalArgumentException("Xero authorisation session expired or did not match. Reconnect from MAWA Settings.");
        }
        // Reject the same callback again. Secret/token exchange only occurs afterwards.
        settings.upsertSetting(HASH, GROUP, "");
        settings.upsertSetting(ISSUED_AT, GROUP, "0");
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
