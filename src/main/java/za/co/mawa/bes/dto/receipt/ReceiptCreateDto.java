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
public class ReceiptCreateDto implements Serializable {
    private String receiptType;
    private String invoiceNumber;
    private String membershipNumber;
    private String membershipPeriod;
    private String tenderType;
    private String amount;
}
