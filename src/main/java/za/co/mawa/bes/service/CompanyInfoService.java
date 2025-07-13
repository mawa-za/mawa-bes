package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class CompanyInfoService {
    @Autowired
    SettingService settingService;
    public String getCompanyName() {
        Properties properties = settingService.getSettings("TENANT");
        try {
            return properties.get("COMPANY-NAME").toString();
        } catch (Exception ex) {
            return "";
        }
    }
}
