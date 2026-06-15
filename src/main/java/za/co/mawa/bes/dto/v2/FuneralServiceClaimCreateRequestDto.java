package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuneralServiceClaimCreateRequestDto {

    private String funeralServiceId;
    private String membershipClaimId;
    private String coverSource;
    private String sourceTenantId;
    private String sourceTenantName;
    private String sourceMembershipId;
    private String sourceReference;
    private String burialSocietyPartnerId;
}
