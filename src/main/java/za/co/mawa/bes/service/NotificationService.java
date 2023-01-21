package za.co.mawa.bes.service;

import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.NotificationDao;

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
