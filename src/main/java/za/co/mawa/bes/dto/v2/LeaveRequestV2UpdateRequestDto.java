package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@NoArgsConstructor
@Getter
@Setter
public class LeaveRequestV2UpdateRequestDto {
    private String type;
    private String employee;
    private String approver;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal days;
}
