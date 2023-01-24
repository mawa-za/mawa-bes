package za.co.mawa.bes.dao;

import java.util.Properties;

public interface SettingDao {
    Properties getSettings(String type);
}
