package za.co.mawa.bes.dto.v2.funeral;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FuneralInvoiceSplitDto {
    private String entityName;
    private String entityType; // BURIAL_SOCIETY or FAMILY_REP
    private String partnerId;
    private Long amountCents;
    private String description;
    private String membershipClaimId;
    private String coverSource;
    private String sourceTenantId;
}
