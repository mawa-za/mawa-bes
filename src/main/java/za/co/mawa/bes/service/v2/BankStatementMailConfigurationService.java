package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.gcp.GcpTenantSecretService;
import za.co.mawa.bes.service.SettingService;
import java.util.Locale;

@Service @RequiredArgsConstructor
public class BankStatementMailConfigurationService {
    static final String GROUP = "BANK-STATEMENT-MAIL";
    private final SettingService settings;
    private final GcpTenantSecretService secrets;

    public record Configuration(boolean enabled, String provider, String mailbox, String sender,
                                String subjectFilter, String tenantId, String clientId,
                                String clientSecretRef, String refreshTokenRef, int lookbackDays,
                                String lastSuccessDate, String accountNumbers, Boolean requireDmarc,
                                String connectionStatus, String connectedDirectoryId,
                                String verifiedMailbox, String connectedAt) { }

    public Configuration get() {
        String days = settings.getSetting("LOOKBACK-DAYS", GROUP);
        int lookback = 14;
        try { if (days != null) lookback = Math.max(1, Math.min(365, Integer.parseInt(days))); }
        catch (NumberFormatException ignored) { }
        return new Configuration("true".equalsIgnoreCase(value("ENABLED")), value("PROVIDER"),
                value("MAILBOX"), value("SENDER"), value("SUBJECT-FILTER"),
                "MICROSOFT_365_SAAS".equals(value("PROVIDER")) ? "" : value("TENANT-ID"),
                "MICROSOFT_365_SAAS".equals(value("PROVIDER")) ? "" : value("CLIENT-ID"),
                "MICROSOFT_365_SAAS".equals(value("PROVIDER")) ? "" : value("CLIENT-SECRET-REF"),
                value("REFRESH-TOKEN-REF"), lookback, value("LAST-SUCCESS-DATE"),
                value("ACCOUNT-NUMBERS"), !"false".equalsIgnoreCase(value("REQUIRE-DMARC")),
                nonBlank(value("MS365-CONNECTION-STATUS"), "DISCONNECTED"),
                value("MS365-CONNECTED-DIRECTORY-ID"), value("MS365-VERIFIED-MAILBOX"),
                value("MS365-CONNECTED-AT"));
    }

    public Configuration save(Configuration input) {
        if (input == null) throw new IllegalArgumentException("Configuration is required");
        String provider = safe(input.provider()).toUpperCase(Locale.ROOT);
        if (!provider.equals("MICROSOFT_365") && !provider.equals("GMAIL")
                && !provider.equals("MICROSOFT_365_SAAS")) {
            throw new IllegalArgumentException("Provider must be MICROSOFT_365_SAAS, MICROSOFT_365 or GMAIL");
        }
        if (!safe(input.mailbox()).matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("A valid mailbox email address is required");
        if (!safe(input.sender()).matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("A trusted bank sender email is required");
        if (!provider.equals("MICROSOFT_365_SAAS") && safe(input.clientId()).isBlank())
            throw new IllegalArgumentException("OAuth client ID is required");
        if (provider.equals("MICROSOFT_365") && safe(input.tenantId()).isBlank())
            throw new IllegalArgumentException("Microsoft 365 directory tenant ID is required");
        if (!provider.equals("MICROSOFT_365_SAAS") && !secrets.isSecretReference(input.clientSecretRef()))
            throw new IllegalArgumentException("OAuth client secret must be a Google Secret Manager reference");
        if (provider.equals("GMAIL") && !secrets.isSecretReference(input.refreshTokenRef()))
            throw new IllegalArgumentException("Gmail refresh token must be a Google Secret Manager reference");
        if (!safe(input.accountNumbers()).matches("[0-9]+(\\s*,\\s*[0-9]+)*"))
            throw new IllegalArgumentException("Specify the bank account number(s), separated by commas");
        boolean saas = provider.equals("MICROSOFT_365_SAAS");
        String previousMailbox = value("MAILBOX");
        String linkedMailbox = value("MS365-VERIFIED-MAILBOX");
        boolean mailboxChanged = previousMailbox != null && !previousMailbox.isBlank()
                && !previousMailbox.equalsIgnoreCase(safe(input.mailbox()));
        boolean mailboxVerified = "VERIFIED".equals(value("MS365-CONNECTION-STATUS"))
                && linkedMailbox != null && linkedMailbox.equalsIgnoreCase(safe(input.mailbox()));
        // Validate before writing anything; a rejected save must not mutate connection state.
        if (saas && input.enabled() && (!mailboxVerified || mailboxChanged))
            throw new IllegalArgumentException("Verify Microsoft mailbox access before enabling imports");
        if (saas && mailboxChanged && "VERIFIED".equals(value("MS365-CONNECTION-STATUS"))) {
            put("MS365-VERIFIED-MAILBOX", "");
            put("MS365-CONNECTION-STATUS", "MAILBOX_ACCESS_REQUIRED");
            put("ENABLED", "false");
        }
        put("PROVIDER", provider);
        put("MAILBOX", safe(input.mailbox()));
        put("SENDER", safe(input.sender()).toLowerCase(Locale.ROOT));
        put("SUBJECT-FILTER", safe(input.subjectFilter()));
        // The MAWA-managed Microsoft application credentials belong to the MAWA runtime,
        // never to individual customer tenant settings. Preserve legacy/Gmail fields only.
        if (!saas) {
            put("TENANT-ID", safe(input.tenantId()));
            put("CLIENT-ID", safe(input.clientId()));
            put("CLIENT-SECRET-REF", safe(input.clientSecretRef()));
        }
        if (!saas) put("REFRESH-TOKEN-REF", safe(input.refreshTokenRef()));
        put("LOOKBACK-DAYS", String.valueOf(Math.max(1, Math.min(365, input.lookbackDays()))));
        put("ACCOUNT-NUMBERS", safe(input.accountNumbers()).replaceAll("\\s+", ""));
        put("REQUIRE-DMARC", String.valueOf(!Boolean.FALSE.equals(input.requireDmarc())));
        put("ENABLED", String.valueOf(input.enabled()));
        return get();
    }

    String secret(String reference) { return secrets.accessSecretReference(reference); }
    private String value(String key) { return settings.getSetting(key, GROUP); }
    private void put(String key, String value) { settings.upsertSetting(key, GROUP, value); }
    private String safe(String value) { return value == null ? "" : value.trim(); }
    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
    public void recordSuccess(String date) { put("LAST-SUCCESS-DATE", date); }
}
