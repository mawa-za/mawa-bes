package za.co.mawa.bes.dto.v2;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LeaveProfileAssignmentDto {
    private String id;
    private String employmentId;
    private String employeeNumber;
    private String employeeName;
    private String positionCode;
    private String leaveProfileId;
    private String leaveProfileName;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String assignmentSource;
    private String overrideReason;
    private Boolean active;
    private Long version;
}
