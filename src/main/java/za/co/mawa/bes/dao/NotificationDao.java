package za.co.mawa.bes.dao;

import za.co.mawa.bes.dto.notification.NotificationCreate1Dto;
import za.co.mawa.bes.dto.notification.NotificationCreateDto;
import za.co.mawa.bes.dto.notification.NotificationDto;
import za.co.mawa.bes.exception.PartnerNotFoundException;
import za.co.mawa.bes.exception.TransactionNotFound;

import java.util.ArrayList;

public interface NotificationDao {
    String create(NotificationCreate1Dto notificationCreateDto) throws Exception;
    boolean send(String id) throws PartnerNotFoundException;
    void sendAll();

    NotificationDto notifications(String id) throws TransactionNotFound;

    NotificationDto get(String id) throws TransactionNotFound;

    void read(String id) throws Exception;

    NotificationDto getPartnerNotifications(String id) throws TransactionNotFound;

}
