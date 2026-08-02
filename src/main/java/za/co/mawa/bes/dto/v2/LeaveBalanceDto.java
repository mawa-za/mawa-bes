package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Builder
public class LeaveBalanceDto {
    private String id;
    private String employmentId;
    private String employeeNumber;
    private String employeeName;
    private String leaveTypeId;
    private String leaveTypeCode;
    private String leaveTypeName;
    private String unit;
    private LocalDate cycleStart;
    private LocalDate cycleEnd;
    private BigDecimal openingBalance;
    private BigDecimal accrued;
    private BigDecimal taken;
    private BigDecimal adjusted;
    private BigDecimal carriedForward;
    private BigDecimal expired;
    private BigDecimal availableBalance;
    private LocalDate lastAccrualDate;
}
