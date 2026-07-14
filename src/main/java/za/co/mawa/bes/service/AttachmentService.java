package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dao.AttachmentDao;
import za.co.mawa.bes.dto.attachment.AttachmentCreateDto;
import za.co.mawa.bes.dto.attachment.AttachmentDto;
import za.co.mawa.bes.dto.attachment.AttachmentInboundDto;
import za.co.mawa.bes.dto.attachment.AttachmentOutboundDto;
import za.co.mawa.bes.entity.AttachmentEntity;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.repository.AttachmentRepository;
import za.co.mawa.bes.utils.Field;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Service
public class AttachmentService implements AttachmentDao {

    private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);

    @Autowired
    UserService userService;

    @Autowired
    AttachmentRepository attachmentRepository;

    @Autowired
    FieldOptionService fieldOptionService;

    @Autowired
    AttachmentStorageService attachmentStorageService;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Override
    @Transactional
    public void save(AttachmentCreateDto attachmentCreateDto) throws Exception {
        saveAndReturn(attachmentCreateDto);
    }

    @Transactional
    public AttachmentEntity saveAndReturn(AttachmentCreateDto attachmentCreateDto) throws Exception {
        if (attachmentCreateDto == null || !StringUtils.hasText(attachmentCreateDto.getFile())) {
            throw new IllegalArgumentException("Attachment file is required");
        }
        byte[] bytes = Base64.getDecoder().decode(stripDataUrlPrefix(attachmentCreateDto.getFile()));
        return saveBytes(bytes, attachmentCreateDto.getExtension(), attachmentCreateDto.getObjectType(), attachmentCreateDto.getObjectId(), attachmentCreateDto.getDocumentType());
    }

    @Transactional
    public AttachmentEntity saveBytes(byte[] bytes, String extension, String documentType, String objectId) {
        return saveBytes(bytes, extension, documentType, objectId, documentType);
    }

    @Transactional
    public AttachmentEntity saveBytes(byte[] bytes, String extension, String objectType, String objectId, String documentType) {
        AttachmentStorageService.StoredAttachment stored = attachmentStorageService.store(bytes, extension, objectType, objectId, documentType);

        AttachmentEntity attachmentEntity = new AttachmentEntity();
        attachmentEntity.setUploadBy(UserContext.getCurrentUser());
        attachmentEntity.setUploadDate(new Date());
        attachmentEntity.setUploadTime(new Date());
        attachmentEntity.setDocumentType(documentType);
        attachmentEntity.setObjectId(objectId);
        attachmentEntity.setExtension(extension);
        attachmentEntity.setStorageProvider(stored.storageProvider());
        attachmentEntity.setStorageBucket(stored.bucket());
        attachmentEntity.setFilePath(stored.path());
        attachmentEntity.setContentType(stored.contentType());
        attachmentEntity.setFileSize(stored.fileSize());
        attachmentEntity.setFile(null);
        return attachmentRepository.save(attachmentEntity);
    }

    @Override
    @Transactional
    public String get(String id) throws DoesNotExist {
        AttachmentEntity attachmentEntity = attachmentRepository.findById(id).orElseThrow(DoesNotExist::new);
        attachmentEntity.setDownloadBy(UserContext.getCurrentUser());
        attachmentEntity.setDownloadDate(new Date());
        byte[] fileBytes = getBytes(attachmentEntity);
        attachmentRepository.save(attachmentEntity);
        return Base64.getEncoder().encodeToString(fileBytes);
    }

    public byte[] getBytes(String id) throws DoesNotExist {
        AttachmentEntity attachmentEntity = attachmentRepository.findById(id).orElseThrow(DoesNotExist::new);
        return getBytes(attachmentEntity);
    }

    public byte[] getBytes(AttachmentEntity attachmentEntity) {
        if (attachmentEntity == null) {
            throw new IllegalArgumentException("Attachment is required");
        }
        if (StringUtils.hasText(attachmentEntity.getFilePath())) {
            return attachmentStorageService.read(attachmentEntity.getStorageBucket(), attachmentEntity.getFilePath());
        }
        if (attachmentEntity.getFile() != null) {
            return attachmentEntity.getFile();
        }
        throw new IllegalStateException("Attachment has no stored file path and no legacy database file content");
    }

    public AttachmentOutboundDto getDocumentByType(AttachmentInboundDto attachmentInboundDto) throws DoesNotExist {
        AttachmentEntity attachmentEntity = attachmentRepository.findByObjectDocumentType(attachmentInboundDto.getObjectId(), attachmentInboundDto.getDocumentType());
        if (attachmentEntity == null) {
            throw new DoesNotExist();
        }
        AttachmentOutboundDto attachmentOutboundDto = new AttachmentOutboundDto();
        attachmentOutboundDto.setFile(Base64.getEncoder().encodeToString(getBytes(attachmentEntity)));
        attachmentOutboundDto.setExtension(attachmentEntity.getExtension());
        return attachmentOutboundDto;
    }

    public AttachmentDto getOne(String id) throws DoesNotExist {
        AttachmentEntity attachmentEntity = attachmentRepository.findById(id).orElseThrow(DoesNotExist::new);
        return toDto(attachmentEntity, true);
    }

    @Override
    public List<AttachmentDto> getAll(String objectId) {
        List<AttachmentDto> attachmentDtoList = new ArrayList<>();
        List<AttachmentEntity> attachmentEntityList = attachmentRepository.findByObjectId(objectId);
        for (AttachmentEntity attachmentEntity : attachmentEntityList) {
            attachmentDtoList.add(toDto(attachmentEntity, false));
        }
        return attachmentDtoList;
    }

    @Override
    @Transactional
    public void delete(String id) throws DoesNotExist {
        AttachmentEntity attachmentEntity = attachmentRepository.findById(id).orElseThrow(DoesNotExist::new);
        if (StringUtils.hasText(attachmentEntity.getFilePath())) {
            attachmentStorageService.delete(attachmentEntity.getStorageBucket(), attachmentEntity.getFilePath());
        }
        attachmentRepository.deleteById(id);
    }

    public int migrateLegacyDatabaseFilesToGcp() {
        return migrateLegacyDatabaseFilesToGcpWithResult().migrated();
    }

    public synchronized MigrationResult migrateLegacyDatabaseFilesToGcpWithResult() {
        if (!attachmentStorageService.isGcpConfigured()) {
            throw new IllegalStateException("Attachment GCP storage is not configured. Set MAWA_ATTACHMENT_BUCKET.");
        }

        final int batchSize = 100;
        int attempted = 0;
        int migrated = 0;
        int failed = 0;
        String cursor = "";
        List<String> failureMessages = new ArrayList<>();

        while (true) {
            List<String> ids = attachmentRepository.findLegacyAttachmentIdsAfter(
                    cursor,
                    PageRequest.of(0, batchSize)
            );
            if (ids.isEmpty()) {
                break;
            }

            for (String id : ids) {
                cursor = id;
                attempted++;
                try {
                    Integer result = new TransactionTemplate(transactionManager)
                            .execute(status -> migrateOneLegacyAttachment(id));
                    if (result != null && result > 0) {
                        migrated += result;
                    }
                } catch (Exception ex) {
                    failed++;
                    String failure = "id=" + id + ": " + rootMessage(ex);
                    if (failureMessages.size() < 10) {
                        failureMessages.add(failure);
                    }
                    // Continue beyond a corrupt/unreadable attachment so one bad
                    // row cannot prevent later attachments from being migrated.
                    log.error("Attachment GCS migration failed for {}", failure, ex);
                }
            }
        }

        long remaining = countLegacyDatabaseFiles();
        log.info(
                "Attachment GCS migration completed: attempted={}, migrated={}, failed={}, remaining={}",
                attempted,
                migrated,
                failed,
                remaining
        );
        return new MigrationResult(attempted, migrated, failed, remaining, failureMessages);
    }

    public long countLegacyDatabaseFiles() {
        return attachmentRepository.countLegacyDatabaseFiles();
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        String message = null;
        while (current != null) {
            if (StringUtils.hasText(current.getMessage())) {
                message = current.getMessage();
            }
            current = current.getCause();
        }
        return StringUtils.hasText(message) ? message : error.getClass().getSimpleName();
    }

    public record MigrationResult(
            int attempted,
            int migrated,
            int failed,
            long remaining,
            List<String> failures
    ) {
        public boolean completed() {
            return remaining == 0;
        }
    }

    private int migrateOneLegacyAttachment(String id) {
        AttachmentEntity attachmentEntity = attachmentRepository.findById(id).orElse(null);
        if (attachmentEntity == null
                || StringUtils.hasText(attachmentEntity.getFilePath())
                || attachmentEntity.getFile() == null
                || attachmentEntity.getFile().length == 0) {
            return 0;
        }

        AttachmentStorageService.StoredAttachment stored = attachmentStorageService.store(
                attachmentEntity.getFile(),
                attachmentEntity.getExtension(),
                attachmentEntity.getDocumentType(),
                attachmentEntity.getObjectId(),
                attachmentEntity.getDocumentType()
        );
        attachmentEntity.setStorageProvider(stored.storageProvider());
        attachmentEntity.setStorageBucket(stored.bucket());
        attachmentEntity.setFilePath(stored.path());
        attachmentEntity.setContentType(stored.contentType());
        attachmentEntity.setFileSize(stored.fileSize());
        attachmentEntity.setFile(null);
        attachmentRepository.saveAndFlush(attachmentEntity);
        return 1;
    }

    private AttachmentDto toDto(AttachmentEntity attachmentEntity, boolean depositDocumentType) {
        AttachmentDto attachmentDto = new AttachmentDto();
        attachmentDto.setId(attachmentEntity.getId());
        if (depositDocumentType) {
            attachmentDto.setDocumentType(fieldOptionService.getFieldOption(Field.DOCUMENT_TYPE_DEPOSIT, attachmentEntity.getDocumentType()));
        } else {
            attachmentDto.setDocumentType(fieldOptionService.getOption(attachmentEntity.getDocumentType()));
        }
        attachmentDto.setUploadDate(attachmentEntity.getUploadDate());
        attachmentDto.setUploadTime(attachmentEntity.getUploadTime());
        try {
            attachmentDto.setUploadBy(userService.getUserByName(attachmentEntity.getUploadBy()).getPartner());
        } catch (Exception ignored) {
        }
        try {
            attachmentDto.setCreatedBy(userService.getUserByName(attachmentEntity.getUploadBy()).getPartner());
        } catch (Exception ignored) {
        }
        attachmentDto.setExtension(attachmentEntity.getExtension());
        attachmentDto.setObjectId(attachmentEntity.getObjectId());
        attachmentDto.setFilePath(attachmentEntity.getFilePath());
        attachmentDto.setStorageProvider(attachmentEntity.getStorageProvider());
        attachmentDto.setContentType(attachmentEntity.getContentType());
        attachmentDto.setFileSize(attachmentEntity.getFileSize());
        return attachmentDto;
    }

    private String stripDataUrlPrefix(String file) {
        if (!StringUtils.hasText(file)) {
            return file;
        }
        int comma = file.indexOf(',');
        if (file.startsWith("data:") && comma > -1) {
            return file.substring(comma + 1);
        }
        return file;
    }
}
