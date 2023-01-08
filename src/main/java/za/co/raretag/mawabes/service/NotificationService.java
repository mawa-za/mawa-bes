package za.co.raretag.mawabes.service;

import org.springframework.stereotype.Service;
import za.co.raretag.mawabes.dao.NotificationDao;

@Service
public class NotificationService implements NotificationDao {
    @Override
    public boolean send(String id) {
        return false;
    }

    @Override
    public void sendAll() {

    }
}
