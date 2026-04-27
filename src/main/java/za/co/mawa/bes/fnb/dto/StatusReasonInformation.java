package za.co.mawa.bes.fnb.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class StatusReasonInformation {
    private String reason;
    private String additionalInformation;
}
