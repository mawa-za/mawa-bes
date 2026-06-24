package za.co.mawa.bes.mapper.notification;

import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.notification.NotificationLogCreateRequestDto;
import za.co.mawa.bes.dto.notification.NotificationLogResponseDto;
import za.co.mawa.bes.dto.notification.NotificationLogUpdateRequestDto;
import za.co.mawa.bes.entity.notification.NotificationLogEntity;

@Component
public class NotificationLogMapper {
    public NotificationLogResponseDto toResponse(NotificationLogEntity entity) {
        if (entity == null) return null;
        return NotificationLogResponseDto.builder()
                .id(entity.getId())
                .notificationId(entity.getNotificationId())
                .action(entity.getAction())
                .result(entity.getResult())
                .executedAt(entity.getExecutedAt())
                .build();
    }
    public NotificationLogEntity toEntity(NotificationLogCreateRequestDto request) {
        if (request == null) return null;
        NotificationLogEntity entity = new NotificationLogEntity();
        entity.setNotificationId(request.getNotificationId());
        entity.setAction(request.getAction());
        entity.setResult(request.getResult());
        entity.setExecutedAt(request.getExecutedAt());
        return entity;
    }
    public void updateEntity(NotificationLogEntity entity, NotificationLogUpdateRequestDto request) {
        if (entity == null || request == null) return;
        entity.setId(request.getId());
        entity.setNotificationId(request.getNotificationId());
        entity.setAction(request.getAction());
        entity.setResult(request.getResult());
        entity.setExecutedAt(request.getExecutedAt());
    }
}
