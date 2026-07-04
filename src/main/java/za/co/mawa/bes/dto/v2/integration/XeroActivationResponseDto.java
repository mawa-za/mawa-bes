package za.co.mawa.bes.dto.v2.integration;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class XeroActivationResponseDto {
    private boolean invoiceIntegrationEnabled;
    private boolean organisationSelectionRequired;
    private String authenticationUrl;
    private String clientIdSecret;
    private String clientSecretSecret;
    private String refreshTokenSecret;
    private String tenantIdSecret;
    private String redirectUrl;
    private String selectedTenantId;
    private String selectedTenantName;
    private List<XeroConnectionDto> connections;
    private String message;
}
