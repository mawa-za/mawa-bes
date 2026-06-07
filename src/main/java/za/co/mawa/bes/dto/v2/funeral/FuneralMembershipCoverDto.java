package za.co.mawa.bes.dto.v2.funeral;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FuneralMembershipCoverDto {
    /**
     * Stable selection id used by funeral screens.
     * LOCAL_TENANT values are formatted as LOCAL:{membershipId}:{deceasedPartnerId}:{deceasedType}.
     * EXTERNAL_TENANT values are formatted as EXTERNAL:{externalCoverId}.
     */
    private String membershipId;
    private String membershipNumber;
    private String burialSocietyName;
    private String burialSocietyPartnerId;
    private Long coverAmountCents;
    private String coverSource;
    private String sourceTenantId;
    private String sourceTenantName;
    private String sourceMembershipId;
    private String sourceReference;
    private String deceasedPartnerId;
    private String deceasedType;
}
