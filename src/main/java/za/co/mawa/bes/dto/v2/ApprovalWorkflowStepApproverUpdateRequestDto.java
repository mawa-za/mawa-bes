package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalWorkflowStepApproverUpdateRequestDto {

    private String id;
    private String workflowStep;
    private String approverTypeId;
    private String approverValueId;
    private String approverNameId;
    private Boolean active;
}
