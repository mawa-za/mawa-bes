package za.co.mawa.bes.dto.v2.funeral;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class InitiateFuneralClaimsDto {
    /**
     * Use membershipId values returned by GET /v2/funeral/check-membership/{identityNumber}.
     */
    private List<String> memberships;
    private String causeOfDeath;
    private String deathCertificateNo;
    private String notes;
}
