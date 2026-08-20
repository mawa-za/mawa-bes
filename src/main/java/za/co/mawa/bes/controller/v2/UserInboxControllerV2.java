package za.co.mawa.bes.controller.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.v2.inbox.InboxCountsResponse;
import za.co.mawa.bes.dto.v2.inbox.UserInboxResponse;
import za.co.mawa.bes.dto.v2.inbox.UserNotificationResponse;

import java.util.List;
import za.co.mawa.bes.service.v2.UserInboxService;

@CrossOrigin
@RestController
@RequestMapping("v2/inbox")
@RequiredArgsConstructor
public class UserInboxControllerV2 {

    private final UserInboxService inboxService;

    @GetMapping
    public UserInboxResponse getInbox(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return inboxService.getInbox(currentUser(headerUserId), limit);
    }


    @GetMapping("/notifications")
    public List<UserNotificationResponse> getNotifications(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "true") boolean unreadOnly
    ) {
        return inboxService.getNotifications(currentUser(headerUserId), limit, unreadOnly);
    }

    @GetMapping("/counts")
    public InboxCountsResponse getCounts(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        return inboxService.getCounts(currentUser(headerUserId));
    }

    @PutMapping("/{notificationId}/read")
    public void markRead(
            @PathVariable String notificationId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        inboxService.markRead(currentUser(headerUserId), notificationId);
    }

    @PutMapping("/read-all")
    public void markAllRead(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        inboxService.markAllRead(currentUser(headerUserId));
    }

    private String currentUser(String headerUserId) {
        if (UserContext.getCurrentUserId() != null && !UserContext.getCurrentUserId().isBlank()) {
            return UserContext.getCurrentUserId();
        }
        if (headerUserId != null && !headerUserId.isBlank()) return headerUserId;
        if (UserContext.getCurrentUser() != null && !UserContext.getCurrentUser().isBlank()) {
            return UserContext.getCurrentUser();
        }
        throw new RuntimeException("Current user could not be determined");
    }
}
