package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.attachment.AttachmentDto;
import za.co.mawa.bes.exception.DoesNotExist;

public interface AttachmentDao {
    void save(AttachmentDto attachmentDto);

    AttachmentDto saveAttachment(AttachmentDto attachmentDto) throws Exception;

    AttachmentDto saveFileAttachment(byte[] file) throws Exception;

    AttachmentDto getAttachment(String id) throws DoesNotExist;



}
