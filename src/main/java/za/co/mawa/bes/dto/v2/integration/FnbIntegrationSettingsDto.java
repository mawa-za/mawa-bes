package za.co.mawa.bes.dto.v2.integration;

import lombok.Data;

@Data
public class FnbIntegrationSettingsDto {
    private Boolean enabled;
    private String baseUrl;
    private String clientIdSecret;
    private String clientSecretSecret;
    private String popRecipient;
    private String debtorAccountNumberSecret;
    private String debtorAccountHolder;
    private String debtorBranchCode;
    private String debtorAccountType;
    private String debtorBankName;
}
