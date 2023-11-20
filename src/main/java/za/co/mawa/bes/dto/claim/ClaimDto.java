package za.co.mawa.bes.dto.claim;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.BankAccountDto;
import za.co.mawa.bes.dto.PersonDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class ClaimDto extends TransactionDto implements Serializable {
    private String membershipId;
    private String memberId;
    private PartnerDto member;
    private String claimantId;
    private PartnerDto claimant;
    private String deceasedId;
    private PartnerDto deceased;
    private Date deathDate;
    private Date burialDate;
    private String paymentMethod;
    private String branch;
    private BankAccountDto bankDetails;

}
