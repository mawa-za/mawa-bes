package za.co.mawa.bes.dto.v2.integration;

import lombok.Data;

@Data
public class FnbIntegrationSettingsDto {
    private Boolean enabled;
    private String baseUrl;
    private String clientId;
    private String clientIdSecret;
    private Boolean clientIdConfigured;
    private String clientSecret;
    private String clientSecretSecret;
    private Boolean clientSecretConfigured;
    private String popRecipient;
    private String debtorAccountNumber;
    private String debtorAccountNumberSecret;
    private Boolean debtorAccountNumberConfigured;
    private String debtorAccountHolder;
    private String debtorBranchCode;
    private String debtorAccountType;
    private String debtorBankName;
}
