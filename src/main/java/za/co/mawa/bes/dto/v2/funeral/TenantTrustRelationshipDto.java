package za.co.mawa.bes.dto.v2.funeral;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantTrustRelationshipDto {
    private String id;
    private String requesterTenantId;
    private String requesterTenantName;
    private String providerTenantId;
    private String providerTenantName;
    private String integrationType;
    private String status;
    private Boolean membershipLookupAllowed;
    private Boolean claimCreationAllowed;
    private Boolean claimStatusReadAllowed;
    private Boolean settlementAllowed;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime revokedAt;
}
