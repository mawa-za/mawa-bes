package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.notification.NotificationCreateDto;
import za.co.mawa.bes.dto.notification.NotificationDto;

public interface NotificationDao {
    String create(NotificationCreateDto notificationCreateDto);
    boolean send(String id);
    void sendAll();

}
