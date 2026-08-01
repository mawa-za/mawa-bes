package za.co.mawa.bes.dto.v2.inbox;

import lombok.Builder;
import lombok.Getter;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;

import java.util.List;

@Getter
@Builder
public class UserInboxResponse {
    private String userId;
    private long unreadCount;
    private long pendingApprovalCount;
    private List<ApprovalRequestResponse> pendingApprovals;
    private List<UserNotificationResponse> notifications;
}
