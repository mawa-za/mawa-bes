package za.co.mawa.bes.dto.v2;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalWorkflowStepUpdateRequestDto {

    private String id;
    private String workflow;
    private String stepNoId;
    private String stepNameId;
    private String approvalModeId;
    private Integer requiredApprovals;
    private Boolean active;
    private List<String> approvers;
}
