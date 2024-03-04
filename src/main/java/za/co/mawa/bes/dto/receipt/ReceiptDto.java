package za.co.mawa.bes.dto.receipt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.partner.PartnerDto;

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
    private PartnerDto createdBy;
    private String transaction;
    private String location;
    private String tenderType;
    private String amount;
    private String cashupId;
    private String cashupNumber;
}
