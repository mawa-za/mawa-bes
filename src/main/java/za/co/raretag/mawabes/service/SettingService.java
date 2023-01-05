package za.co.raretag.mawabes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.raretag.mawabes.entity.SettingEntity;
import za.co.raretag.mawabes.dao.SettingDao;
import za.co.raretag.mawabes.repository.SettingRepository;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
@Service
public class SettingService implements SettingDao {
    @Autowired
    SettingRepository settingRepository;
    @Override
    public Properties getSettings(String type) {
        List<SettingEntity> settings =  settingRepository.findAll();
        Iterator<SettingEntity> it = settings.iterator();
        Properties props = new Properties();
        while (it.hasNext()) {
            SettingEntity setting = it.next();
            if ("type".equals(setting.getSettingsPK().getSetting())) {
                props.put(setting.getSettingsPK().getAttribute(), setting.getValue());
            }
        }
        return props;
    }
}
