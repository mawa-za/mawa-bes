package za.co.mawa.bes.dao;

public interface NotificationDao {
    boolean send(String id);
    void sendAll();

}
