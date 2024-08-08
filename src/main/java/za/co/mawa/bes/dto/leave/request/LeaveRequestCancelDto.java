package za.co.mawa.bes.dto.leave.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class LeaveRequestCancelDto {
    private String leaveRequestId;
    private String reason;
}
