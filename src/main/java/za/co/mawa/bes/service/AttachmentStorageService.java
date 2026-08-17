package za.co.mawa.bes.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobTargetOption;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.TenantContext;

import java.util.Locale;
import java.util.UUID;

@Service
public class AttachmentStorageService {

    private final String bucketName;
    private final String prefix;
    private final String provider;
    private final Storage storage;

    public AttachmentStorageService(
            @Value("${mawa.attachments.storage.bucket:${MAWA_ATTACHMENT_BUCKET:}}") String bucketName,
            @Value("${mawa.attachments.storage.prefix:${MAWA_ATTACHMENT_PREFIX:attachments}}") String prefix,
            @Value("${mawa.attachments.storage.provider:${MAWA_ATTACHMENT_STORAGE_PROVIDER:GCP}}") String provider
    ) {
        this.bucketName = bucketName;
        this.prefix = StringUtils.hasText(prefix) ? sanitisePathPart(prefix) : "attachments";
        this.provider = StringUtils.hasText(provider) ? provider.trim().toUpperCase(Locale.ROOT) : "GCP";
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    public StoredAttachment store(byte[] bytes, String extension, String objectId, String documentType) {
        return store(bytes, extension, null, objectId, documentType, null);
    }

    public StoredAttachment store(byte[] bytes, String extension, String objectType, String objectId, String documentType) {
        return store(bytes, extension, objectType, objectId, documentType, null);
    }

    public StoredAttachment store(byte[] bytes, String extension, String objectType, String objectId, String documentType, String originalFileName) {
        if (!"GCP".equals(provider)) {
            throw new IllegalStateException("Unsupported attachment storage provider: " + provider + ". Configure MAWA_ATTACHMENT_STORAGE_PROVIDER=GCP.");
        }
        if (!StringUtils.hasText(bucketName)) {
            throw new IllegalStateException("Attachment storage bucket is not configured. Set MAWA_ATTACHMENT_BUCKET or mawa.attachments.storage.bucket.");
        }
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Attachment file is empty");
        }

        String normalisedExtension = normaliseExtension(extension);
        String path = buildObjectPath(objectType, objectId, documentType, normalisedExtension, originalFileName);
        String contentType = contentTypeFor(normalisedExtension);

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, path))
                .setContentType(contentType)
                .build();
        storage.create(blobInfo, bytes, BlobTargetOption.doesNotExist());
        return new StoredAttachment(provider, bucketName, path, contentType, (long) bytes.length);
    }

    /**
     * Stores a migrated legacy attachment at a deterministic object path. If a
     * previous attempt uploaded the object but failed before the database row
     * was committed, the existing object is safely reused on retry.
     */
    public StoredAttachment storeLegacyAttachment(
            byte[] bytes,
            String extension,
            String objectType,
            String objectId,
            String documentType,
            String attachmentId
    ) {
        if (!"GCP".equals(provider)) {
            throw new IllegalStateException(
                    "Unsupported attachment storage provider: " + provider
                            + ". Configure MAWA_ATTACHMENT_STORAGE_PROVIDER=GCP."
            );
        }
        if (!StringUtils.hasText(bucketName)) {
            throw new IllegalStateException(
                    "Attachment storage bucket is not configured. Set MAWA_ATTACHMENT_BUCKET or mawa.attachments.storage.bucket."
            );
        }
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Attachment file is empty");
        }
        if (!StringUtils.hasText(attachmentId)) {
            throw new IllegalArgumentException("Attachment id is required for legacy migration");
        }

        String normalisedExtension = normaliseExtension(extension);
        String path = buildLegacyObjectPath(
                objectType,
                objectId,
                documentType,
                normalisedExtension,
                attachmentId
        );
        String contentType = contentTypeFor(normalisedExtension);
        BlobId blobId = BlobId.of(bucketName, path);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        try {
            storage.create(blobInfo, bytes, BlobTargetOption.doesNotExist());
        } catch (StorageException ex) {
            if (ex.getCode() != 412 && ex.getCode() != 409) {
                throw ex;
            }
            Blob existing = storage.get(blobId);
            if (existing == null || !existing.exists()) {
                throw ex;
            }
            Long existingSize = existing.getSize();
            if (existingSize != null && existingSize != bytes.length) {
                throw new IllegalStateException(
                        "Existing GCS object has an unexpected size for attachment "
                                + attachmentId + ": expected " + bytes.length
                                + " but found " + existingSize
                );
            }
        }

        return new StoredAttachment(
                provider,
                bucketName,
                path,
                contentType,
                (long) bytes.length
        );
    }

    public byte[] read(String bucket, String path) {
        String resolvedBucket = StringUtils.hasText(bucket) ? bucket : bucketName;
        if (!StringUtils.hasText(resolvedBucket) || !StringUtils.hasText(path)) {
            throw new IllegalArgumentException("Attachment bucket/path is required");
        }
        Blob blob = storage.get(BlobId.of(resolvedBucket, path));
        if (blob == null || !blob.exists()) {
            throw new IllegalStateException("Attachment file was not found in Google Cloud Storage: " + path);
        }
        return blob.getContent();
    }

    public void delete(String bucket, String path) {
        String resolvedBucket = StringUtils.hasText(bucket) ? bucket : bucketName;
        if (StringUtils.hasText(resolvedBucket) && StringUtils.hasText(path)) {
            storage.delete(BlobId.of(resolvedBucket, path));
        }
    }

    public boolean isGcpConfigured() {
        return "GCP".equals(provider) && StringUtils.hasText(bucketName);
    }

    public String defaultBucket() {
        return bucketName;
    }

    private String buildLegacyObjectPath(
            String objectType,
            String objectId,
            String documentType,
            String extension,
            String attachmentId
    ) {
        String safeTenant = resolveTenantPathScope();
        String safeModule = sanitisePathPart(resolveModuleOrType(objectType, documentType));
        String safeObjectId = sanitisePathPart(StringUtils.hasText(objectId) ? objectId : "unlinked");
        String safeAttachmentId = sanitisePathPart(attachmentId);
        String fileName = "legacy-" + safeAttachmentId
                + (StringUtils.hasText(extension) ? "." + extension : "");
        return prefix + "/" + safeTenant + "/" + safeModule + "/" + safeObjectId + "/" + fileName;
    }

    private String buildObjectPath(String objectType, String objectId, String documentType, String extension, String originalFileName) {
        String safeTenant = resolveTenantPathScope();
        String safeModule = sanitisePathPart(resolveModuleOrType(objectType, documentType));
        String safeObjectId = sanitisePathPart(StringUtils.hasText(objectId) ? objectId : "unlinked");
        String safeFileName = buildSafeFileName(originalFileName, documentType, extension);
        return prefix + "/" + safeTenant + "/" + safeModule + "/" + safeObjectId + "/" + safeFileName;
    }

    String resolveTenantPathScope() {
        String tenantId = TenantContext.getCurrentTenant();
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalStateException("Tenant id is not available for attachment storage");
        }
        return sanitisePathPart(tenantId);
    }

    private String resolveModuleOrType(String objectType, String documentType) {
        if (StringUtils.hasText(objectType)) {
            return objectType;
        }
        if (StringUtils.hasText(documentType)) {
            return documentType;
        }
        return "attachments";
    }

    private String buildSafeFileName(String originalFileName, String documentType, String extension) {
        String baseName = StringUtils.hasText(originalFileName) ? originalFileName : documentType;
        String safeBaseName = sanitisePathPart(StringUtils.hasText(baseName) ? baseName : "attachment");
        String uniquePrefix = UUID.randomUUID().toString();
        if (!StringUtils.hasText(extension)) {
            return uniquePrefix + "-" + safeBaseName;
        }
        String extensionWithDot = "." + extension;
        if (safeBaseName.endsWith(extensionWithDot)) {
            return uniquePrefix + "-" + safeBaseName;
        }
        return uniquePrefix + "-" + safeBaseName + extensionWithDot;
    }

    private String sanitisePathPart(String value) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }
        String sanitised = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "-");
        sanitised = sanitised.replaceAll("^-+", "").replaceAll("-+$", "");
        return StringUtils.hasText(sanitised) ? sanitised : "unknown";
    }

    private String normaliseExtension(String extension) {
        if (!StringUtils.hasText(extension)) {
            return "bin";
        }
        String cleaned = extension.trim().toLowerCase(Locale.ROOT);
        if (cleaned.startsWith(".")) {
            cleaned = cleaned.substring(1);
        }
        return cleaned.replaceAll("[^a-z0-9]+", "");
    }

    private String contentTypeFor(String extension) {
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "csv" -> "text/csv";
            case "txt" -> "text/plain";
            case "xml" -> "application/xml";
            case "json" -> "application/json";
            default -> "application/octet-stream";
        };
    }

    public record StoredAttachment(String storageProvider, String bucket, String path, String contentType, Long fileSize) {
    }
}
