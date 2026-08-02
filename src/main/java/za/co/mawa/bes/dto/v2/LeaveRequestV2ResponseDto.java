package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
public class LeaveRequestV2ResponseDto {
    private String id;
    private String requestNumber;
    private FieldOptionDto type;
    private LeaveTypeDto leaveType;
    private PartnerDto employee;
    private String employmentId;
    private String employeeNumber;
    private String leaveProfileId;
    private String leaveProfileName;
    private String workingCalendarId;
    private String workingCalendarName;
    private String assignmentSource;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal days;
    private String unit;
    private BigDecimal availableBalance;
    private BigDecimal projectedBalance;
    private String requestReason;
    private List<String> attachmentObjectIds;
    private Boolean supportingDocumentRequired;
    private String approvalRequestId;
    private FieldOptionDto status;
    private String statusReason;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<LeaveRequestStatusHistoryV2Dto> statusHistory;
}
