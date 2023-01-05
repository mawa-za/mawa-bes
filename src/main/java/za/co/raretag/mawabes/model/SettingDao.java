package za.co.raretag.mawabes.model;

import java.util.Properties;

public interface SettingDao {
    Properties getSettings(String type);
}
