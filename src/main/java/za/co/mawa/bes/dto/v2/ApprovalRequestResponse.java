package za.co.mawa.bes.dto.v2;


import lombok.Builder;
import lombok.Getter;
import za.co.mawa.bes.enums.ApprovalStatus;
import za.co.mawa.bes.enums.ApprovalType;

import java.util.Date;

@Getter
@Builder
public class ApprovalRequestResponse {

    private String id;

    private ApprovalType approvalType;
    private String referenceId;
    private String referenceNo;

    private String title;
    private String description;

    private String requesterId;
    private String requesterName;
    private Boolean canSelfApprove;

    private String workflowId;
    private Integer currentStepNo;

    private ApprovalStatus status;

    private String payloadJson;

    private String finalActionBy;
    private String finalActionByName;
    private Date finalActionAt;

    private Date createdAt;
    private Date updatedAt;
}