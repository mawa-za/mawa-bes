package za.co.mawa.bes.dto.claim;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class ClaimQueryDto implements Serializable {
    private String no;
    private String claimant;
    private String deceased;
    private String member;
    private String membership;
    private String type;
    private Date deathDate;
    private Date burialDate;
    private String status;
}
