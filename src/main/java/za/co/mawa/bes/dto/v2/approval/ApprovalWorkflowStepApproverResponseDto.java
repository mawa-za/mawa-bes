package za.co.mawa.bes.dto.v2.approval;

import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.ApproverType;

@Getter
@Setter
public class ApprovalWorkflowStepApproverResponseDto {

    private String id;

    private ApproverType approverType;

    private String approverValue;

    private String approverName;

    private Boolean active;
}