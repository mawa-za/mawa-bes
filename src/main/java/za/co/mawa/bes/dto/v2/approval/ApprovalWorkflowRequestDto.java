package za.co.mawa.bes.dto.v2.approval;

import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.ApprovalType;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ApprovalWorkflowRequestDto {

    private ApprovalType approvalType;

    private String name;

    private String description;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    private Boolean active = true;

    private List<ApprovalWorkflowStepRequestDto> steps;
}