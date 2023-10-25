package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.attachment.AttachmentCreateDto;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.dao.AttachmentDao;
import za.co.mawa.bes.dto.attachment.AttachmentDto;
import za.co.mawa.bes.entity.AttachmentEntity;
import za.co.mawa.bes.repository.AttachmentRepository;
import za.co.mawa.bes.utils.SimpleKeyGenerator;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.Base64;
import java.text.SimpleDateFormat;

@Service
public class AttachmentService implements AttachmentDao {

    @Autowired
    UserService userService;

    @Autowired
    SimpleKeyGenerator key;

    @Autowired
    AttachmentRepository attachmentRepository;
    @Override
    public AttachmentDto save(AttachmentCreateDto attachmentCreateDto) throws Exception {
        try {
            AttachmentEntity attachmentEntity = new AttachmentEntity();
            attachmentEntity.setFile(Base64.getDecoder().decode(attachmentCreateDto.getFile()));
            attachmentEntity.setUploadedBy(UserContext.getCurrentUser());
            attachmentEntity.setUploadDate(new Date());
            attachmentEntity.setUploadTime(new Date());
            return entityToDto(attachmentRepository.save(attachmentEntity));
        } catch (Exception exception) {
            throw new Exception();
        }
    }

    @Override
    public AttachmentDto get(String id) throws DoesNotExist {
        AttachmentEntity attachmentEntity = attachmentRepository.getById(id);
        if (attachmentEntity != null) {
            attachmentEntity.setDownloadBy(UserContext.getCurrentUser());
            attachmentEntity.setDownloadDate(new Date());
            return entityToAttachDto(attachmentRepository.save(attachmentEntity));
        } else {
            throw new DoesNotExist();
            // return null;
        }
    }

    private AttachmentDto entityToDto(AttachmentEntity attachEntity) {
        AttachmentDto attach = new AttachmentDto();
        attach.setId(attachEntity.getId());
        return attach;
    }

    private AttachmentDto entityToAttachDto(AttachmentEntity attachEntity) {
        AttachmentDto attach = new AttachmentDto();
        String decodedString = Base64.getEncoder().encodeToString(attachEntity.getFile());
        attach.setId(attachEntity.getId());
        attach.setFile(decodedString);
        return attach;
    }


}
