package za.co.mawa.bes.dto.v2.membership.claim;

import java.util.ArrayList;
import java.util.List;

public class MembershipClaimsAttachRequest {

    private List<String> claimIds = new ArrayList<>();

    public List<String> getClaimIds() {
        return claimIds;
    }

    public void setClaimIds(List<String> claimIds) {
        this.claimIds = claimIds;
    }
}