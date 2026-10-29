package za.co.mawa.bes.dto.v2.funeral;

import lombok.Data;

@Data
public class FuneralPackageAmendmentRequest {
    private String newPackageId;
    private String reason;
    /** The customer or an authorised family representative has agreed to the amended arrangement. */
    private Boolean customerAcknowledged;
}
