package za.co.mawa.bes.dto.receipt;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ReceiptCreateDto implements Serializable {
    private String receiptType;
    private String transaction;
    private String invoiceNumber;
    private String membershipNumber;
    private String membershipPeriod;
    private String tenderType;
    private String location;
    private BigDecimal amount;
    private String externalReceiptNo;
    //private  String receiptTo;
    //private  String receiptFrom;
}
