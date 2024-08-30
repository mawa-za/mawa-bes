package za.co.mawa.bes.dto.deposit;

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
public class DepositDto implements Serializable {
    private String amount;
    private String id;
    private String number;
    private String transactionIdLink;
    private String createdOn;
    private PartnerDto createdBy;
    private String changedBy;
    private String status;
}
