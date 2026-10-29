package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;

@Component
@RequiredArgsConstructor
public class FuneralPackageAmendmentApprovalHandler implements ApprovalCompletionHandler {
    private final FuneralPackageAmendmentService amendments;
    @Override public ApprovalType supports() { return ApprovalType.FUNERAL_PACKAGE_AMENDMENT; }
    @Override public void onApproved(ApprovalRequestEntity request, String actor) {
        amendments.approved(request.getReferenceId(), actor);
    }
    @Override public void onRejected(ApprovalRequestEntity request, String actor) {
        amendments.rejected(request.getReferenceId(), false);
    }
    @Override public void onCancelled(ApprovalRequestEntity request, String actor) {
        amendments.rejected(request.getReferenceId(), true);
    }
}
