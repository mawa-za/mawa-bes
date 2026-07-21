package za.co.mawa.bes.dto.v2.payapp;

import lombok.Data;

@Data
public class CashupDepositRequest {
    private String depositDate;
    private Long amountCents;
    private String paymentMethod;
    private String bankName;
    private String referenceNo;
    private String notes;
    private String createdBy;

    /** Base64 encoded proof of deposit. */
    private String attachmentFile;
    private String attachmentExtension;
    private String attachmentDocumentType;
}
