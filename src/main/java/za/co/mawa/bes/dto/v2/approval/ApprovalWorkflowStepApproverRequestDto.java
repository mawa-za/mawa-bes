package za.co.mawa.bes.dto.v2.approval;

import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.ApproverType;

@Getter
@Setter
public class ApprovalWorkflowStepApproverRequestDto {

    private ApproverType approverType;

    private String approverValue;

    private String approverName;

    private Boolean active = true;
}