package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.attachment.AttachmentCreateDto;
import za.co.mawa.bes.dto.attachment.AttachmentDto;
import za.co.mawa.bes.exception.DoesNotExist;

public interface AttachmentDao {
    AttachmentDto save(AttachmentCreateDto attachmentCreateDto) throws Exception;
    AttachmentDto get(String id) throws DoesNotExist;



}
