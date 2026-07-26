package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.dto.attachment.AttachmentCreateDto;
import za.co.mawa.bes.entity.AttachmentEntity;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.service.AttachmentService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyFormDocumentService {
    private final JdbcTemplate jdbcTemplate;
    private final AttachmentService attachmentService;
    private final ReferenceDataValidationService referenceDataValidationService;

    public List<Map<String, Object>> list(boolean activeOnly) {
        return jdbcTemplate.queryForList("""
            SELECT id,form_code,title,description,category,version_no,attachment_id,file_name,extension,active,uploaded_at,uploaded_by
              FROM company_form_document
            """ + (activeOnly ? " WHERE active=1 " : " ") + "ORDER BY category,title");
    }

    public Map<String, Object> get(String id) {
        return jdbcTemplate.queryForMap("SELECT * FROM company_form_document WHERE id=?", id);
    }

    public boolean isActive(String id) {
        Object active = get(id).get("active");
        if (active instanceof Boolean value) return value;
        if (active instanceof Number value) return value.intValue() != 0;
        return "1".equals(String.valueOf(active)) || Boolean.parseBoolean(String.valueOf(active));
    }

    @Transactional
    public Map<String, Object> upload(Map<String, Object> request, String userId) throws Exception {
        String code = required(request, "formCode").toUpperCase();
        String title = required(request, "title");
        String category = referenceDataValidationService.requireOption(
                "COMPANY-FORM-CATEGORY", required(request, "category"), "Category");
        String base64 = required(request, "file");
        String extension = required(request, "extension").replace(".", "").toLowerCase();
        String fileName = Objects.toString(request.get("fileName"), code + "." + extension);

        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT * FROM company_form_document WHERE form_code=?", code);
        String id = existing.isEmpty() ? UUID.randomUUID().toString() : Objects.toString(existing.get(0).get("id"));
        String oldAttachmentId = existing.isEmpty() ? null : Objects.toString(existing.get(0).get("attachment_id"), null);
        int version = existing.isEmpty() ? 1 : ((Number) existing.get(0).get("version_no")).intValue() + 1;

        AttachmentCreateDto attachment = new AttachmentCreateDto();
        attachment.setFile(base64);
        attachment.setExtension(extension);
        attachment.setDocumentType("COMPANY-FORM");
        attachment.setObjectType("COMPANY-FORM");
        attachment.setObjectId(id);
        AttachmentEntity stored = attachmentService.saveAndReturn(attachment);

        jdbcTemplate.update("""
            INSERT INTO company_form_document(id,form_code,title,description,category,version_no,attachment_id,file_name,extension,active,uploaded_by)
            VALUES(?,?,?,?,?,?,?,?,?,1,?)
            ON DUPLICATE KEY UPDATE title=VALUES(title),description=VALUES(description),category=VALUES(category),version_no=VALUES(version_no),attachment_id=VALUES(attachment_id),file_name=VALUES(file_name),extension=VALUES(extension),active=1,uploaded_at=CURRENT_TIMESTAMP,uploaded_by=VALUES(uploaded_by)
            """, id, code, title, blank(request.get("description")), category, version, stored.getId(), fileName, extension, userId);

        if (StringUtils.hasText(oldAttachmentId) && !oldAttachmentId.equals(stored.getId())) {
            try { attachmentService.delete(oldAttachmentId); } catch (DoesNotExist ignored) { }
        }
        return get(id);
    }

    @Transactional
    public void deactivate(String id) {
        jdbcTemplate.update("UPDATE company_form_document SET active=0 WHERE id=?", id);
    }

    public Download download(String id) throws DoesNotExist {
        Map<String, Object> form = get(id);
        String attachmentId = Objects.toString(form.get("attachment_id"));
        byte[] bytes = attachmentService.getBytes(attachmentId);
        String extension = Objects.toString(form.get("extension"), "pdf").toLowerCase();
        String contentType = switch (extension) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
        return new Download(bytes, contentType, Objects.toString(form.get("file_name"), "form." + extension));
    }

    public record Download(byte[] bytes, String contentType, String fileName) {}

    private static String required(Map<String, Object> request, String key) {
        String value = blank(request.get(key));
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException(key + " is required");
        return value;
    }
    private static String blank(Object value) { return value == null ? null : value.toString().trim(); }
}
