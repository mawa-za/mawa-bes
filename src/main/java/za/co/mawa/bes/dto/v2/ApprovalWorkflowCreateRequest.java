package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.ApprovalType;

import java.util.List;

@Getter
@Setter
public class ApprovalWorkflowCreateRequest {

    private ApprovalType approvalType;
    private String name;
    private String description;
    private List<ApprovalWorkflowStepCreateRequest> steps;
}
