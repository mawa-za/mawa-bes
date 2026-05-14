package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.ApproverType;

@Getter
@Setter
public class ApprovalWorkflowStepCreateRequest {

    private Integer stepNo;
    private String stepName;
    private ApproverType approverType;
    private String approverValue;
    private Integer requiredApprovals = 1;
}