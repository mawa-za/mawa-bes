package za.co.mawa.bes.dto.v2;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class LeaveBalanceAdjustmentRequestDto {
    private String employmentId;
    private String leaveTypeId;
    private BigDecimal adjustmentAmount;
    private LocalDate effectiveDate;
    private String reason;
    private List<String> attachmentObjectIds;
}
