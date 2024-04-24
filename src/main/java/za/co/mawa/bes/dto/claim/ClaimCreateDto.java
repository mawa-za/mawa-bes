package za.co.mawa.bes.dto.claim;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.BankAccountCreateDto;
import za.co.mawa.bes.dto.BankAccountDto;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class ClaimCreateDto implements Serializable {
    private String type;
    private String customerId;
    private String contractId;
    private String claimantId;
    private String informantId;
    private String deceasedId;
    private String memberId;
    private String membershipId;
    private Date deathDate;
    private Date burialDate;
    private String paymentMethod;
    private String branch;
    private BankAccountCreateDto bankAccount;

}
