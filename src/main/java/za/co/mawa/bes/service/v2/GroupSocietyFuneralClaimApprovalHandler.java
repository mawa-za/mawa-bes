package za.co.mawa.bes.service.v2;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;

@Component
public class GroupSocietyFuneralClaimApprovalHandler implements ApprovalCompletionHandler {

    private final ObjectProvider<GroupSocietyFuneralClaimService> serviceProvider;
    private final ObjectProvider<FuneralClaimSettlementService> settlementProvider;

    public GroupSocietyFuneralClaimApprovalHandler(
            ObjectProvider<GroupSocietyFuneralClaimService> serviceProvider,
            ObjectProvider<FuneralClaimSettlementService> settlementProvider
    ) {
        this.serviceProvider = serviceProvider;
        this.settlementProvider = settlementProvider;
    }

    @Override
    public ApprovalType supports() {
        return ApprovalType.GROUP_SOCIETY_FUNERAL_CLAIM;
    }

    @Override
    @Transactional
    public void onApproved(ApprovalRequestEntity request, String actor) {
        serviceProvider.getObject().complete(request.getReferenceId(), true, actor);
        settlementProvider.getObject().settleApprovedGroupSocietyClaim(request.getReferenceId(), actor);
    }

    @Override
    public void onRejected(ApprovalRequestEntity request, String actor) {
        serviceProvider.getObject().complete(request.getReferenceId(), false, actor);
    }

    @Override
    public void onCancelled(ApprovalRequestEntity request, String actor) {
        serviceProvider.getObject().complete(request.getReferenceId(), false, actor);
    }
}
