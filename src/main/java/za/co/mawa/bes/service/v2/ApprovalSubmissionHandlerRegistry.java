package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ApprovalSubmissionHandlerRegistry {

    private final List<ApprovalSubmissionHandler> handlers;

    public void handleSubmit(ApprovalRequestEntity approvalRequest, String actionBy) {
        handlers.stream()
                .filter(handler -> handler.supports() == approvalRequest.getApprovalType())
                .findFirst()
                .ifPresent(handler -> handler.onSubmit(approvalRequest, actionBy));
    }
}
