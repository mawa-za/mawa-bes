package za.co.mawa.bes.dto.v2.approval;

import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.ApprovalMode;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ApprovalWorkflowStepResponseDto {

    private String id;

    private Integer stepNo;

    private String stepName;

    private ApprovalMode approvalMode;

    private Integer requiredApprovals;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<ApprovalWorkflowStepApproverResponseDto> approvers;
}