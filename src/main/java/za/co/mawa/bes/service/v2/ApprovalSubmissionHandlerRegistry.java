package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;

@Component
@RequiredArgsConstructor
public class ApprovalSubmissionHandlerRegistry {

    private final ObjectProvider<ApprovalSubmissionHandler> handlers;

    public void handleSubmit(ApprovalRequestEntity approvalRequest, String actionBy) {
        handlers.orderedStream()
                .filter(handler -> handler.supports() == approvalRequest.getApprovalType())
                .findFirst()
                .ifPresent(handler -> handler.onSubmit(approvalRequest, actionBy));
    }
}
