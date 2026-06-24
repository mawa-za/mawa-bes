package za.co.mawa.bes.mapper.notification;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.notification.NotificationCreateRequestDto;
import za.co.mawa.bes.dto.notification.NotificationResponseDto;
import za.co.mawa.bes.dto.notification.NotificationUpdateRequestDto;
import za.co.mawa.bes.entity.notification.NotificationEntity;

@Component
public class NotificationMapper {
    public NotificationResponseDto toResponse(NotificationEntity entity) {
        if (entity == null) return null;
        return NotificationResponseDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .sender(entity.getSender())
                .recipient(entity.getRecipient())
                .subject(entity.getSubject())
                .body(entity.getBody())
                .created_by(entity.getCreated_by())
                .createdAt(entity.getCreatedAt())
                .status(entity.getStatus())
                .build();
    }
    public NotificationEntity toEntity(NotificationCreateRequestDto request) {
        if (request == null) return null;
        NotificationEntity entity = new NotificationEntity();
        entity.setType(request.getType());
        entity.setSender(request.getSender());
        entity.setRecipient(request.getRecipient());
        entity.setSubject(request.getSubject());
        entity.setBody(request.getBody());
        entity.setCreated_by(request.getCreated_by());
        entity.setStatus(request.getStatus());
        return entity;
    }
    public void updateEntity(NotificationEntity entity, NotificationUpdateRequestDto request) {
        if (entity == null || request == null) return;
        entity.setId(request.getId());
        entity.setType(request.getType());
        entity.setSender(request.getSender());
        entity.setRecipient(request.getRecipient());
        entity.setSubject(request.getSubject());
        entity.setBody(request.getBody());
        entity.setCreated_by(request.getCreated_by());
        entity.setStatus(request.getStatus());
    }
}
