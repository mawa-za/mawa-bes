package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.exception.DoesNotExist;
import za.co.mawa.bes.dao.AttachmentDao;
import za.co.mawa.bes.dto.AttachmentDto;
import za.co.mawa.bes.entity.AttachmentEntity;
import za.co.mawa.bes.repository.AttachmentRepository;
import za.co.mawa.bes.utils.SimpleKeyGenerator;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
    public void save(AttachmentDto attachmentDto) {

    }

    @Override
    public AttachmentDto saveAttachment(AttachmentDto attachmentDto) throws Exception {
        try {
            AttachmentEntity entity = new AttachmentEntity();
           // String id = key.generateUUID();
            AttachmentDto attach = new AttachmentDto();
            entity.setFile(Base64.getDecoder().decode(attachmentDto.getFile()));
            entity.setUploadedBy(getUser());
            entity.setUploadDate(new Date());
            entity.setUploadTime(new Date());
            return entityToDto(attachmentRepository.save(entity));

        }
        catch (Exception exception)
        {
            throw new Exception();
        }
    }

    @Override
    public AttachmentDto saveFileAttachment(byte[] file) throws Exception {
        try {
            AttachmentEntity entity = new AttachmentEntity();
            // String id = key.generateUUID();
            AttachmentDto attach = new AttachmentDto();
            byte[] base64Bytes = Base64.getEncoder().encode(file);
            entity.setFile(base64Bytes);
            entity.setUploadedBy(getUser());
            entity.setUploadDate(new Date());
            entity.setUploadTime(new Date());
            return entityToDto(attachmentRepository.save(entity));

           }
        catch (Exception exception)
         {
            throw new Exception();
         }
    }

    @Override
    public AttachmentDto getAttachment(String id) throws DoesNotExist {
        AttachmentEntity entity = attachmentRepository.getById(id);
        if(entity != null)
        {
            entity.setDownloadedBy(getUser());
            entity.setDownloadDate(new Date());
            return entityToAttachDto(attachmentRepository.save(entity));
        }
        else{
            throw new DoesNotExist();
           // return null;
        }

    }

    private AttachmentDto entityToDto(AttachmentEntity attachEntity)
    {
         AttachmentDto attach = new AttachmentDto();
         attach.setId(attachEntity.getId());
        return attach;

    }

    private AttachmentDto entityToAttachDto(AttachmentEntity attachEntity)
    {
        AttachmentDto attach = new AttachmentDto();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat formatterTime = new SimpleDateFormat("HH:mm:ss");
            SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy-MM-dd");
            String decodedString = Base64.getEncoder().encodeToString(attachEntity.getFile());
            attach.setId(attachEntity.getId());
            if(attachEntity.getUploadDate() != null){
                attach.setUploadDate(formatterDate.format(attachEntity.getUploadDate()));
            }
            if(attachEntity.getUploadTime() != null) {
                attach.setUploadTime(formatterTime.format(attachEntity.getUploadTime()));
            }
            attach.setUploadedBy(attachEntity.getUploadedBy());
            if(attachEntity.getDownloadDate() != null){
                attach.setDownloadDate(formatter.format(attachEntity.getDownloadDate()));
            }
            attach.setFile(decodedString);

        attach.setDownloadedBy(attachEntity.getDownloadedBy());
        return attach;
    }

    private String getUser()
    {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUser = userDetails.getUsername();
        return currentUser;
    }


}
