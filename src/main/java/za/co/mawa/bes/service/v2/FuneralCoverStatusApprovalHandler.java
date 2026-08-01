package za.co.mawa.bes.service.v2;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;
@Component @RequiredArgsConstructor
public class FuneralCoverStatusApprovalHandler implements ApprovalCompletionHandler {
    private final ThirdPartyFuneralUnderwritingService service;
    public ApprovalType supports(){ return ApprovalType.FUNERAL_COVER_STATUS_CHANGE; }
    public void onApproved(ApprovalRequestEntity request,String actor){ service.completeApproval(request.getReferenceId(),true,actor); }
    public void onRejected(ApprovalRequestEntity request,String actor){ service.completeApproval(request.getReferenceId(),false,actor); }
    public void onCancelled(ApprovalRequestEntity request,String actor){ service.completeApproval(request.getReferenceId(),false,actor); }
}
