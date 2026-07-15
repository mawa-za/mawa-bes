package za.co.mawa.bes.configuration.gcp;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.spring.TenantHostNormalizer;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.service.TenantAdminService;

import java.util.Locale;

/**
 * Generates immutable tenant-specific Google Secret Manager names using:
 * mawa-{environment}-{tenant-host-normalised}-{integration}-{secret-purpose}.
 */
@Service
public class TenantSecretNameService {

    private final Environment environment;
    private final TenantAdminService tenantAdminService;

    public TenantSecretNameService(Environment environment, TenantAdminService tenantAdminService) {
        this.environment = environment;
        this.tenantAdminService = tenantAdminService;
    }

    public String currentTenantSecretName(String integration, String purpose) {
        return buildSecretName(resolveCurrentTenantHost(), integration, purpose);
    }

    public String buildSecretName(String tenantReference, String integration, String purpose) {
        if (!StringUtils.hasText(integration) || !StringUtils.hasText(purpose)) {
            throw new IllegalArgumentException("Integration and secret purpose are required");
        }
        String tenantKey = normalisePart(firstNonBlank(TenantHostNormalizer.normalize(tenantReference), tenantReference, "tenant"));
        return "mawa-" + environmentName() + "-" + tenantKey + "-"
                + normalisePart(integration) + "-" + normalisePart(purpose);
    }

    public String resolveCurrentTenantHost() {
        String requestHost = TenantHostNormalizer.normalize(TenantContext.getCurrentTenantURL());
        if (StringUtils.hasText(requestHost)) {
            return requestHost;
        }

        String tenantId = TenantContext.getCurrentTenant();
        if (StringUtils.hasText(tenantId)) {
            try {
                TenantDto tenant = tenantAdminService.getAll().stream()
                        .filter(candidate -> tenantId.equals(candidate.getId()))
                        .findFirst()
                        .orElse(null);
                if (tenant != null) {
                    String host = TenantHostNormalizer.normalize(tenant.getHost());
                    if (StringUtils.hasText(host)) {
                        return host;
                    }
                    if (StringUtils.hasText(tenant.getName())) {
                        return tenant.getName();
                    }
                }
            } catch (Exception ignored) {
                // Fail below rather than generating an unstable legacy-id-based name.
            }
            String normalizedTenantReference = TenantHostNormalizer.normalize(tenantId);
            if (StringUtils.hasText(normalizedTenantReference)
                    && (normalizedTenantReference.contains(".") || "localhost".equals(normalizedTenantReference))) {
                return normalizedTenantReference;
            }
        }
        throw new IllegalStateException("Unable to resolve the tenant host or name required for GCP secret naming");
    }

    private String environmentName() {
        String[] activeProfiles = environment.getActiveProfiles();
        String activeProfile = activeProfiles != null && activeProfiles.length > 0 ? activeProfiles[0] : null;
        String value = firstNonBlank(
                environment.getProperty("mawa.environment"),
                activeProfile,
                environment.getProperty("spring.profiles.active"),
                environment.getProperty("MAWA_ENV"),
                "dev"
        );
        return normalisePart(value.split(",")[0]);
    }

    private String normalisePart(String value) {
        String normalised = value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        if (!StringUtils.hasText(normalised)) {
            throw new IllegalArgumentException("Secret name components must contain at least one letter or number");
        }
        return normalised;
    }

    private String firstNonBlank(String... values) {
        if (values != null) {
            for (String value : values) {
                if (StringUtils.hasText(value)) {
                    return value.trim();
                }
            }
        }
        return null;
    }
}
