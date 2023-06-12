package za.co.mawa.bes.dto.claim;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.PartnerDto;
import za.co.mawa.bes.dto.PersonDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class ClaimDto extends TransactionDto implements Serializable {
    private String membershipId;
    private String memberId;
    private PersonDto member;
    private String claimantId;
    private PersonDto claimant;
    private String deceasedId;
    private PersonDto deceased;
//    private Date creationDate;
    private Date deathDate;
    private Date burialDate;

}
