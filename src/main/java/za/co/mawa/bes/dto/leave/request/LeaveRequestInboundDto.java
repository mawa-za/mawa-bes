package za.co.mawa.bes.dto.leave.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class LeaveRequestInboundDto {
    private String type;
    private String employee;
    private String approver;
    private Date startDate;
    private Date endDate;
    private BigDecimal days;
}
