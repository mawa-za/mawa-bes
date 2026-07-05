package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.integration.FnbIntegrationSettingsDto;
import za.co.mawa.bes.service.SettingService;

@Service
@RequiredArgsConstructor
public class FnbIntegrationAdministrationService {
    private static final String FNB = "FNB-API";
    private static final String EFT = "EFT-BANK-ACCOUNT";
    private final SettingService settingService;

    public FnbIntegrationSettingsDto getSettings() {
        FnbIntegrationSettingsDto dto = new FnbIntegrationSettingsDto();
        dto.setEnabled(parseBoolean(settingService.getSetting("ENABLED", FNB), settingService.getSetting("FNB-INTEGRATION-ENABLED", FNB)));
        dto.setBaseUrl(settingService.getSetting("BASE-URL", FNB));
        dto.setClientIdSecret(firstNonBlank(settingService.getSetting("CLIENT-ID-SECRET", FNB), settingService.getSetting("CLIENT-ID", FNB)));
        dto.setClientSecretSecret(firstNonBlank(settingService.getSetting("CLIENT-SECRET-SECRET", FNB), settingService.getSetting("CLIENT-SECRET", FNB)));
        dto.setPopRecipient(settingService.getSetting("POP-RECIPIENT", FNB));
        dto.setDebtorAccountNumberSecret(firstNonBlank(settingService.getSetting("ACCOUNT-NUMBER-SECRET", EFT), settingService.getSetting("ACCOUNT-NUMBER", EFT)));
        dto.setDebtorAccountHolder(settingService.getSetting("ACCOUNT-HOLDER", EFT));
        dto.setDebtorBranchCode(settingService.getSetting("BRANCH-CODE", EFT));
        dto.setDebtorAccountType(settingService.getSetting("ACCOUNT-TYPE", EFT));
        dto.setDebtorBankName(settingService.getSetting("BANK-NAME", EFT));
        return dto;
    }

    @Transactional
    public FnbIntegrationSettingsDto save(FnbIntegrationSettingsDto request) {
        if (request == null) {
            return getSettings();
        }
        upsert("ENABLED", FNB, boolString(request.getEnabled()));
        upsert("BASE-URL", FNB, request.getBaseUrl());
        upsert("CLIENT-ID-SECRET", FNB, request.getClientIdSecret());
        upsert("CLIENT-SECRET-SECRET", FNB, request.getClientSecretSecret());
        upsert("POP-RECIPIENT", FNB, request.getPopRecipient());
        upsert("ACCOUNT-NUMBER-SECRET", EFT, request.getDebtorAccountNumberSecret());
        upsert("ACCOUNT-HOLDER", EFT, request.getDebtorAccountHolder());
        upsert("BRANCH-CODE", EFT, request.getDebtorBranchCode());
        upsert("ACCOUNT-TYPE", EFT, request.getDebtorAccountType());
        upsert("BANK-NAME", EFT, request.getDebtorBankName());
        return getSettings();
    }

    private void upsert(String attribute, String group, String value) {
        settingService.upsertSetting(attribute, group, value == null ? "" : value.trim());
    }

    private Boolean parseBoolean(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "1".equals(value);
            }
        }
        return false;
    }

    private String boolString(Boolean value) {
        return Boolean.TRUE.equals(value) ? "true" : "false";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }
}
