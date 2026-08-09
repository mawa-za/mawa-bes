package za.co.mawa.bes.configuration.gcp;

import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.secretmanager.v1.AccessSecretVersionRequest;
import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.AddSecretVersionRequest;
import com.google.cloud.secretmanager.v1.CreateSecretRequest;
import com.google.cloud.secretmanager.v1.GetSecretRequest;
import com.google.cloud.secretmanager.v1.Replication;
import com.google.cloud.secretmanager.v1.Secret;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.protobuf.ByteString;
import org.json.JSONObject;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.service.SettingService;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resolves tenant-level integration secrets from Google Secret Manager.
 *
 * Tenant properties/settings should store secret references, not secret values.
 */
@Service
public class GcpTenantSecretService {

    private static final String[] SECRET_REFERENCE_SUFFIXES = new String[]{
            "-GCP-SECRET",
            "-SECRET-NAME",
            "-SECRET-REF",
            "-SECRET"
    };

    private final Environment environment;
    private final SettingService settingService;

    public GcpTenantSecretService(Environment environment, SettingService settingService) {
        this.environment = environment;
        this.settingService = settingService;
    }

    public String resolveTenantProperty(JSONObject properties, String propertyName) {
        if (properties == null || !StringUtils.hasText(propertyName)) {
            return null;
        }

        String secretReference = findTenantPropertySecretReference(properties, propertyName);
        if (StringUtils.hasText(secretReference)) {
            return accessSecret(secretReference);
        }

        String directValue = properties.optString(propertyName, null);
        if (isSecretReference(directValue)) {
            return accessSecret(directValue);
        }

        return blankToNull(directValue);
    }

