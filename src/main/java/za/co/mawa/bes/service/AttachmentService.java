package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.attachment.AttachmentCreateDto;
import za.co.mawa.bes.dto.attachment.AttachmentInboundDto;
import za.co.mawa.bes.dto.attachment.AttachmentOutboundDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.user.UserDto;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.dao.AttachmentDao;
import za.co.mawa.bes.dto.attachment.AttachmentDto;
import za.co.mawa.bes.entity.AttachmentEntity;
import za.co.mawa.bes.repository.AttachmentRepository;
import za.co.mawa.bes.utils.Field;
import za.co.mawa.bes.utils.SimpleKeyGenerator;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Date;
import java.util.Base64;
import java.text.SimpleDateFormat;
import java.util.List;

@Service
public class AttachmentService implements AttachmentDao {

    @Autowired
    UserService userService;

    @Autowired
    SimpleKeyGenerator key;

    @Autowired
    AttachmentRepository attachmentRepository;
    @Autowired
    FieldOptionService fieldOptionService;

    @Override
    public void save(AttachmentCreateDto attachmentCreateDto) throws Exception {
        try {
            AttachmentEntity attachmentEntity = new AttachmentEntity();
            attachmentEntity.setFile(Base64.getDecoder().decode(attachmentCreateDto.getFile()));
            attachmentEntity.setUploadBy(UserContext.getCurrentUser());
            attachmentEntity.setUploadDate(new Date());
            attachmentEntity.setUploadTime(new Date());
            attachmentEntity.setDocumentType(attachmentCreateDto.getDocumentType());
            attachmentEntity.setObjectId(attachmentCreateDto.getObjectId());
            attachmentEntity.setExtension(attachmentCreateDto.getExtension());
            attachmentRepository.save(attachmentEntity);
        } catch (Exception exception) {
            throw new Exception();
        }
    }


    @Override
    public String get(String id) throws DoesNotExist {
        AttachmentEntity attachmentEntity = attachmentRepository.getById(id);
        if (attachmentEntity != null) {
            attachmentEntity.setDownloadBy(UserContext.getCurrentUser());
            attachmentEntity.setDownloadDate(new Date());
            String base64String = Base64.getEncoder().encodeToString(attachmentEntity.getFile());
            attachmentRepository.save(attachmentEntity);
            return base64String;
        } else {
            throw new DoesNotExist();
            // return null;
        }
    }

    public AttachmentOutboundDto getDocumentByType(AttachmentInboundDto attachmentInboundDto) throws DoesNotExist {
        AttachmentEntity attachmentEntity = attachmentRepository.findByObjectDocumentType(attachmentInboundDto.getObjectId(), attachmentInboundDto.getDocumentType());
        AttachmentOutboundDto attachmentOutboundDto = new AttachmentOutboundDto();
        attachmentOutboundDto.setFile(Base64.getEncoder().encodeToString(attachmentEntity.getFile()));
        attachmentOutboundDto.setExtension(attachmentEntity.getExtension());
        return attachmentOutboundDto;
    }

    public AttachmentDto getOne(String id) throws DoesNotExist {

        AttachmentEntity attachmentEntity = attachmentRepository.getById(id);
        AttachmentDto attachmentDto = new AttachmentDto();
        attachmentDto.setId(attachmentEntity.getId());
        attachmentDto.setDocumentType(fieldOptionService.getFieldOption(Field.DOCUMENT_TYPE_DEPOSIT, attachmentEntity.getDocumentType()));
        attachmentDto.setUploadDate(attachmentEntity.getUploadDate());
        attachmentDto.setUploadTime(attachmentEntity.getUploadTime());
        try {
            attachmentDto.setUploadBy(userService.getUserByName(attachmentEntity.getUploadBy()).getPartner());
        } catch (Exception e) {

        }
        try {
            attachmentDto.setCreatedBy(userService.getUserByName(attachmentEntity.getUploadBy()).getPartner());
        } catch (Exception e) {

        }
        attachmentDto.setExtension(attachmentEntity.getExtension());
        return attachmentDto;

    }

    @Override
    public List<AttachmentDto> getAll(String objectId) {
        List<AttachmentDto> attachmentDtoList = new ArrayList<>();
        List<AttachmentEntity> attachmentEntityList = attachmentRepository.findByObjectId(objectId);
        for (AttachmentEntity attachmentEntity : attachmentEntityList) {
            AttachmentDto attachmentDto = new AttachmentDto();
            attachmentDto.setId(attachmentEntity.getId());
            attachmentDto.setDocumentType(fieldOptionService.getOption(attachmentEntity.getDocumentType()));
            attachmentDto.setUploadDate(attachmentEntity.getUploadDate());
            attachmentDto.setUploadTime(attachmentEntity.getUploadTime());
            try {
                attachmentDto.setUploadBy(userService.getUserByName(attachmentEntity.getUploadBy()).getPartner());
            } catch (Exception e) {

            }
            try {
                attachmentDto.setCreatedBy(userService.getUserByName(attachmentEntity.getUploadBy()).getPartner());
            } catch (Exception e) {

            }
            attachmentDto.setExtension(attachmentEntity.getExtension());
            attachmentDtoList.add(attachmentDto);
        }
        return attachmentDtoList;
    }

    @Override
    public void delete(String id) throws DoesNotExist {
        attachmentRepository.deleteById(id);
    }

}
