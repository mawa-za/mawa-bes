package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.v2.SystemInstallationFileDtos.DownloadResponse;
import za.co.mawa.bes.dto.v2.SystemInstallationFileDtos.Response;
import za.co.mawa.bes.dto.v2.SystemInstallationFileDtos.UploadRequest;
import za.co.mawa.bes.entity.AttachmentEntity;
import za.co.mawa.bes.entity.v2.SystemInstallationFileEntity;
import za.co.mawa.bes.repository.v2.SystemInstallationFileRepository;
import za.co.mawa.bes.service.AttachmentService;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemInstallationFileService {
    private static final String OBJECT_ID = "SYSTEM-INSTALLATION-FILES";
    private static final String OBJECT_TYPE = "system-installation-files";
    private static final String DOCUMENT_TYPE = "INSTALLATION-FILE";

    private final SystemInstallationFileRepository repository;
    private final AttachmentService attachmentService;

    @Transactional(readOnly = true)
    public List<Response> list() {
        return repository.findByActiveTrueOrderByDisplayNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public Response upload(UploadRequest request) {
        if (request == null || blank(request.getDisplayName())) {
            throw new IllegalArgumentException("Installation file name is required");
        }
        if (blank(request.getFileName())) {
            throw new IllegalArgumentException("File name is required");
        }
        if (blank(request.getFile())) {
            throw new IllegalArgumentException("Installation file content is required");
        }
        String extension = cleanExtension(request.getExtension(), request.getFileName());
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(stripDataUrl(request.getFile()));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Installation file content is not valid base64", exception);
        }
        if (bytes.length == 0) throw new IllegalArgumentException("Installation file is empty");

        AttachmentEntity attachment = attachmentService.saveBytes(
                bytes, extension, OBJECT_TYPE, OBJECT_ID, DOCUMENT_TYPE);
        String actor = UserContext.getCurrentUser();
        SystemInstallationFileEntity entity = repository.save(SystemInstallationFileEntity.builder()
                .displayName(request.getDisplayName().trim())
                .description(blank(request.getDescription()) ? null : request.getDescription().trim())
                .fileName(request.getFileName().trim())
                .extension(extension)
                .attachmentId(attachment.getId())
                .active(true)
                .createdAt(LocalDateTime.now())
                .createdBy(actor)
                .build());
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public DownloadResponse download(String id) throws Exception {
        SystemInstallationFileEntity entity = repository.findById(id)
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .orElseThrow(() -> new IllegalArgumentException("Installation file not found: " + id));
        byte[] bytes = attachmentService.getBytes(entity.getAttachmentId());
        return DownloadResponse.builder()
                .fileName(entity.getFileName())
                .extension(entity.getExtension())
                .file(Base64.getEncoder().encodeToString(bytes))
                .build();
    }

    @Transactional
    public void delete(String id) throws Exception {
        SystemInstallationFileEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Installation file not found: " + id));
        if (!Boolean.TRUE.equals(entity.getActive())) return;
        attachmentService.delete(entity.getAttachmentId());
        entity.setActive(false);
        repository.save(entity);
    }

    private Response toResponse(SystemInstallationFileEntity entity) {
        return Response.builder()
                .id(entity.getId())
                .displayName(entity.getDisplayName())
                .description(entity.getDescription())
                .fileName(entity.getFileName())
                .extension(entity.getExtension())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    private String cleanExtension(String extension, String fileName) {
        String value = blank(extension) ? "" : extension.trim().toLowerCase();
        if (value.startsWith(".")) value = value.substring(1);
        if (value.isBlank() && fileName != null && fileName.contains(".")) {
            value = fileName.substring(fileName.lastIndexOf('.') + 1).trim().toLowerCase();
        }
        return value.isBlank() ? "bin" : value.replaceAll("[^a-z0-9]", "");
    }

    private String stripDataUrl(String value) {
        String trimmed = value == null ? "" : value.trim();
        int comma = trimmed.indexOf(',');
        return trimmed.startsWith("data:") && comma >= 0 ? trimmed.substring(comma + 1) : trimmed;
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
