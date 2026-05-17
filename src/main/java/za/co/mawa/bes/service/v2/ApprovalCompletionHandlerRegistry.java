package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ApprovalCompletionHandlerRegistry {

    private final List<ApprovalCompletionHandler> handlers;

    public void handleApproved(ApprovalRequestEntity approvalRequest, String actionBy) {
        handlers.stream()
                .filter(handler -> handler.supports() == approvalRequest.getApprovalType())
                .findFirst()
                .ifPresent(handler -> handler.onApproved(approvalRequest, actionBy));
    }
}
