package za.co.mawa.bes.service.v2;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;

@Component
public class FuneralUnderwritingApprovalHandler implements ApprovalCompletionHandler {

    private final ObjectProvider<ThirdPartyFuneralUnderwritingService> serviceProvider;

    public FuneralUnderwritingApprovalHandler(ObjectProvider<ThirdPartyFuneralUnderwritingService> serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Override
    public ApprovalType supports() {
        return ApprovalType.FUNERAL_UNDERWRITING;
    }

    @Override
    public void onApproved(ApprovalRequestEntity request, String actor) {
        serviceProvider.getObject().completeApproval(request.getReferenceId(), true, actor);
    }

    @Override
    public void onRejected(ApprovalRequestEntity request, String actor) {
        serviceProvider.getObject().completeApproval(request.getReferenceId(), false, actor);
    }

    @Override
    public void onCancelled(ApprovalRequestEntity request, String actor) {
        serviceProvider.getObject().completeApproval(request.getReferenceId(), false, actor);
    }
}
