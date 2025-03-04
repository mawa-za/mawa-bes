package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.SettingEntity;
import za.co.mawa.bes.entity.SettingPKEntity;
import za.co.mawa.bes.repository.SettingRepository;
import za.co.mawa.bes.dao.SettingDao;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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
    public void createSetting(String attribute, String setting, String value) {
        SettingEntity newSetting = new SettingEntity();
        SettingPKEntity settingPKEntity = new SettingPKEntity();
        settingPKEntity.setSetting(setting);
        settingPKEntity.setAttribute(attribute);
        newSetting.setSettingsPK(settingPKEntity);
        newSetting.setValue(value);

        settingRepository.save(newSetting);
    }

    public String getSetting(String attribute, String setting ) {
        SettingPKEntity settingPKEntity = new SettingPKEntity();
        settingPKEntity.setSetting(setting);
        settingPKEntity.setAttribute(attribute);

        Optional<SettingEntity> settingEntity = settingRepository.findById(settingPKEntity);
        return settingEntity.map(SettingEntity::getValue).orElse(null);
    }

    public void updateSetting(String attribute, String setting, String newValue) {
        SettingPKEntity settingPKEntity = new SettingPKEntity();
        settingPKEntity.setSetting(setting);
        settingPKEntity.setAttribute(attribute);

        Optional<SettingEntity> settingEntity = settingRepository.findById(settingPKEntity);
        if (settingEntity.isPresent()) {
            SettingEntity existingSetting = settingEntity.get();
            existingSetting.setValue(newValue);
            settingRepository.save(existingSetting);
        }
    }

    public void deleteSetting(String attribute, String setting) {
        SettingPKEntity settingPKEntity = new SettingPKEntity();
        settingPKEntity.setSetting(setting);
        settingPKEntity.setAttribute(attribute);

        settingRepository.deleteById(settingPKEntity);
    }
}
