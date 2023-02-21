package za.co.mawa.bes.dto.claim;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class ClaimCreateDto implements Serializable {
    private String claimantId;
    private String deceasedId;
    private String memberId;
    private String membershipId;
    private String type;
    private Date deathDate;
    private Date burialDate;

}
