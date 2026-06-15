package za.co.mawa.bes.dto.v2;

import java.util.Date;
import za.co.mawa.bes.enums.ApprovalActionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalActionCreateRequestDto {

    private String approvalRequestId;
    private Integer stepNo;
    private ApprovalActionType action;
    private String actionBy;
    private Date actionAt;
    private String comments;
}
