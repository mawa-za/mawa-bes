package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.NotificationDao;
import za.co.mawa.bes.dto.notification.NotificationCreateDto;
import za.co.mawa.bes.entity.notification.NotificationEntity;
import za.co.mawa.bes.entity.notification.NotificationLogEntity;
import za.co.mawa.bes.repository.NotificationRepository;

@Service
public class NotificationService implements NotificationDao {
        @Autowired
    NotificationRepository notificationRepository;
    @Override
    public String create(NotificationCreateDto notificationCreateDto) {
        NotificationEntity notificationEntity = new NotificationEntity();
        notificationEntity.setType(notificationCreateDto.getType());
        notificationEntity.setSender(notificationCreateDto.getSender());
        notificationEntity.setRecipient(notificationCreateDto.getRecipient());
        notificationEntity.setSubject(notificationCreateDto.getSubject());
        notificationEntity.setBody(notificationCreateDto.getBody());
        return notificationRepository.save(notificationEntity).getId();
    }

    @Override
    public boolean send(String id) {
        return false;
    }


    @Override
    public void sendAll() {

    }
}
