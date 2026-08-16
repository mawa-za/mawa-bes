package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.service.AttachmentStorageService;

import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Performs the source-tenant side of an externally-created funeral claim
 * submission. This bean must be invoked through {@link CrossTenantExecutionService}
 * so its transaction is opened against the claim-owning tenant schema.
 */
@Service
@RequiredArgsConstructor
public class ExternalFuneralClaimApprovalService {

    private static final String GENERATED_CLAIM_FORM = "CLAIM-FORM";

    private final JdbcTemplate jdbcTemplate;
    private final ApprovalService approvalService;
    private final AttachmentStorageService attachmentStorageService;

    /**
     * Submits all claims in one source-tenant transaction. If any source claim
     * fails validation or approval submission, every source-tenant database
     * change in this batch is rolled back.
     */
    @Transactional
    public List<String> submitBatch(
            String providerTenantId,
            List<ExternalClaimSubmission> submissions
    ) {
        String sourceTenantId = requireTenantId(TenantContext.getCurrentTenant(), "Source tenant");
        String providerTenant = requireTenantId(providerTenantId, "Funeral provider tenant");
        if (submissions == null || submissions.isEmpty()) {
            return List.of();
        }

        // Preflight and copy every document before creating any approval request.
        // This keeps multiple external covers all-or-nothing in the source tenant.
        for (ExternalClaimSubmission submission : submissions) {
            ApprovalSubmitRequest request = requireRequest(submission);
            validateSourceClaim(request);
            validateWorkflow(request.getApprovalType());
            copyProviderAttachments(
                    providerTenant,
                    sourceTenantId,
                    request.getReferenceId(),
                    submission.providerAttachmentObjectIds());
            requireSupportingDocument(request.getReferenceId());
        }

        List<String> submittedClaimIds = new ArrayList<>();
        for (ExternalClaimSubmission submission : submissions) {
            ApprovalSubmitRequest request = submission.approvalRequest();
            approvalService.submitForApproval(request);
            submittedClaimIds.add(request.getReferenceId());
        }
        return submittedClaimIds;
    }

    private ApprovalSubmitRequest requireRequest(ExternalClaimSubmission submission) {
        if (submission == null || submission.approvalRequest() == null) {
            throw new IllegalArgumentException("External claim approval request is required");
        }
        ApprovalSubmitRequest request = submission.approvalRequest();
        if (!StringUtils.hasText(request.getReferenceId())) {
            throw new IllegalArgumentException("External claim reference is required");
        }
        if (request.getApprovalType() == null || !request.getApprovalType().isMembershipClaimApproval()) {
            throw new IllegalArgumentException("External funeral claim approval type is invalid");
        }
        return request;
    }

