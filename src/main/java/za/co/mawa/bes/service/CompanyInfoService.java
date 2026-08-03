package za.co.mawa.bes.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyInfoService {

    private final SettingService settingService;

    public String getCompanyName() {
        return setting("NAME", "COMPANY-NAME");
    }

    public String getCompanyRegistrationNumber() {
        return setting("REGISTRATION-NUMBER", "COMPANY-REGISTRATION-NUMBER");
    }

    public String getVATNumber() {
        return setting("VAT-NUMBER");
    }

    public String getFspNumber() {
        return setting("FSP-NUMBER");
    }

    public String getCompanyAddress() {
        String structured = joinWith(", ",
                setting("ADDRESS-LINE-1"),
                setting("ADDRESS-LINE-2"),
                setting("SUBURB"),
                setting("CITY"),
                setting("POSTAL-CODE")
        );
        return structured.isBlank() ? setting("COMPANY-ADDRESS") : structured;
    }

    public String getCompanyTelephoneNumber() {
        return setting("PHONE", "COMPANY-TELEPHONE-NUMBER");
    }

    public String getCompanyEmail() {
        return setting("EMAIL");
    }

    public String getCompanyWebsite() {
        return setting("WEBSITE");
    }

    public String getContactDetails() {
        return joinWith(" | ", getCompanyTelephoneNumber(), getCompanyEmail(), getCompanyWebsite());
    }

    private String setting(String... attributes) {
        Properties properties = settingService.getSettings("TENANT");
        for (String attribute : attributes) {
            Object value = properties.get(attribute);
            if (value != null && !value.toString().trim().isEmpty()) {
                return value.toString().trim();
            }
        }
        return "";
    }

    private String joinWith(String delimiter, String... values) {
        return Arrays.stream(values)
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isEmpty())
                .collect(Collectors.joining(delimiter));
    }
}
