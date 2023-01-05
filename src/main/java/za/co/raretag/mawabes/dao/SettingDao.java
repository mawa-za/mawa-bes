package za.co.raretag.mawabes.dao;

import java.util.Properties;

public interface SettingDao {
    Properties getSettings(String type);
}
