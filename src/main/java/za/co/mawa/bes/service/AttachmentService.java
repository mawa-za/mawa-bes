package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
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
import za.co.mawa.bes.service.v2.MembershipActionGuardService;

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
    LegacyAttachmentObjectIdResolver legacyAttachmentObjectIdResolver;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MembershipActionGuardService membershipActionGuardService;

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
        membershipActionGuardService.requireActionableForObject(objectId);
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
        AttachmentEntity attachmentEntity = null;
        for (String candidateObjectId : legacyAttachmentObjectIdResolver.resolveObjectIds(attachmentInboundDto.getObjectId())) {
            attachmentEntity = attachmentRepository
                    .findFirstByObjectIdAndDocumentTypeOrderByUploadDateDescUploadTimeDescIdDesc(
                            candidateObjectId, attachmentInboundDto.getDocumentType())
                    .orElse(null);
            if (attachmentEntity != null) {
                break;
            }
        }
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
        List<String> objectIds = legacyAttachmentObjectIdResolver.resolveObjectIds(objectId);
        List<AttachmentEntity> attachmentEntityList = objectIds.isEmpty()
                ? List.of()
                : attachmentRepository.findByObjectIdIn(objectIds);
        for (AttachmentEntity attachmentEntity : attachmentEntityList) {
            attachmentDtoList.add(toDto(attachmentEntity, false));
        }
        return attachmentDtoList;
    }

    @Override
    @Transactional
    public void delete(String id) throws DoesNotExist {
        AttachmentEntity attachmentEntity = attachmentRepository.findById(id).orElseThrow(DoesNotExist::new);
        membershipActionGuardService.requireActionableForObject(attachmentEntity.getObjectId());
        validateBusinessAttachmentDeletion(attachmentEntity);
        if (StringUtils.hasText(attachmentEntity.getFilePath())) {
            attachmentStorageService.delete(attachmentEntity.getStorageBucket(), attachmentEntity.getFilePath());
        }
        attachmentRepository.deleteById(id);
    }

    private void validateBusinessAttachmentDeletion(AttachmentEntity attachment) {
        if (attachment == null || !StringUtils.hasText(attachment.getObjectId())) return;
        String objectId = attachment.getObjectId().trim();
        String paymentStatus = findStatus("payment_request", objectId);
        if (paymentStatus != null && !isPreApprovalStatus(paymentStatus)) {
            throw new IllegalStateException("Payment request attachments can only be deleted before approval is completed");
        }
        String claimStatus = findStatus("membership_claim", objectId);
        if (claimStatus != null && !isPreApprovalStatus(claimStatus)) {
            throw new IllegalStateException("Membership claim attachments can only be deleted before approval is completed");
        }
    }

    private String findStatus(String table, String objectId) {
        try {
            List<String> rows = jdbcTemplate.query(
                    "SELECT status FROM `" + table + "` WHERE id=? LIMIT 1",
                    (rs, rowNum) -> rs.getString("status"), objectId);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isPreApprovalStatus(String status) {
        if (status == null) return false;
        String normalized = status.trim().toUpperCase(java.util.Locale.ROOT);
        return java.util.Set.of("DRAFT", "PENDING", "PENDING_APPROVAL", "AWAITING_APPROVAL", "SUBMITTED")
                .contains(normalized);
    }

    public int migrateLegacyDatabaseFilesToGcp() {
        return migrateLegacyDatabaseFilesToGcpWithResult().migrated();
    }

    public synchronized MigrationResult migrateLegacyDatabaseFilesToGcpWithResult() {
        String cursor = "";
        int attempted = 0;
        int migrated = 0;
        int failed = 0;
        List<String> failures = new ArrayList<>();
        MigrationResult batch;

        do {
            batch = migrateLegacyDatabaseFilesToGcpBatch(cursor, 5);
            attempted += batch.attempted();
            migrated += batch.migrated();
            failed += batch.failed();
            for (String failure : batch.failures()) {
                if (failures.size() < 10) {
                    failures.add(failure);
                }
            }
            cursor = batch.nextCursor();
        } while (!batch.scanComplete());

        return new MigrationResult(
                attempted,
                migrated,
                failed,
                batch.remaining(),
                failures,
                cursor,
                true
        );
    }

    /**
     * Migrates a bounded page of legacy attachment rows. The cursor always
     * advances, including past rows that fail, so a corrupt attachment cannot
     * starve every later attachment. Callers can continue with nextCursor until
     * scanComplete is true.
     */
    public synchronized MigrationResult migrateLegacyDatabaseFilesToGcpBatch(
            String afterId,
            int requestedLimit
    ) {
        if (!attachmentStorageService.isGcpConfigured()) {
            throw new IllegalStateException(
                    "Attachment GCP storage is not configured. Set MAWA_ATTACHMENT_BUCKET."
            );
        }

        int limit = Math.max(1, Math.min(requestedLimit, 10));
        String cursor = StringUtils.hasText(afterId) ? afterId.trim() : "";
        List<String> ids = attachmentRepository.findLegacyAttachmentIdsAfter(
                cursor,
                PageRequest.of(0, limit)
        );

        if (ids.isEmpty()) {
            long remaining = countLegacyDatabaseFiles();
            return new MigrationResult(
                    0,
                    0,
                    0,
                    remaining,
                    List.of(),
                    cursor,
                    true
            );
        }

        int attempted = 0;
        int migrated = 0;
        int failed = 0;
        List<String> failureMessages = new ArrayList<>();

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
                log.error("Attachment GCS migration failed for {}", failure, ex);
            }
        }

        boolean scanComplete = attachmentRepository.findLegacyAttachmentIdsAfter(
                cursor,
                PageRequest.of(0, 1)
        ).isEmpty();
        long remaining = countLegacyDatabaseFiles();

        log.info(
                "Attachment GCS migration batch completed: attempted={}, migrated={}, failed={}, remaining={}, nextCursor={}, scanComplete={}",
                attempted,
                migrated,
                failed,
                remaining,
                cursor,
                scanComplete
        );
        return new MigrationResult(
                attempted,
                migrated,
                failed,
                remaining,
                failureMessages,
                cursor,
                scanComplete
        );
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
            List<String> failures,
            String nextCursor,
            boolean scanComplete
    ) {
        public boolean completed() {
            return remaining == 0;
        }
    }

    private int migrateOneLegacyAttachment(String id) {
        AttachmentEntity attachmentEntity = attachmentRepository.findByIdForMigration(id).orElse(null);
        if (attachmentEntity == null
                || StringUtils.hasText(attachmentEntity.getFilePath())
                || attachmentEntity.getFile() == null) {
            return 0;
        }
        if (attachmentEntity.getFile().length == 0) {
            throw new IllegalStateException("Legacy attachment file is empty");
        }

        AttachmentStorageService.StoredAttachment stored =
                attachmentStorageService.storeLegacyAttachment(
                        attachmentEntity.getFile(),
                        attachmentEntity.getExtension(),
                        attachmentEntity.getDocumentType(),
                        attachmentEntity.getObjectId(),
                        attachmentEntity.getDocumentType(),
                        attachmentEntity.getId()
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
