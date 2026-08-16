package za.co.mawa.bes.dto.v2;

import lombok.Data;

@Data
public class MembershipStatusChangeRequest {
    private String requestedBy;
    private String reason;
}
