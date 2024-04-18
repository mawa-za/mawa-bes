package za.co.mawa.bes.dto.receipt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;

import java.io.Serializable;
import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ReceiptDto implements Serializable {
    private String id;
    private String receiptNumber;
    private String externalReceiptNo;
    private FieldOptionDto receiptType;
    private String creationDate;
    private String creationTime;
    private PartnerDto createdBy;
    private TransactionDto transaction;
    private FieldOptionDto location;
    private FieldOptionDto tenderType;
    private BigDecimal amount;
    private String cashupId;
    private String cashupNumber;

}
