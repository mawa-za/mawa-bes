package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.gcp.GcpTenantSecretService;
import za.co.mawa.bes.configuration.gcp.TenantSecretNameService;
import za.co.mawa.bes.dto.v2.integration.FnbIntegrationSettingsDto;
import za.co.mawa.bes.service.SettingService;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FnbIntegrationAdministrationService {
    private static final String FNB = "FNB-API";

    private final SettingService settingService;
    private final GcpTenantSecretService gcpTenantSecretService;
    private final TenantSecretNameService tenantSecretNameService;
    private final PaymentAccountConfigurationService paymentAccountConfigurationService;

    public FnbIntegrationSettingsDto getSettings() {
        String clientIdSecret = tenantSecretNameService.currentTenantSecretName("fnb", "client-id");
        String clientSecretSecret = tenantSecretNameService.currentTenantSecretName("fnb", "client-secret");

        FnbIntegrationSettingsDto dto = new FnbIntegrationSettingsDto();
        dto.setEnabled(parseBoolean(
            settingService.getSetting("ENABLED", FNB),
            settingService.getSetting("FNB-INTEGRATION-ENABLED", FNB)
        ));
        dto.setBaseUrl(settingService.getSetting("BASE-URL", FNB));
        dto.setClientIdSecret(clientIdSecret);
        dto.setClientIdConfigured(gcpTenantSecretService.hasAccessibleSecretVersion(clientIdSecret));
        dto.setClientSecretSecret(clientSecretSecret);
        dto.setClientSecretConfigured(gcpTenantSecretService.hasAccessibleSecretVersion(clientSecretSecret));
        dto.setPopRecipient(settingService.getSetting("POP-RECIPIENT", FNB));

        // Compatibility-only response fields. The source of truth is Payment Account Configuration.
        paymentAccountConfigurationService.activeFnbDebtor().ifPresent(account -> applyDebtorSummary(dto, account));
        return dto;
    }

    @Transactional
    public FnbIntegrationSettingsDto save(FnbIntegrationSettingsDto request) {
        if (request == null) {
            return getSettings();
        }

        String clientIdSecret = tenantSecretNameService.currentTenantSecretName("fnb", "client-id");
        String clientSecretSecret = tenantSecretNameService.currentTenantSecretName("fnb", "client-secret");

        if (Boolean.TRUE.equals(request.getEnabled())) {
            requireConfigured(clientIdSecret, request.getClientId(), "FNB Client ID");
            requireConfigured(clientSecretSecret, request.getClientSecret(), "FNB Client Secret");
            // Integration activation is independent from payment routing readiness.
            // Approved payment requests are still protected by PaymentRequestFnbPaymentQueueService,
            // which refuses to queue them until an active request-type debtor account is configured for FNB.
        }

        saveSecretValue(clientIdSecret, request.getClientId());
        saveSecretValue(clientSecretSecret, request.getClientSecret());

        upsert("ENABLED", FNB, booleanText(request.getEnabled()));
        upsert("BASE-URL", FNB, request.getBaseUrl());
        upsert("CLIENT-ID-SECRET", FNB, clientIdSecret);
        upsert("CLIENT-SECRET-SECRET", FNB, clientSecretSecret);
        upsert("POP-RECIPIENT", FNB, request.getPopRecipient());
        return getSettings();
    }

    private void applyDebtorSummary(FnbIntegrationSettingsDto dto, Map<String, Object> account) {
        dto.setDebtorAccountNumber(mask(ObjectsText.get(account, "account_number")));
        dto.setDebtorAccountNumberConfigured(true);
        dto.setDebtorAccountHolder(ObjectsText.get(account, "account_holder"));
        dto.setDebtorBranchCode(ObjectsText.get(account, "branch_code"));
        dto.setDebtorAccountType(ObjectsText.get(account, "account_type"));
        dto.setDebtorBankName(ObjectsText.get(account, "bank_name"));
    }

    private String mask(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= 4 ? "****" : "****" + trimmed.substring(trimmed.length() - 4);
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
            if (StringUtils.hasText(value)) {
                return "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "1".equals(value);
            }
        }
        return false;
    }

    private String booleanText(Boolean value) {
        return Boolean.TRUE.equals(value) ? "true" : "false";
    }

    private static final class ObjectsText {
        private static String get(Map<String, Object> values, String key) {
            Object value = values.get(key);
            return value == null ? null : value.toString();
        }
    }
}
