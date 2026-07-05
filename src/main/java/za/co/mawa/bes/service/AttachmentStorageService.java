package za.co.mawa.bes.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.TenantContext;

import java.time.Instant;
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
        String path = buildObjectPath(objectId, documentType, normalisedExtension);
        String contentType = contentTypeFor(normalisedExtension);

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, path))
                .setContentType(contentType)
                .build();
        storage.create(blobInfo, bytes);
        return new StoredAttachment(provider, bucketName, path, contentType, (long) bytes.length);
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

    private String buildObjectPath(String objectId, String documentType, String extension) {
        String tenant = StringUtils.hasText(TenantContext.getCurrentTenantURL())
                ? TenantContext.getCurrentTenantURL()
                : TenantContext.getCurrentTenant();
        String safeTenant = sanitisePathPart(StringUtils.hasText(tenant) ? tenant : "unknown-tenant");
        String safeObjectId = sanitisePathPart(StringUtils.hasText(objectId) ? objectId : "unlinked");
        String safeDocumentType = sanitisePathPart(StringUtils.hasText(documentType) ? documentType : "document");
        String datePart = Instant.now().toString().substring(0, 10);
        String fileName = UUID.randomUUID() + (StringUtils.hasText(extension) ? "." + extension : "");
        return prefix + "/" + safeTenant + "/" + safeObjectId + "/" + datePart + "/" + safeDocumentType + "/" + fileName;
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
