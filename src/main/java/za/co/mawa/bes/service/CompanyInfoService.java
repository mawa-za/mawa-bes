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
    public String getCompanyAddress() {
        Properties properties = settingService.getSettings("TENANT");
        try {
            return properties.get("COMPANY-ADDRESS").toString();
        } catch (Exception ex) {
            return "";
        }
    }
    public String getCompanyTelephoneNumber() {
        Properties properties = settingService.getSettings("TENANT");
        try {
            return properties.get("COMPANY-TELEPHONE-NUMBER").toString();
        } catch (Exception ex) {
            return "";
        }
    }

    public String getVATNumber() {
        Properties properties = settingService.getSettings("TENANT");
        try {
            return properties.get("VAT-NUMBER").toString();
        } catch (Exception ex) {
            return "";
        }
    }
}
