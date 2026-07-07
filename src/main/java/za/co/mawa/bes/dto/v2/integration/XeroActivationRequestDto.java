package za.co.mawa.bes.dto.v2.integration;

import lombok.Data;

@Data
public class XeroActivationRequestDto {
    private String clientId;
    private String clientSecret;
    private String redirectUrl;
    private Boolean invoiceIntegrationEnabled;
}
