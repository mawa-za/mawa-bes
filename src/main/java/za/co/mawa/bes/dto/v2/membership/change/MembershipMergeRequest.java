package za.co.mawa.bes.dto.v2.membership.change;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MembershipMergeRequest {
    private String sourceMembershipId;
    private String reason;
}