    private void validateSourceClaim(ApprovalSubmitRequest request) {
        List<Map<String, Object>> claims = jdbcTemplate.queryForList(
                "SELECT id, claim_type, status FROM membership_claim WHERE id = ?",
                request.getReferenceId());
        if (claims.isEmpty()) {
            throw new IllegalArgumentException("External claim was not found in the source tenant: " + request.getReferenceId());
        }

        Map<String, Object> claim = claims.get(0);
        String status = Objects.toString(claim.get("status"), "");
        if (!"DRAFT".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException(
                    "Only DRAFT claims can be submitted for approval. Current status: " + status);
        }

        String claimType = Objects.toString(claim.get("claim_type"), "").toUpperCase(Locale.ROOT);
        String expectedApprovalType = "CLAIM_" + claimType;
        if (!request.getApprovalType().name().equals(expectedApprovalType)
                && request.getApprovalType() != ApprovalType.CLAIM) {
            throw new IllegalArgumentException(
                    "External claim approval type does not match the source claim type: " + claimType);
        }

        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM approval_request WHERE reference_id = ? "
                        + "AND approval_type IN ('CLAIM','CLAIM_CASH','CLAIM_TOMBSTONE','CLAIM_FUNERAL','CLAIM_COMBINATION','CLAIM_GROCERY')",
                Integer.class,
                request.getReferenceId());
        if (existing != null && existing > 0) {
            throw new IllegalStateException(
                    "Approval request already exists for reference: " + request.getReferenceId());
        }
    }

    private void validateWorkflow(ApprovalType approvalType) {
        List<Map<String, Object>> workflowRows = jdbcTemplate.queryForList(
                "SELECT id, active, auto_approve FROM approval_workflow WHERE approval_type = ? LIMIT 1",
                approvalType.name());
        if (workflowRows.isEmpty() && approvalType != ApprovalType.CLAIM) {
            workflowRows = jdbcTemplate.queryForList(
                    "SELECT id, active, auto_approve FROM approval_workflow WHERE approval_type = 'CLAIM' LIMIT 1");
        }
        if (workflowRows.isEmpty()) {
            throw new IllegalStateException("No approval workflow configured for type: " + approvalType);
        }

        Map<String, Object> workflow = workflowRows.get(0);
        boolean active = booleanValue(workflow.get("active"), true);
        boolean autoApprove = booleanValue(workflow.get("auto_approve"), false);
        if (!active || autoApprove) {
            return;
        }

        Integer activeSteps = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM approval_workflow_step WHERE workflow_id = ? AND active = 1",
                Integer.class,
                Objects.toString(workflow.get("id"), null));
        if (activeSteps == null || activeSteps == 0) {
            throw new IllegalStateException("Approval workflow has no active steps");
        }
    }

    private void copyProviderAttachments(
            String providerTenantId,
            String sourceTenantId,
            String sourceClaimId,
            List<String> providerObjectIds
    ) {
        if (providerObjectIds == null || providerObjectIds.isEmpty()) {
            return;
        }

        Set<String> copiedProviderAttachmentIds = new LinkedHashSet<>();
        for (String objectId : providerObjectIds) {
            if (!StringUtils.hasText(objectId)) continue;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, object_id, document_type, upload_by, upload_time, upload_date, "
                            + "file, file_path, storage_bucket, storage_provider, content_type, file_size, extension "
                            + "FROM " + qualifiedTable(providerTenantId, "attachment") + " WHERE object_id = ?",
                    objectId.trim());
            for (Map<String, Object> row : rows) {
                String providerAttachmentId = Objects.toString(row.get("id"), null);
                if (!StringUtils.hasText(providerAttachmentId)
                        || !copiedProviderAttachmentIds.add(providerAttachmentId)) {
                    continue;
                }
                copyProviderAttachment(
                        providerTenantId,
                        sourceTenantId,
                        sourceClaimId,
                        providerAttachmentId,
                        row);
            }
        }
    }

    private void copyProviderAttachment(
            String providerTenantId,
            String sourceTenantId,
            String sourceClaimId,
            String providerAttachmentId,
            Map<String, Object> row
    ) {
        String sourceAttachmentId = externalCopyId(sourceTenantId, providerTenantId, providerAttachmentId, sourceClaimId);
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attachment WHERE id = ?",
                Integer.class,
                sourceAttachmentId);
        if (existing != null && existing > 0) {
            return;
        }

        byte[] bytes = readProviderAttachmentBytes(row);
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("External claim attachment is empty: " + providerAttachmentId);
        }

        String documentType = Objects.toString(row.get("document_type"), "SUPPORTING-DOCUMENT");
        String extension = Objects.toString(row.get("extension"), "bin");
        AttachmentStorageService.StoredAttachment stored = attachmentStorageService.storeLegacyAttachment(
                bytes,
                extension,
                "claims",
                sourceClaimId,
                documentType,
                sourceAttachmentId);

        String uploadBy = Objects.toString(row.get("upload_by"), null);
        jdbcTemplate.update(
                "INSERT INTO attachment "
                        + "(id, object_id, document_type, upload_by, upload_time, upload_date, file, "
                        + "file_path, storage_bucket, storage_provider, content_type, file_size, extension) "
                        + "VALUES (?, ?, ?, ?, COALESCE(?, CURRENT_TIMESTAMP), COALESCE(?, CURRENT_TIMESTAMP), NULL, ?, ?, ?, ?, ?, ?)",
                sourceAttachmentId,
                sourceClaimId,
                documentType,
                StringUtils.hasText(uploadBy) ? uploadBy : "EXTERNAL@" + providerTenantId,
                row.get("upload_time"),
                row.get("upload_date"),
                stored.path(),
                stored.bucket(),
                stored.storageProvider(),
                stored.contentType(),
                stored.fileSize(),
                extension);
    }

    private byte[] readProviderAttachmentBytes(Map<String, Object> row) {
        String filePath = Objects.toString(row.get("file_path"), null);
        if (StringUtils.hasText(filePath)) {
            return attachmentStorageService.read(
                    Objects.toString(row.get("storage_bucket"), null),
                    filePath);
        }

        Object file = row.get("file");
        if (file instanceof byte[] bytes) {
            return bytes;
        }
        if (file instanceof Blob blob) {
            try {
                return blob.getBytes(1, Math.toIntExact(blob.length()));
            } catch (SQLException exception) {
                throw new IllegalStateException("Unable to read legacy external claim attachment", exception);
            }
        }
        return null;
    }

    private void requireSupportingDocument(String claimId) {
        Integer supportingDocuments = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attachment WHERE object_id = ? "
                        + "AND UPPER(COALESCE(document_type, '')) <> ?",
                Integer.class,
                claimId,
                GENERATED_CLAIM_FORM);
        if (supportingDocuments == null || supportingDocuments == 0) {
            throw new IllegalArgumentException(
                    "Attach at least one supporting document in Claim Documentation before submitting the funeral claims for approval");
        }
    }

    private String externalCopyId(
            String sourceTenantId,
            String providerTenantId,
            String providerAttachmentId,
            String sourceClaimId
    ) {
        UUID deterministic = UUID.nameUUIDFromBytes(
                (sourceTenantId + ":" + providerTenantId + ":" + providerAttachmentId + ":" + sourceClaimId)
                        .getBytes(StandardCharsets.UTF_8));
        return "EXT-" + deterministic;
    }

    private boolean booleanValue(Object value, boolean fallback) {
        if (value == null) return fallback;
        if (value instanceof Boolean bool) return bool;
        if (value instanceof Number number) return number.intValue() != 0;
        String text = value.toString().trim();
        if (text.isEmpty()) return fallback;
        return "1".equals(text) || Boolean.parseBoolean(text);
    }

    private String requireTenantId(String tenantId, String label) {
        if (!StringUtils.hasText(tenantId) || !tenantId.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new IllegalArgumentException(label + " is invalid");
        }
        return tenantId.trim();
    }

    private String qualifiedTable(String tenantId, String tableName) {
        String tenant = requireTenantId(tenantId, "Tenant");
        if (!StringUtils.hasText(tableName) || !tableName.matches("[A-Za-z0-9_]{1,128}")) {
            throw new IllegalArgumentException("Invalid table name");
        }
        return "`" + tenant + "`.`" + tableName + "`";
    }

    public record ExternalClaimSubmission(
            ApprovalSubmitRequest approvalRequest,
            List<String> providerAttachmentObjectIds
    ) {
        public ExternalClaimSubmission {
            providerAttachmentObjectIds = providerAttachmentObjectIds == null
                    ? List.of()
                    : List.copyOf(providerAttachmentObjectIds);
        }
    }
}
