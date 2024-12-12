package za.co.mawa.bes.dto.claim;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class ClaimEditDto implements Serializable{
    private String claimantId;
    private BigDecimal paidOutAmount;
    private Date deathDate;
    private Date burialDate;
}
