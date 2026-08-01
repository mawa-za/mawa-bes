package za.co.mawa.bes.dto.v2.inbox;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InboxCountsResponse {
    private long unreadCount;
    private long pendingApprovalCount;
}
