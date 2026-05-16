package za.co.mawa.bes.dto.v2.payment;

import za.co.mawa.bes.enums.PaymentRequestStatus;

public class PaymentRequestStatusUpdateRequest {

    private PaymentRequestStatus status;
    private String comment;
    private String approvalRequestId;

    public PaymentRequestStatus getStatus() { return status; }
    public void setStatus(PaymentRequestStatus status) { this.status = status; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getApprovalRequestId() { return approvalRequestId; }
    public void setApprovalRequestId(String approvalRequestId) { this.approvalRequestId = approvalRequestId; }
}
