package za.co.mawa.bes.configuration.gcp;

import com.google.cloud.secretmanager.v1.AccessSecretVersionRequest;
import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads selected Spring properties from Google Secret Manager very early in the
 * Spring Boot startup lifecycle.
 *
 * This is intentionally implemented as an EnvironmentPostProcessor instead of a
 * normal @Configuration bean because datasource, JWT and mail properties are
 * bound before regular beans are available.
 *
 * Enable with:
 *   GCP_SECRET_ENABLED=true
 *   GCP_PROJECT_ID=your-gcp-project
 *   GCP_SECRET_MAPPINGS=spring.datasource.password=mawa-db-password,jwt.secret=mawa-jwt-secret
 *
 * Mapping format:
 *   spring.property.name=secret-name[:version]
 *
 * Version is optional and defaults to latest.
 */
public class GcpSecretManagerEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Log log = LogFactory.getLog(GcpSecretManagerEnvironmentPostProcessor.class);

    private static final String PROPERTY_SOURCE_NAME = "gcpSecretManager";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }

        boolean enabled = getBoolean(environment,
                "gcp.secret-manager.enabled",
                "gcp.secret.enabled",
                "GCP_SECRET_ENABLED"
        );

        if (!enabled) {
            return;
        }

        String projectId = firstNonBlank(
                environment.getProperty("gcp.secret-manager.project-id"),
                environment.getProperty("gcp.project-id"),
                environment.getProperty("GCP_PROJECT_ID"),
                environment.getProperty("GOOGLE_CLOUD_PROJECT")
        );

        String mappingsText = firstNonBlank(
                environment.getProperty("gcp.secret-manager.mappings"),
                environment.getProperty("gcp.secret.mappings"),
                environment.getProperty("GCP_SECRET_MAPPINGS")
        );

        if (!StringUtils.hasText(projectId)) {
            throw new IllegalStateException("Google Secret Manager is enabled but no project id was provided. Set GCP_PROJECT_ID or gcp.secret-manager.project-id.");
        }

        Map<String, String> mappings = parseMappings(mappingsText);
        if (mappings.isEmpty()) {
            log.warn("Google Secret Manager is enabled but no mappings were provided. Set GCP_SECRET_MAPPINGS or gcp.secret-manager.mappings.");
            return;
        }

        Map<String, Object> secretProperties = new LinkedHashMap<>();

        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            for (Map.Entry<String, String> entry : mappings.entrySet()) {
                String propertyName = entry.getKey();
                SecretReference secretReference = SecretReference.from(entry.getValue());
                String secretValue = accessSecret(client, projectId, secretReference);
                secretProperties.put(propertyName, secretValue);
                log.info("Loaded Spring property '" + propertyName + "' from Google Secret Manager secret '" + secretReference.safeName() + "'.");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create Google Secret Manager client. Check Cloud Run service account / Application Default Credentials.", ex);
        }

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, secretProperties));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private static String accessSecret(SecretManagerServiceClient client, String projectId, SecretReference secretReference) {
        String versionName = secretReference.toVersionName(projectId);
        AccessSecretVersionRequest request = AccessSecretVersionRequest.newBuilder()
                .setName(versionName)
                .build();
        AccessSecretVersionResponse response = client.accessSecretVersion(request);
        return response.getPayload().getData().toStringUtf8();
    }

    private static boolean getBoolean(ConfigurableEnvironment environment, String... keys) {
        for (String key : keys) {
            String value = environment.getProperty(key);
            if (StringUtils.hasText(value)) {
                return Boolean.parseBoolean(value.trim());
            }
        }
        return false;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static Map<String, String> parseMappings(String mappingsText) {
        Map<String, String> mappings = new LinkedHashMap<>();
        if (!StringUtils.hasText(mappingsText)) {
            return mappings;
        }

        String normalized = mappingsText.replace("\r", "\n");
        String[] entries = normalized.split("[,;\\n]");
        for (String rawEntry : entries) {
            String entry = rawEntry.trim();
            if (!StringUtils.hasText(entry) || entry.startsWith("#")) {
                continue;
            }

            int separatorIndex = entry.indexOf('=');
            if (separatorIndex <= 0 || separatorIndex == entry.length() - 1) {
                throw new IllegalArgumentException("Invalid GCP secret mapping '" + entry + "'. Expected spring.property.name=secret-name[:version].");
            }

            String propertyName = entry.substring(0, separatorIndex).trim();
            String secretName = entry.substring(separatorIndex + 1).trim();
            if (!StringUtils.hasText(propertyName) || !StringUtils.hasText(secretName)) {
                throw new IllegalArgumentException("Invalid GCP secret mapping '" + entry + "'. Property name and secret name are required.");
            }
            mappings.put(propertyName, secretName);
        }
        return mappings;
    }

    private record SecretReference(String name, String version) {

        private static SecretReference from(String value) {
            String trimmed = value.trim();

            if (trimmed.startsWith("projects/")) {
                return new SecretReference(trimmed, null);
            }

            int versionSeparator = trimmed.lastIndexOf(':');
            if (versionSeparator > 0 && versionSeparator < trimmed.length() - 1) {
                return new SecretReference(trimmed.substring(0, versionSeparator).trim(), trimmed.substring(versionSeparator + 1).trim());
            }
            return new SecretReference(trimmed, "latest");
        }

        private String toVersionName(String projectId) {
            if (name.startsWith("projects/")) {
                return name;
            }
            return "projects/" + projectId + "/secrets/" + name + "/versions/" + version;
        }

        private String safeName() {
            if (name.startsWith("projects/")) {
                return name;
            }
            return name + ":" + version;
        }
    }
}
