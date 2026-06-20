package za.co.mawa.bes.dto.v2;

import java.util.Date;
import za.co.mawa.bes.enums.ApprovalStatus;
import za.co.mawa.bes.enums.ApprovalType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequestResponseDto {

    private String id;
    private ApprovalType approvalType;
    private String referenceId;
    private String referenceNo;
    private String title;
    private String description;
    private String requesterId;
    private String workflowId;
    private Integer currentStepNo;
    private ApprovalStatus status;
    private String payloadJson;
    private String finalActionBy;
    private Date finalActionAt;
    private Date createdAt;
    private String createdBy;
    private Date updatedAt;
    private String updatedBy;
}
