package za.co.mawa.bes.service;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.gcp.GcpTenantSecretService;

import java.util.Locale;
import java.util.UUID;

/**
 * Tenant-local outgoing email settings. Credentials are NEVER stored in settings or returned
 * to the ERP. Each tenant's Microsoft Graph credential lives in Google Secret Manager.
 * The shared provider remains the default for existing tenants.
 */
@Service
public class OutgoingEmailConfigurationService {
    private static final String GROUP = "OUTGOING-EMAIL";
    public static final String SHARED = "MAWA_SHARED";
    public static final String TENANT_GRAPH = "TENANT_MICROSOFT_365";

    private final SettingService settings;
    private final GcpTenantSecretService secrets;
    private final Environment environment;

    public OutgoingEmailConfigurationService(SettingService settings, GcpTenantSecretService secrets,
                                             Environment environment) {
        this.settings = settings;
        this.secrets = secrets;
        this.environment = environment;
    }

    /** Never includes client credentials, even when a secret has already been saved. */
    public record Configuration(boolean enabled, String provider, String senderName, String replyTo,
                                String mailbox, String directoryId, String clientId,
                                boolean clientSecretConfigured) { }

    /** Optional secret: blank means keep existing, not clear it. */
    public record Update(boolean enabled, String provider, String senderName, String replyTo,
                         String mailbox, String directoryId, String clientId, String clientSecret) { }

    public Configuration get() {
        requireTenant();
        String provider = get("PROVIDER");
        if (!TENANT_GRAPH.equals(provider)) provider = SHARED;
        return new Configuration(!"false".equalsIgnoreCase(get("ENABLED")), provider,
                safe(get("SENDER-NAME")), safe(get("REPLY-TO")), safe(get("MAILBOX")),
                safe(get("DIRECTORY-ID")), safe(get("CLIENT-ID")),
                StringUtils.hasText(get("CLIENT-SECRET-REF")));
    }

    /** All fields are validated before changing persisted settings. */
    public Configuration save(Update input) {
        requireTenant();
        if (input == null) throw new IllegalArgumentException("Outgoing email settings are required");
        String provider = safe(input.provider()).toUpperCase(Locale.ROOT);
        if (!SHARED.equals(provider) && !TENANT_GRAPH.equals(provider))
            throw new IllegalArgumentException("Unsupported outgoing email provider");
        String name = safe(input.senderName());
        if (name.length() > 120 || name.contains("\r") || name.contains("\n"))
            throw new IllegalArgumentException("Invalid email sender display name");
        String replyTo = safe(input.replyTo());
        if (!replyTo.isBlank()) SharedOutgoingEmailService.validatedSingleAddress(replyTo);
        String mailbox = safe(input.mailbox()).toLowerCase(Locale.ROOT);
        String directoryId = safe(input.directoryId());
        String clientId = safe(input.clientId());
        String secret = input.clientSecret() == null ? "" : input.clientSecret();
        String secretRef = get("CLIENT-SECRET-REF");
        if (TENANT_GRAPH.equals(provider)) {
            SharedOutgoingEmailService.validatedSingleAddress(mailbox);
            if (!validUuid(directoryId) || !validUuid(clientId))
                throw new IllegalArgumentException("Microsoft directory ID and application ID must be valid UUIDs");
            if (StringUtils.hasText(secret)) {
                if (secret.length() > 4096 || secret.contains("\n") || secret.contains("\r"))
                    throw new IllegalArgumentException("Invalid Microsoft application secret");
                // A deterministic per-tenant reference, never based on untrusted user input.
                secretRef = secretReference();
                secrets.createOrAddSecretVersion(secretRef, secret);
            } else if (!StringUtils.hasText(secretRef) || !secrets.hasAccessibleSecretVersion(secretRef)) {
                throw new IllegalArgumentException("A Microsoft application secret is required");
            }
        }
        put("PROVIDER", provider);
        put("ENABLED", String.valueOf(input.enabled()));
        put("SENDER-NAME", name);
        put("REPLY-TO", replyTo);
        // Preserve the customer's connection details when switching back to MAWA shared sender.
        if (TENANT_GRAPH.equals(provider)) {
            put("MAILBOX", mailbox);
            put("DIRECTORY-ID", directoryId);
            put("CLIENT-ID", clientId);
            put("CLIENT-SECRET-REF", secretRef);
        }
        return get();
    }

    public String tenantGraphSecret() {
        requireTenant();
        String ref = get("CLIENT-SECRET-REF");
        if (!StringUtils.hasText(ref)) throw new IllegalStateException("Microsoft outgoing email credentials not configured");
        return secrets.accessSecretReference(ref);
    }

    private String secretReference() {
        String tenant = requireTenant();
        // Isolate prod/hotfix secrets even when those services run in the same GCP project.
        String namespace = safe(environment.getProperty("MAWA_OUTGOING_SECRET_NAMESPACE"));
        // K_SERVICE is populated by Cloud Run and separates hotfix from prod if
        // the explicit namespace hasn't been provisioned yet.
        if (namespace.isEmpty()) namespace = safe(environment.getProperty("K_SERVICE"));
        if (namespace.isEmpty()) namespace = safe(environment.getProperty("spring.profiles.active", "dev"));
        namespace = namespace.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
        if (namespace.length() > 40) namespace = namespace.substring(0, 40);
        return "mawa-outgoing-" + namespace + "-" + tenant.toLowerCase(Locale.ROOT);
    }

    private static String requireTenant() {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null || !tenant.matches("[A-Za-z0-9_-]{1,128}"))
            throw new IllegalStateException("Valid tenant context required for outgoing email configuration");
        return tenant;
    }

    private static boolean validUuid(String value) {
        try { return UUID.fromString(value).toString().equalsIgnoreCase(value); }
        catch (RuntimeException ex) { return false; }
    }
    private String get(String key) { return settings.getSetting(key, GROUP); }
    private void put(String key, String value) { settings.upsertSetting(key, GROUP, value); }
    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
