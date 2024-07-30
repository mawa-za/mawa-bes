package za.co.mawa.bes.dto.leave.request;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class LeaveRequestEditDto {
    private Date startDate;
    private Date EndDate;
    private String status;
}
