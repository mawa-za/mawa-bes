package za.co.mawa.bes.dto.v2.approval;

import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.ApprovalMode;

import java.util.List;

@Getter
@Setter
public class ApprovalWorkflowStepRequestDto {

    private Integer stepNo;

    private String stepName;

    private ApprovalMode approvalMode = ApprovalMode.ANY_ONE;

    private Integer requiredApprovals = 1;

    private Boolean active = true;

    private List<ApprovalWorkflowStepApproverRequestDto> approvers;
}