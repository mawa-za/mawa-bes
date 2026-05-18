package za.co.mawa.bes.service.v2;

import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;

public interface ApprovalSubmissionHandler {

    ApprovalType supports();

    void onSubmit(ApprovalRequestEntity approvalRequest, String actionBy);
}