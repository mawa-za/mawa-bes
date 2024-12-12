package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.SettingEntity;
import za.co.mawa.bes.repository.SettingRepository;
import za.co.mawa.bes.dao.SettingDao;

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
            if (type.equals(setting.getSettingsPK().getSetting())) {
                props.put(setting.getSettingsPK().getAttribute(), setting.getValue());
            }
        }
        return props;
    }
}
