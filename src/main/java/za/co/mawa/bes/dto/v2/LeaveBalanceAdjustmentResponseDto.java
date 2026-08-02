package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Builder
public class LeaveBalanceAdjustmentResponseDto {
    private String id;
    private String requestNumber;
    private String employmentId;
    private String employeeNumber;
    private String employeeName;
    private String leaveTypeId;
    private String leaveTypeCode;
    private String leaveTypeName;
    private BigDecimal adjustmentAmount;
    private LocalDate effectiveDate;
    private String reason;
    private List<String> attachmentObjectIds;
    private String status;
    private String approvalRequestId;
    private LocalDateTime requestedAt;
    private String requestedBy;
    private LocalDateTime actionedAt;
    private String actionedBy;
    private String statusReason;
}
