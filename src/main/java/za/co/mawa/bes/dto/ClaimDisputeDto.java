package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class ClaimDisputeDto implements Serializable {
    private String claimId;
    private String reason;
    private String comments;
}
