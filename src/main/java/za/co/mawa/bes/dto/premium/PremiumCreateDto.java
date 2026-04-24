package za.co.mawa.bes.dto.premium;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PremiumCreateDto implements Serializable {
    private String membershipId;
    private String membershipPeriod;
    private String tenderType;
    private String location;
    private String amount;
    private String terminalId;
    private String externalReceiptNo;
    private String receiptNo;
    private String createdBy;

}
