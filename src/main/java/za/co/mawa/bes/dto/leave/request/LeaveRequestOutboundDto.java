package za.co.mawa.bes.dto.leave.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerDto;

import java.math.BigDecimal;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class LeaveRequestOutboundDto {
    private FieldOptionDto type;
    private PartnerDto employee;
    private PartnerDto approver;
    private Date startDate;
    private Date endDate;
    private BigDecimal days;
}
