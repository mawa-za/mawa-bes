package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;
import za.co.mawa.bes.dto.EmploymentDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Builder
public class EmploymentActionResponseDto {
    private String id;
    private String requestNumber;
    private String actionType;
    private String employmentId;
    private String partnerId;
    private PartnerDto employee;
    private EmploymentDto currentEmployment;
    private String proposedType;
    private LocalDate proposedStartDate;
    private LocalDate proposedEndDate;
    private String proposedPosition;
    private String proposedBranch;
    private String proposedDepartment;
    private LocalDate effectiveDate;
    private LocalDate expectedReturnDate;
    private String reason;
    private Boolean affectsPayroll;
    private Boolean suspendSystemAccess;
    private List<String> attachmentObjectIds;
    private String status;
    private String approvalRequestId;
    private String resultingEmploymentId;
    private LocalDateTime requestedAt;
    private String requestedBy;
    private LocalDateTime actionedAt;
    private String actionedBy;
    private String statusReason;
}
