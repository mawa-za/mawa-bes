package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.ApprovalType;

@Getter
@Setter
public class ApprovalSubmitRequest {

    private ApprovalType approvalType;

    private String referenceId;
    private String referenceNo;

    private String title;
    private String description;

    private String requesterId;

    /**
     * JSON string snapshot.
     * Example:
     * {
     *   "claimNo": "CLM-0001",
     *   "amount": 125000,
     *   "memberName": "John Doe"
     * }
     */
    private String payloadJson;
}