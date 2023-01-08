package za.co.raretag.mawabes.dao;

public interface NotificationDao {
    boolean send(String id);
    void sendAll();

}
