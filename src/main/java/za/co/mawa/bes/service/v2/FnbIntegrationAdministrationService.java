package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.gcp.GcpTenantSecretService;
import za.co.mawa.bes.configuration.gcp.TenantSecretNameService;
import za.co.mawa.bes.dto.v2.integration.FnbIntegrationSettingsDto;
import za.co.mawa.bes.service.SettingService;

@Service
@RequiredArgsConstructor
public class FnbIntegrationAdministrationService {
    private static final String FNB = "FNB-API";
    private static final String EFT = "EFT-BANK-ACCOUNT";

    private final SettingService settingService;
    private final GcpTenantSecretService gcpTenantSecretService;
    private final TenantSecretNameService tenantSecretNameService;

    public FnbIntegrationSettingsDto getSettings() {
        String clientIdSecret = tenantSecretNameService.currentTenantSecretName("fnb", "client-id");
        String clientSecretSecret = tenantSecretNameService.currentTenantSecretName("fnb", "client-secret");
        String accountNumberSecret = tenantSecretNameService.currentTenantSecretName("fnb", "account-number");

        FnbIntegrationSettingsDto dto = new FnbIntegrationSettingsDto();
        dto.setEnabled(parseBoolean(settingService.getSetting("ENABLED", FNB), settingService.getSetting("FNB-INTEGRATION-ENABLED", FNB)));
        dto.setBaseUrl(settingService.getSetting("BASE-URL", FNB));
        dto.setClientIdSecret(clientIdSecret);
        dto.setClientIdConfigured(gcpTenantSecretService.hasAccessibleSecretVersion(clientIdSecret));
        dto.setClientSecretSecret(clientSecretSecret);
        dto.setClientSecretConfigured(gcpTenantSecretService.hasAccessibleSecretVersion(clientSecretSecret));
        dto.setPopRecipient(settingService.getSetting("POP-RECIPIENT", FNB));
        dto.setDebtorAccountNumberSecret(accountNumberSecret);
        dto.setDebtorAccountNumberConfigured(gcpTenantSecretService.hasAccessibleSecretVersion(accountNumberSecret));
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

        String clientIdSecret = tenantSecretNameService.currentTenantSecretName("fnb", "client-id");
        String clientSecretSecret = tenantSecretNameService.currentTenantSecretName("fnb", "client-secret");
        String accountNumberSecret = tenantSecretNameService.currentTenantSecretName("fnb", "account-number");

        if (Boolean.TRUE.equals(request.getEnabled())) {
            requireConfigured(clientIdSecret, request.getClientId(), "FNB Client ID");
            requireConfigured(clientSecretSecret, request.getClientSecret(), "FNB Client Secret");
            requireConfigured(accountNumberSecret, request.getDebtorAccountNumber(), "FNB debtor account number");
        }

        saveSecretValue(clientIdSecret, request.getClientId());
        saveSecretValue(clientSecretSecret, request.getClientSecret());
        saveSecretValue(accountNumberSecret, request.getDebtorAccountNumber());

        upsert("ENABLED", FNB, boolString(request.getEnabled()));
        upsert("BASE-URL", FNB, request.getBaseUrl());
        upsert("CLIENT-ID-SECRET", FNB, clientIdSecret);
        upsert("CLIENT-SECRET-SECRET", FNB, clientSecretSecret);
        upsert("POP-RECIPIENT", FNB, request.getPopRecipient());
        upsert("ACCOUNT-NUMBER-SECRET", EFT, accountNumberSecret);
        upsert("ACCOUNT-HOLDER", EFT, request.getDebtorAccountHolder());
        upsert("BRANCH-CODE", EFT, request.getDebtorBranchCode());
        upsert("ACCOUNT-TYPE", EFT, request.getDebtorAccountType());
        upsert("BANK-NAME", EFT, request.getDebtorBankName());
        return getSettings();
    }

    private void saveSecretValue(String secretName, String value) {
        if (StringUtils.hasText(value)) {
            gcpTenantSecretService.createOrAddSecretVersion(secretName, value.trim());
        }
    }

    private void requireConfigured(String secretName, String submittedValue, String label) {
        if (!StringUtils.hasText(submittedValue) && !gcpTenantSecretService.hasAccessibleSecretVersion(secretName)) {
            throw new IllegalArgumentException(label + " is required before enabling FNB integration");
        }
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
}
