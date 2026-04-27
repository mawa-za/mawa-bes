package za.co.mawa.bes.fnb.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class OriginalGroupHeader {
    private String originalMessageId;
    private String originalCreationDateTime;
    private String originalInitiatingPartyName;
    private String originalInitiatingPartyBIC;
    private Integer originalTotalNumberOfTransactions;
    private BigDecimal originalTotalControlSum;
}
