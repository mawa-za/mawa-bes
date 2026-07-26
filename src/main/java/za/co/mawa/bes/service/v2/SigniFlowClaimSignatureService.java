package za.co.mawa.bes.service.v2;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import za.co.mawa.bes.configuration.gcp.GcpTenantSecretService;
import za.co.mawa.bes.configuration.gcp.TenantSecretNameService;
import za.co.mawa.bes.entity.AttachmentEntity;
import za.co.mawa.bes.entity.v2.MembershipClaimEntity;
import za.co.mawa.bes.repository.AttachmentRepository;
import za.co.mawa.bes.repository.v2.MembershipClaimRepository;
import za.co.mawa.bes.service.AttachmentService;
import za.co.mawa.bes.service.v2.claim.ClaimFormGenerationService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SigniFlowClaimSignatureService {
    private static final String CONFIG_ID = "DEFAULT";
    private static final String CLAIM_FORM = "CLAIM-FORM";
    private static final String SIGNED_CLAIM_FORM = "CLAIM-FORM-SIGNED";

    private final JdbcTemplate jdbcTemplate;
    private final MembershipClaimRepository claimRepository;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentService attachmentService;
    private final ClaimFormGenerationService claimFormGenerationService;
    private final TenantSecretNameService tenantSecretNameService;
    private final GcpTenantSecretService secretService;

    private final RestTemplate restTemplate = new RestTemplate();

    public SigniFlowClaimSignatureService(
            JdbcTemplate jdbcTemplate,
            MembershipClaimRepository claimRepository,
            AttachmentRepository attachmentRepository,
            AttachmentService attachmentService,
            ClaimFormGenerationService claimFormGenerationService,
            TenantSecretNameService tenantSecretNameService,
            GcpTenantSecretService secretService) {
        this.jdbcTemplate = jdbcTemplate;
        this.claimRepository = claimRepository;
        this.attachmentRepository = attachmentRepository;
        this.attachmentService = attachmentService;
        this.claimFormGenerationService = claimFormGenerationService;
        this.tenantSecretNameService = tenantSecretNameService;
        this.secretService = secretService;
    }

    public Map<String, Object> getConfiguration() {
        ensureConfiguration();
        Map<String, Object> row = new LinkedHashMap<>(jdbcTemplate.queryForMap(
                "SELECT * FROM signiflow_configuration WHERE id = ?", CONFIG_ID));
        String secretName = tenantSecretNameService.currentTenantSecretName("signiflow", "password");
        row.put("password_secret_name", secretName);
        row.put("password_configured", secretService.hasAccessibleSecretVersion(secretName));
        row.remove("created_by");
        row.remove("updated_by");
        return row;
    }

    @Transactional
    public Map<String, Object> saveConfiguration(Map<String, Object> request, String actor) {
        ensureConfiguration();
        boolean enabled = booleanValue(request.get("enabled"));
        String baseUrl = normalizeBaseUrl(text(request.get("baseUrl"), request.get("base_url")));
        String username = text(request.get("username"));
        String password = text(request.get("password"));
        int dueDays = integer(request.get("defaultDueDays"), request.get("default_due_days"), 7, 1, 90);
        boolean workflowEmails = booleanValue(request.containsKey("sendWorkflowEmails")
                ? request.get("sendWorkflowEmails") : request.get("send_workflow_emails"));
        boolean firstEmail = booleanValue(request.containsKey("sendFirstEmail")
                ? request.get("sendFirstEmail") : request.get("send_first_email"));
        String secretName = tenantSecretNameService.currentTenantSecretName("signiflow", "password");

        if (enabled) {
            validateConfiguredBaseUrl(baseUrl);
            if (!StringUtils.hasText(username)) throw new IllegalArgumentException("SigniFlow username is required");
            if (!StringUtils.hasText(password) && !secretService.hasAccessibleSecretVersion(secretName)) {
                throw new IllegalArgumentException("SigniFlow password is required before enabling the integration");
            }
        }
        if (StringUtils.hasText(password)) {
            secretService.createOrAddSecretVersion(secretName, password);
        }

        jdbcTemplate.update("""
                UPDATE signiflow_configuration
                   SET enabled = ?, base_url = ?, username = ?, password_secret_name = ?,
                       default_due_days = ?, send_workflow_emails = ?, send_first_email = ?,
                       updated_by = ?, updated_at = CURRENT_TIMESTAMP
                 WHERE id = ?
                """, enabled, baseUrl, blankToNull(username), secretName, dueDays,
                workflowEmails, firstEmail, actor(actor), CONFIG_ID);
        return getConfiguration();
    }

    public Map<String, Object> testConfiguration() {
        Map<String, Object> config = requireEnabledConfiguration(false);
        Object token = login(config);
        return Map.of(
                "success", true,
                "message", "SigniFlow authentication succeeded",
                "tokenReceived", token != null
        );
    }

    public List<Map<String, Object>> signerOptions(String claimId) {
        claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT DISTINCT p.id AS partnerId,
                       COALESCE(pv.partner_no, p.number, '') AS partnerNumber,
                       COALESCE(NULLIF(pv.partner_name, ''),
                                TRIM(CONCAT_WS(' ', NULLIF(p.name2,''), NULLIF(p.name3,''), NULLIF(p.name1,''))),
                                p.name1, '') AS name,
                       COALESCE((
                           SELECT pc.value FROM partner_contact pc
                            WHERE pc.partner = p.id AND UPPER(pc.type) IN ('EMAIL','EMAIL-ADDRESS')
                              AND (pc.valid_from IS NULL OR pc.valid_from <= CURRENT_DATE)
                              AND (pc.valid_to IS NULL OR pc.valid_to >= CURRENT_DATE)
                            ORDER BY pc.type LIMIT 1
                       ), '') AS email,
                       CASE
                         WHEN p.id = mc.claimant_partner_id THEN 'CLAIMANT'
                         WHEN p.id = mc.deceased_partner_id THEN 'DECEASED'
                         WHEN p.id = m.member_id THEN 'MEMBER'
                         ELSE 'DEPENDENT'
                       END AS relationship
                  FROM membership_claim mc
                  JOIN membership m ON m.id = mc.membership_id
                  JOIN partner p ON p.id IN (mc.claimant_partner_id, mc.deceased_partner_id, m.member_id)
                  LEFT JOIN partner_view pv ON pv.partner_id = p.id
                 WHERE mc.id = ?
                UNION
                SELECT DISTINCT p.id AS partnerId,
                       COALESCE(pv.partner_no, p.number, '') AS partnerNumber,
                       COALESCE(NULLIF(pv.partner_name, ''),
                                TRIM(CONCAT_WS(' ', NULLIF(p.name2,''), NULLIF(p.name3,''), NULLIF(p.name1,''))),
                                p.name1, '') AS name,
                       COALESCE((
                           SELECT pc.value FROM partner_contact pc
                            WHERE pc.partner = p.id AND UPPER(pc.type) IN ('EMAIL','EMAIL-ADDRESS')
                              AND (pc.valid_from IS NULL OR pc.valid_from <= CURRENT_DATE)
                              AND (pc.valid_to IS NULL OR pc.valid_to >= CURRENT_DATE)
                            ORDER BY pc.type LIMIT 1
                       ), '') AS email,
                       'DEPENDENT' AS relationship
                  FROM membership_claim mc
                  JOIN membership_dependent md ON md.membership_id = mc.membership_id
                  JOIN partner p ON p.id = md.dependent_partner_id
                  LEFT JOIN partner_view pv ON pv.partner_id = p.id
                 WHERE mc.id = ?
                   AND COALESCE(md.active, 1) = 1
                   AND UPPER(COALESCE(md.status, 'ACTIVE')) NOT IN ('REMOVED','REPLACED','INACTIVE')
                 ORDER BY relationship, name
                """, claimId, claimId);

        Map<String, Map<String, Object>> byPartner = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String partnerId = text(row.get("partnerId"));
            if (!StringUtils.hasText(partnerId)) continue;
            Map<String, Object> current = byPartner.get(partnerId);
            if (current == null || signerPriority(text(row.get("relationship")))
                    < signerPriority(text(current.get("relationship")))) {
                byPartner.put(partnerId, row);
            }
        }
        return new ArrayList<>(byPartner.values());
    }

    public List<Map<String, Object>> workflows(String claimId) {
        claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));
        return jdbcTemplate.queryForList("""
                SELECT w.*, a.document_type AS source_document_type,
                       sa.document_type AS signed_document_type
                  FROM claim_signiflow_workflow w
                  LEFT JOIN attachment a ON a.id = w.attachment_id
                  LEFT JOIN attachment sa ON sa.id = w.signed_attachment_id
                 WHERE w.claim_id = ?
                 ORDER BY w.created_at DESC
                """, claimId);
    }

    public Map<String, Object> sendClaimForm(String claimId, Map<String, Object> request, String actor) {
        MembershipClaimEntity claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));
        Map<String, Object> config = requireEnabledConfiguration(true);

        AttachmentEntity attachment = resolveClaimForm(claimId, text(request.get("attachmentId")));
        String signerPartnerId = firstNonBlank(text(request.get("signerPartnerId")), claim.getClaimantPartnerId());
        Map<String, Object> signer = resolveSigner(signerPartnerId, text(request.get("signerName")),
                text(request.get("signerEmail")));
        String signerName = required(text(signer.get("name")), "Signer name");
        String signerEmail = required(text(signer.get("email")), "Signer email address").toLowerCase(Locale.ROOT);
        if (!signerEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Signer email address is invalid");
        }

        String workflowId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO claim_signiflow_workflow(
                    id, claim_id, attachment_id, signer_partner_id, signer_name, signer_email,
                    status, created_by, updated_by
                ) VALUES(?, ?, ?, ?, ?, ?, 'SUBMITTING', ?, ?)
                ON DUPLICATE KEY UPDATE signer_partner_id = VALUES(signer_partner_id),
                    signer_name = VALUES(signer_name), signer_email = VALUES(signer_email),
                    status = 'SUBMITTING', result_message = NULL, updated_by = VALUES(updated_by),
                    updated_at = CURRENT_TIMESTAMP
                """, workflowId, claimId, attachment.getId(), blankToNull(signerPartnerId), signerName,
                signerEmail, actor(actor), actor(actor));
        Map<String, Object> existing = jdbcTemplate.queryForMap(
                "SELECT * FROM claim_signiflow_workflow WHERE claim_id = ? AND attachment_id = ?",
                claimId, attachment.getId());
        workflowId = text(existing.get("id"));

        try {
            Object token = login(config);
            Map<String, Object> payload = buildWorkflowPayload(config, claim, attachment, signerName, signerEmail, token);
            Map<String, Object> response = post(config, "/FullWorkflow", payload);
            Long documentId = longValue(first(response, "docIDField", "DocIDField", "documentId", "DocID"));
            Long portfolioId = longValue(first(response, "portfolioIDField", "PortfolioIDField", "portfolioId", "PortfolioID"));
            String status = firstNonBlank(text(first(response, "statusField", "StatusField", "status")), "SUBMITTED");
            String result = responseMessage(response);
            if (documentId == null && isFailureResult(response)) {
                throw new IllegalStateException(firstNonBlank(result, "SigniFlow rejected the claim form workflow"));
            }
            jdbcTemplate.update("""
                    UPDATE claim_signiflow_workflow
                       SET signiflow_document_id = ?, signiflow_portfolio_id = ?, status = ?,
                           result_message = ?, submitted_at = CURRENT_TIMESTAMP,
                           updated_at = CURRENT_TIMESTAMP, updated_by = ?
                     WHERE id = ?
                    """, documentId, portfolioId, normalizeStatus(status), truncate(result), actor(actor), workflowId);
            return workflow(workflowId);
        } catch (RuntimeException ex) {
            jdbcTemplate.update("""
                    UPDATE claim_signiflow_workflow
                       SET status = 'FAILED', result_message = ?, updated_at = CURRENT_TIMESTAMP, updated_by = ?
                     WHERE id = ?
                    """, truncate(rootMessage(ex)), actor(actor), workflowId);
            throw ex;
        }
    }

    public Map<String, Object> refreshWorkflow(String workflowId, String actor) {
        Map<String, Object> workflow = workflow(workflowId);
        Map<String, Object> config = requireEnabledConfiguration(true);
        Long documentId = longValue(workflow.get("signiflow_document_id"));
        if (documentId == null) throw new IllegalStateException("The workflow has no SigniFlow document ID");
        Object token = login(config);
        Map<String, Object> response = post(config, "/GetDocStatus", Map.of(
                "docIDField", documentId,
                "tokenField", token
        ));
        String status = firstNonBlank(text(first(response, "statusField", "StatusField", "status", "resultField")), "UNKNOWN");
        String normalized = normalizeStatus(status);
        jdbcTemplate.update("""
                UPDATE claim_signiflow_workflow
                   SET status = ?, result_message = ?,
                       completed_at = CASE WHEN ? IN ('COMPLETED','SIGNED','DOCUMENT_SIGNED') THEN CURRENT_TIMESTAMP ELSE completed_at END,
                       updated_at = CURRENT_TIMESTAMP, updated_by = ?
                 WHERE id = ?
                """, normalized, truncate(responseMessage(response)), normalized, actor(actor), workflowId);
        return workflow(workflowId);
    }

    public Map<String, Object> downloadSignedDocument(String workflowId, String actor) {
        Map<String, Object> workflow = refreshWorkflow(workflowId, actor);
        String existingAttachmentId = text(workflow.get("signed_attachment_id"));
        if (StringUtils.hasText(existingAttachmentId)) return workflow;

        Map<String, Object> config = requireEnabledConfiguration(true);
        Long documentId = longValue(workflow.get("signiflow_document_id"));
        Object token = login(config);
        Map<String, Object> response = post(config, "/GetDoc", Map.of(
                "docIDField", documentId,
                "tokenField", token
        ));
        String base64 = text(first(response, "docField", "DocField", "documentField", "document"));
        if (!StringUtils.hasText(base64)) {
            throw new IllegalStateException(firstNonBlank(responseMessage(response),
                    "SigniFlow has not returned a signed document yet"));
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(stripDataUrl(base64));
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("SigniFlow returned an invalid signed document", ex);
        }
        AttachmentEntity signed = attachmentService.saveBytes(bytes, "pdf", "claims",
                text(workflow.get("claim_id")), SIGNED_CLAIM_FORM);
        jdbcTemplate.update("""
                UPDATE claim_signiflow_workflow
                   SET signed_attachment_id = ?, status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP,
                       updated_at = CURRENT_TIMESTAMP, updated_by = ?
                 WHERE id = ?
                """, signed.getId(), actor(actor), workflowId);
        return workflow(workflowId);
    }

    private AttachmentEntity resolveClaimForm(String claimId, String attachmentId) {
        if (StringUtils.hasText(attachmentId)) {
            AttachmentEntity attachment = attachmentRepository.findById(attachmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Claim form attachment not found"));
            if (!claimId.equals(attachment.getObjectId())) {
                throw new IllegalArgumentException("The selected attachment does not belong to this claim");
            }
            if (!CLAIM_FORM.equalsIgnoreCase(attachment.getDocumentType())) {
                throw new IllegalArgumentException("Only a generated claim form can be sent for signature");
            }
            return attachment;
        }
        return claimFormGenerationService.generateForSubmittedClaim(claimId);
    }

    private Map<String, Object> resolveSigner(String partnerId, String submittedName, String submittedEmail) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("partnerId", blankToNull(partnerId));
        result.put("name", blankToNull(submittedName));
        result.put("email", blankToNull(submittedEmail));
        if (!StringUtils.hasText(partnerId)) return result;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT p.id AS partnerId,
                       COALESCE(NULLIF(pv.partner_name, ''),
                                TRIM(CONCAT_WS(' ', NULLIF(p.name2,''), NULLIF(p.name3,''), NULLIF(p.name1,''))),
                                p.name1, '') AS name,
                       COALESCE((
                           SELECT pc.value FROM partner_contact pc
                            WHERE pc.partner = p.id AND UPPER(pc.type) IN ('EMAIL','EMAIL-ADDRESS')
                              AND (pc.valid_from IS NULL OR pc.valid_from <= CURRENT_DATE)
                              AND (pc.valid_to IS NULL OR pc.valid_to >= CURRENT_DATE)
                            ORDER BY pc.type LIMIT 1
                       ), '') AS email
                  FROM partner p
                  LEFT JOIN partner_view pv ON pv.partner_id = p.id
                 WHERE p.id = ?
                """, partnerId);
        if (rows.isEmpty()) throw new IllegalArgumentException("Signer partner not found");
        Map<String, Object> row = rows.get(0);
        if (!StringUtils.hasText(text(result.get("name")))) result.put("name", row.get("name"));
        if (!StringUtils.hasText(text(result.get("email")))) result.put("email", row.get("email"));
        return result;
    }

    private Map<String, Object> buildWorkflowPayload(Map<String, Object> config,
                                                      MembershipClaimEntity claim,
                                                      AttachmentEntity attachment,
                                                      String signerName,
                                                      String signerEmail,
                                                      Object token) {
        byte[] bytes = attachmentService.getBytes(attachment);
        int dueDays = integer(config.get("default_due_days"), null, 7, 1, 90);
        Map<String, Object> workflowUser = new LinkedHashMap<>();
        workflowUser.put("actionField", "Sign");
        workflowUser.put("allowProxyField", false);
        workflowUser.put("autoSignField", false);
        workflowUser.put("emailAddressField", signerEmail);
        workflowUser.put("languageCodeField", "en");
        workflowUser.put("signReasonField", "Sign MAWA claim form " + claim.getClaimNo());
        workflowUser.put("userFirstNameField", firstName(signerName));
        workflowUser.put("userLastNameField", lastName(signerName));
        workflowUser.put("userFullNameField", signerName);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tokenField", token);
        payload.put("docField", Base64.getEncoder().encodeToString(bytes));
        payload.put("docNameField", "MAWA Claim Form - " + firstNonBlank(claim.getClaimNo(), claim.getId()));
        payload.put("extensionField", "pdf");
        payload.put("dueDateField", LocalDate.now().plusDays(dueDays).toString());
        payload.put("customMessageField", "Please review and electronically sign the attached MAWA claim form.");
        payload.put("sendWorkflowEmailsField", booleanValue(config.get("send_workflow_emails")));
        payload.put("sendFirstEmailField", booleanValue(config.get("send_first_email")));
        payload.put("autoRemindField", true);
        payload.put("flattenDocumentField", false);
        payload.put("workflowUsersListField", List.of(workflowUser));
        return payload;
    }

    private Object login(Map<String, Object> config) {
        String username = required(text(config.get("username")), "SigniFlow username");
        String secretName = firstNonBlank(text(config.get("password_secret_name")),
                tenantSecretNameService.currentTenantSecretName("signiflow", "password"));
        String password = secretService.hasAccessibleSecretVersion(secretName)
                ? secretService.accessSecretReference(secretName)
                : null;
        if (!StringUtils.hasText(password)) throw new IllegalStateException("SigniFlow password is not configured");
        Map<String, Object> response = post(config, "/Login", Map.of(
                "userNameField", username,
                "passwordField", password
        ));
        Object token = first(response, "tokenField", "TokenField", "token");
        if (token == null || isFailureResult(response)) {
            throw new IllegalStateException(firstNonBlank(responseMessage(response), "SigniFlow authentication failed"));
        }
        return token;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(Map<String, Object> config, String endpoint, Map<String, Object> payload) {
        String url = normalizeBaseUrl(text(config.get("base_url"))) + endpoint;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url,
                    new HttpEntity<>(payload, headers), Map.class);
            Map<?, ?> body = response.getBody();
            if (body == null) throw new IllegalStateException("SigniFlow returned an empty response");
            Map<String, Object> result = new LinkedHashMap<>();
            body.forEach((key, value) -> result.put(String.valueOf(key), value));
            return result;
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException("SigniFlow request failed (" + ex.getStatusCode().value() + "): "
                    + truncate(ex.getResponseBodyAsString()), ex);
        } catch (RuntimeException ex) {
            if (ex instanceof IllegalStateException) throw ex;
            throw new IllegalStateException("Unable to call SigniFlow: " + rootMessage(ex), ex);
        }
    }

    private Map<String, Object> requireEnabledConfiguration(boolean requireEnabled) {
        Map<String, Object> config = getConfiguration();
        if (requireEnabled && !booleanValue(config.get("enabled"))) {
            throw new IllegalStateException("SigniFlow integration is not enabled");
        }
        try {
            validateConfiguredBaseUrl(text(config.get("base_url")));
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
        return config;
    }

    private void ensureConfiguration() {
        jdbcTemplate.update("""
                INSERT INTO signiflow_configuration(id, enabled)
                SELECT ?, 0 WHERE NOT EXISTS (SELECT 1 FROM signiflow_configuration WHERE id = ?)
                """, CONFIG_ID, CONFIG_ID);
    }

    private Map<String, Object> workflow(String workflowId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM claim_signiflow_workflow WHERE id = ?", workflowId);
        if (rows.isEmpty()) throw new IllegalArgumentException("SigniFlow workflow not found: " + workflowId);
        return rows.get(0);
    }

    private String responseMessage(Map<String, Object> response) {
        return firstNonBlank(text(first(response, "resultField", "ResultField", "message", "Message")),
                text(first(response, "statusField", "StatusField", "status")));
    }

    private boolean isFailureResult(Map<String, Object> response) {
        String result = responseMessage(response);
        if (!StringUtils.hasText(result)) return false;
        String value = result.toUpperCase(Locale.ROOT);
        return value.contains("FAIL") || value.contains("ERROR") || value.contains("INVALID");
    }

    private Object first(Map<String, Object> values, String... keys) {
        for (String key : keys) if (values.containsKey(key)) return values.get(key);
        return null;
    }

    private String normalizeBaseUrl(String value) {
        String url = blankToNull(value);
        if (url == null) return null;
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        return url;
    }

    private void validateConfiguredBaseUrl(String value) {
        String url = normalizeBaseUrl(value);
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException("SigniFlow service URL is required");
        }
        String lower = url.toLowerCase(Locale.ROOT);
        if ((!lower.startsWith("https://") && !lower.startsWith("http://"))
                || lower.contains("server-url")) {
            throw new IllegalArgumentException("Enter the actual SigniFlow HTTPS service URL before enabling the integration");
        }
    }


    private int signerPriority(String relationship) {
        return switch (firstNonBlank(relationship, "DEPENDENT").toUpperCase(Locale.ROOT)) {
            case "CLAIMANT" -> 0;
            case "DECEASED" -> 1;
            case "MEMBER" -> 2;
            default -> 3;
        };
    }

    private String normalizeStatus(String value) {
        String status = firstNonBlank(value, "UNKNOWN").trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_').replace('-', '_');
        return status.length() > 50 ? status.substring(0, 50) : status;
    }

    private String firstName(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        return parts.length == 0 ? fullName : parts[0];
    }

    private String lastName(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        return parts.length < 2 ? "" : parts[parts.length - 1];
    }

    private String stripDataUrl(String value) {
        int comma = value.indexOf(',');
        return value.startsWith("data:") && comma >= 0 ? value.substring(comma + 1) : value;
    }

    private int integer(Object first, Object second, int defaultValue, int minimum, int maximum) {
        Object raw = first != null ? first : second;
        int value;
        try { value = raw == null ? defaultValue : Integer.parseInt(raw.toString()); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Invalid numeric value: " + raw); }
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException("Value must be between " + minimum + " and " + maximum);
        }
        return value;
    }

    private Long longValue(Object value) {
        if (value == null || !StringUtils.hasText(value.toString())) return null;
        if (value instanceof Number number) return number.longValue();
        try { return Long.parseLong(value.toString()); }
        catch (NumberFormatException ignored) { return null; }
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        return value != null && Set.of("true", "1", "yes", "y", "on")
                .contains(value.toString().trim().toLowerCase(Locale.ROOT));
    }

    private String required(String value, String label) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException(label + " is required");
        return value.trim();
    }

    private String text(Object... values) {
        for (Object value : values) if (value != null) return value.toString().trim();
        return null;
    }

    private String blankToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }

    private String firstNonBlank(String... values) {
        for (String value : values) if (StringUtils.hasText(value)) return value.trim();
        return null;
    }

    private String actor(String value) { return firstNonBlank(value, "SYSTEM"); }

    private String truncate(String value) {
        if (value == null) return null;
        return value.length() <= 4000 ? value : value.substring(0, 4000);
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) current = current.getCause();
        return firstNonBlank(current.getMessage(), error.getMessage(), error.getClass().getSimpleName());
    }
}
