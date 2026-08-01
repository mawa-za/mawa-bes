package za.co.mawa.bes.service.v2;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;
@Component @RequiredArgsConstructor
public class GroupSocietyStatusApprovalHandler implements ApprovalCompletionHandler {
    private final GroupSocietyApprovalService service;
    public ApprovalType supports(){ return ApprovalType.GROUP_SOCIETY_STATUS_CHANGE; }
    public void onApproved(ApprovalRequestEntity request,String actor){ service.completeStatus(request.getReferenceId(),true,actor); }
    public void onRejected(ApprovalRequestEntity request,String actor){ service.completeStatus(request.getReferenceId(),false,actor); }
    public void onCancelled(ApprovalRequestEntity request,String actor){ service.completeStatus(request.getReferenceId(),false,actor); }
}
