package za.co.mawa.bes.dto.v2.inbox;

import lombok.Builder;
import lombok.Getter;
import za.co.mawa.bes.enums.UserNotificationType;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserNotificationResponse {
    private String id;
    private UserNotificationType notificationType;
    private String title;
    private String message;
    private String approvalRequestId;
    private Integer approvalStepNo;
    private String approvalType;
    private String approvalStatus;
    private String referenceId;
    private String referenceNo;
    private String actionBy;
    private String actionByDisplayName;
    private String route;
    private LocalDateTime readAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
