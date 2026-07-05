package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Autowired
    UserService userService;

    @Autowired
    AttachmentRepository attachmentRepository;

    @Autowired
    FieldOptionService fieldOptionService;

    @Autowired
    AttachmentStorageService attachmentStorageService;

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
        return saveBytes(bytes, attachmentCreateDto.getExtension(), attachmentCreateDto.getDocumentType(), attachmentCreateDto.getObjectId());
    }

    @Transactional
    public AttachmentEntity saveBytes(byte[] bytes, String extension, String documentType, String objectId) {
        AttachmentStorageService.StoredAttachment stored = attachmentStorageService.store(bytes, extension, objectId, documentType);

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

    @Transactional
    public int migrateLegacyDatabaseFilesToGcp() {
        if (!attachmentStorageService.isGcpConfigured()) {
            throw new IllegalStateException("Attachment GCP storage is not configured. Set MAWA_ATTACHMENT_BUCKET.");
        }
        int migrated = 0;
        for (AttachmentEntity attachmentEntity : attachmentRepository.findAll()) {
            if (StringUtils.hasText(attachmentEntity.getFilePath()) || attachmentEntity.getFile() == null || attachmentEntity.getFile().length == 0) {
                continue;
            }
            AttachmentStorageService.StoredAttachment stored = attachmentStorageService.store(
                    attachmentEntity.getFile(),
                    attachmentEntity.getExtension(),
                    attachmentEntity.getObjectId(),
                    attachmentEntity.getDocumentType()
            );
            attachmentEntity.setStorageProvider(stored.storageProvider());
            attachmentEntity.setStorageBucket(stored.bucket());
            attachmentEntity.setFilePath(stored.path());
            attachmentEntity.setContentType(stored.contentType());
            attachmentEntity.setFileSize(stored.fileSize());
            attachmentEntity.setFile(null);
            attachmentRepository.save(attachmentEntity);
            migrated++;
        }
        return migrated;
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
