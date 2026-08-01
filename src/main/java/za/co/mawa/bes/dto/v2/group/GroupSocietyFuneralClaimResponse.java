package za.co.mawa.bes.dto.v2.group;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class GroupSocietyFuneralClaimResponse {
    private String id;
    private String claimNo;
    private String funeralServiceId;
    private String groupSocietyId;
    private String groupNo;
    private String societyName;
    private String deceasedFirstNames;
    private String deceasedLastName;
    private String identityType;
    private String identityNumber;
    private Long requestedCoverCents;
    private Long approvedCoverCents;
    private String status;
    private String approvalRequestId;
    private String notes;
    private LocalDateTime createdAt;
}
