package za.co.mawa.bes.dto.v2.funeral;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuneralTenantIntegrationConfigDto {
    private String membershipSourceMode;
    private String externalTenantId;
    private String externalTenantName;
    private String externalTenantPartnerId;
    private Boolean membershipLookupEnabled;
    private Boolean claimCreationEnabled;
    private Boolean claimStatusSyncEnabled;
    private Boolean active;
}
