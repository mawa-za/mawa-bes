package za.co.mawa.bes.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumUpdateRequestDto {

    private String id;
    private String receiptNumber;
    private Date creationDate;
    private Date creationTime;
    private String membershipId;
    private String extReceiptNumber;
    private String membershipPeriod;
    private String tenderType;
    private String location;
    private String terminalId;
    private BigDecimal amount;
}
