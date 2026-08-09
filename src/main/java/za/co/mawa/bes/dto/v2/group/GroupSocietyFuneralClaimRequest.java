package za.co.mawa.bes.dto.v2.group;

import lombok.Data;

@Data
public class GroupSocietyFuneralClaimRequest {
    private String groupSocietyId;
    private String deceasedFirstNames;
    private String deceasedLastName;
    private String identityType;
    private String identityNumber;
    private Long requestedCoverCents;
    private String requestedBy;
    private String notes;
}
