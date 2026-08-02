package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ApprovalCompletionHandlerRegistry {

    private final ObjectProvider<ApprovalCompletionHandler> handlers;

    public void handleApproved(ApprovalRequestEntity approvalRequest, String actionBy) {
        findHandler(approvalRequest).ifPresent(handler -> handler.onApproved(approvalRequest, actionBy));
    }

    public void handleRejected(ApprovalRequestEntity approvalRequest, String actionBy) {
        findHandler(approvalRequest).ifPresent(handler -> handler.onRejected(approvalRequest, actionBy));
    }

    public void handleCancelled(ApprovalRequestEntity approvalRequest, String actionBy) {
        findHandler(approvalRequest).ifPresent(handler -> handler.onCancelled(approvalRequest, actionBy));
    }

    private Optional<ApprovalCompletionHandler> findHandler(ApprovalRequestEntity approvalRequest) {
        return handlers.orderedStream()
                .filter(handler -> handler.supports() == approvalRequest.getApprovalType())
                .findFirst();
    }
}
