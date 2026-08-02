package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter @Builder
public class LeaveRequestPreviewDto {
    private String employmentId;
    private String employeeNumber;
    private String leaveTypeId;
    private String leaveTypeCode;
    private String leaveTypeName;
    private String unit;
    private String leaveProfileId;
    private String leaveProfileName;
    private String assignmentSource;
    private String workingCalendarId;
    private String workingCalendarName;
    private BigDecimal requestedAmount;
    private BigDecimal availableBalance;
    private BigDecimal projectedBalance;
    private Boolean supportingDocumentRequired;
    private Boolean allowed;
    private String message;
}
