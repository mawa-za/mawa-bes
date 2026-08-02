package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class LeaveRequestV2UpdateRequestDto {
    private String type;
    private String leaveTypeId;
    private String employee;
    private String employmentId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal days;
    private BigDecimal requestedAmount;
    private String unit;
    private String reason;
    private List<String> attachmentObjectIds;
}
