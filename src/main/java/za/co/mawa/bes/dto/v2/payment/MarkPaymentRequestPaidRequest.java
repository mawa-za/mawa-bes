package za.co.mawa.bes.dto.v2.payment;

import java.time.LocalDate;

public class MarkPaymentRequestPaidRequest {

    private LocalDate paidDate;
    private String paidReference;
    private String comment;
    private String proofAttachmentId;

    public LocalDate getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDate paidDate) { this.paidDate = paidDate; }
    public String getPaidReference() { return paidReference; }
    public void setPaidReference(String paidReference) { this.paidReference = paidReference; }
    public String getProofAttachmentId() { return proofAttachmentId; }
    public void setProofAttachmentId(String proofAttachmentId) { this.proofAttachmentId = proofAttachmentId; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