    public String resolveTenantProperty(JSONObject properties, String propertyName, String defaultValue) {
        String value = resolveTenantProperty(properties, propertyName);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    public String resolveSetting(String attribute, String group) {
        if (!StringUtils.hasText(attribute) || !StringUtils.hasText(group)) {
            return null;
        }

        String secretReference = findSettingSecretReference(attribute, group);
        if (StringUtils.hasText(secretReference)) {
            return accessSecret(secretReference);
        }

        String directValue = settingService.getSetting(attribute, group);
        if (isSecretReference(directValue)) {
            return accessSecret(directValue);
        }

        return blankToNull(directValue);
    }

    public String findTenantPropertySecretReference(JSONObject properties, String propertyName) {
        if (properties == null || !StringUtils.hasText(propertyName)) {
            return null;
        }
        for (String suffix : SECRET_REFERENCE_SUFFIXES) {
            String value = properties.optString(propertyName + suffix, null);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    public String findSettingSecretReference(String attribute, String group) {
        if (!StringUtils.hasText(attribute) || !StringUtils.hasText(group)) {
            return null;
        }
        for (String suffix : SECRET_REFERENCE_SUFFIXES) {
            String value = settingService.getSetting(attribute + suffix, group);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    public boolean hasTenantPropertySecretReference(JSONObject properties, String propertyName) {
        return StringUtils.hasText(findTenantPropertySecretReference(properties, propertyName))
                || isSecretReference(properties == null ? null : properties.optString(propertyName, null));
    }

    public boolean hasSettingSecretReference(String attribute, String group) {
        return StringUtils.hasText(findSettingSecretReference(attribute, group))
                || isSecretReference(settingService.getSetting(attribute, group));
    }

    public void addSecretVersion(String secretReference, String value) {
        if (!StringUtils.hasText(secretReference)) {
            throw new IllegalArgumentException("Google Secret Manager secret reference is required");
        }
        if (value == null) {
            value = "";
        }

        SecretReference reference = SecretReference.from(secretReference, projectId());
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            AddSecretVersionRequest request = AddSecretVersionRequest.newBuilder()
                    .setParent(reference.toSecretName())
                    .setPayload(com.google.cloud.secretmanager.v1.SecretPayload.newBuilder()
                            .setData(ByteString.copyFromUtf8(value))
                            .build())
                    .build();
            client.addSecretVersion(request);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create Google Secret Manager client. Check Cloud Run service account / Application Default Credentials.", ex);
        }
    }

    /**
     * Creates the secret container if it is missing, then appends a new secret version.
     * Use this for self-service integration activation from MAWA settings screens.
     */
    public void createOrAddSecretVersion(String secretReference, String value) {
        SecretReference reference = SecretReference.from(secretReference, projectId());
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            ensureSecretExists(client, reference);
            AddSecretVersionRequest request = AddSecretVersionRequest.newBuilder()
                    .setParent(reference.toSecretName())
                    .setPayload(com.google.cloud.secretmanager.v1.SecretPayload.newBuilder()
                            .setData(ByteString.copyFromUtf8(value == null ? "" : value))
                            .build())
                    .build();
            client.addSecretVersion(request);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create Google Secret Manager client. Check Cloud Run service account / Application Default Credentials.", ex);
        }
    }

    public void createSecretIfMissing(String secretReference) {
        SecretReference reference = SecretReference.from(secretReference, projectId());
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            ensureSecretExists(client, reference);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create Google Secret Manager client. Check Cloud Run service account / Application Default Credentials.", ex);
        }
    }

    /** Resolves an explicit Google Secret Manager reference for integration services. */
    public String accessSecretReference(String secretReference) {
        if (!StringUtils.hasText(secretReference)) {
            return null;
        }
        return accessSecret(secretReference);
    }

    public boolean hasAccessibleSecretVersion(String secretReference) {
        if (!StringUtils.hasText(secretReference)) {
            return false;
        }
        try {
            return accessSecret(secretReference) != null;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private void ensureSecretExists(SecretManagerServiceClient client, SecretReference reference) {
        try {
            client.getSecret(GetSecretRequest.newBuilder().setName(reference.toSecretName()).build());
        } catch (NotFoundException ex) {
            try {
                Secret secret = Secret.newBuilder()
                        .setReplication(Replication.newBuilder()
                                .setAutomatic(Replication.Automatic.newBuilder().build())
                                .build())
                        .build();
                CreateSecretRequest request = CreateSecretRequest.newBuilder()
                        .setParent("projects/" + reference.projectId())
                        .setSecretId(reference.secretName())
                        .setSecret(secret)
                        .build();
                client.createSecret(request);
            } catch (AlreadyExistsException ignored) {
                // Another request created it between getSecret and createSecret.
            }
        }
    }

    public boolean isSecretReference(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.startsWith("gcp-secret://")
                || trimmed.startsWith("sm://")
                || trimmed.startsWith("secret://")
                || trimmed.startsWith("projects/");
    }

    public Map<String, String> resolvedTenantProperties(JSONObject properties, String... propertyNames) {
        Map<String, String> resolved = new LinkedHashMap<>();
        if (propertyNames == null) {
            return resolved;
        }
        for (String propertyName : propertyNames) {
            resolved.put(propertyName, resolveTenantProperty(properties, propertyName));
        }
        return resolved;
    }

    private String accessSecret(String secretReference) {
        SecretReference reference = SecretReference.from(secretReference, projectId());
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            AccessSecretVersionRequest request = AccessSecretVersionRequest.newBuilder()
                    .setName(reference.toVersionName())
                    .build();
            AccessSecretVersionResponse response = client.accessSecretVersion(request);
            return response.getPayload().getData().toStringUtf8();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create Google Secret Manager client. Check Cloud Run service account / Application Default Credentials.", ex);
        }
    }

    private String projectId() {
        String projectId = firstNonBlank(
                environment.getProperty("gcp.secret-manager.project-id"),
                environment.getProperty("gcp.project-id"),
                environment.getProperty("GCP_PROJECT_ID"),
                environment.getProperty("GOOGLE_CLOUD_PROJECT")
        );
        if (!StringUtils.hasText(projectId)) {
            throw new IllegalStateException("Google Secret Manager project id is required. Set GCP_PROJECT_ID or gcp.secret-manager.project-id.");
        }
        return projectId;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        if (!StringUtils.hasText(value) || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return value.trim();
    }

    private record SecretReference(String projectId, String secretName, String version) {

        private static SecretReference from(String rawValue, String defaultProjectId) {
            if (!StringUtils.hasText(rawValue)) {
                throw new IllegalArgumentException("Secret reference is required");
            }

            String value = rawValue.trim();
            value = stripPrefix(value, "gcp-secret://");
            value = stripPrefix(value, "sm://");
            value = stripPrefix(value, "secret://");

            if (value.startsWith("projects/")) {
                String[] parts = value.split("/");
                if (parts.length >= 4 && "projects".equals(parts[0]) && "secrets".equals(parts[2])) {
                    String project = parts[1];
                    String secret = parts[3];
                    String version = parts.length >= 6 && "versions".equals(parts[4]) ? parts[5] : "latest";
                    return new SecretReference(project, secret, version);
                }
                throw new IllegalArgumentException("Invalid Google Secret Manager reference: " + rawValue);
            }

            String secretName = value;
            String version = "latest";
            int versionSeparator = value.lastIndexOf(':');
            if (versionSeparator > 0 && versionSeparator < value.length() - 1) {
                secretName = value.substring(0, versionSeparator).trim();
                version = value.substring(versionSeparator + 1).trim();
            }

            if (!StringUtils.hasText(defaultProjectId)) {
                throw new IllegalStateException("Google Secret Manager project id is required for secret: " + secretName);
            }
            return new SecretReference(defaultProjectId, secretName, version);
        }

        private String toVersionName() {
            return toSecretName() + "/versions/" + version;
        }

        private String toSecretName() {
            return "projects/" + projectId + "/secrets/" + secretName;
        }

        private static String stripPrefix(String value, String prefix) {
            if (value.startsWith(prefix)) {
                return value.substring(prefix.length());
            }
            return value;
        }
    }
}
