package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PremiumReceiptOfflineDto {

    private String localReceiptId;

    private String receiptNo;

    private String periodYYYYMM;

    private Long amountCents;

    private Boolean printed;
}
