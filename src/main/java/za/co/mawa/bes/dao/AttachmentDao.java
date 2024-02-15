package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.attachment.AttachmentCreateDto;
import za.co.mawa.bes.dto.attachment.AttachmentDto;
import za.co.mawa.bes.exception.DoesNotExist;

import java.util.List;

public interface AttachmentDao {
    void save(AttachmentCreateDto attachmentCreateDto) throws Exception;
    String get(String id) throws DoesNotExist;
    List<AttachmentDto> getAll(String objectId);
    void delete(String id) throws DoesNotExist;

}
