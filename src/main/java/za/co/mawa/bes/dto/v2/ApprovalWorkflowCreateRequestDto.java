package za.co.mawa.bes.dto.v2;

import java.math.BigDecimal;
import java.util.List;
import za.co.mawa.bes.enums.ApprovalType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalWorkflowCreateRequestDto {

    private ApprovalType approvalType;
    private String name;
    private String description;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Boolean active;
    private List<String> steps;
}
