package za.co.mawa.bes.dto.receipt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ReceiptDto implements Serializable {
    private String id;
    private String receiptNumber;
    private String receiptType;
    private String creationDate;
    private String creationTime;
    private String createdBy;
    private String invoiceNumber;
    private String membershipNumber;
    private String membershipPeriod;
    private String tenderType;
    private String amount;
    private String cashupId;
    private String cashupNumber;
}
