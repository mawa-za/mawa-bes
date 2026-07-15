package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class LeaveRequestV2CancelRequestDto {
    private String leaveRequestId;
    private String reason;
}
