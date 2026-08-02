package za.co.mawa.bes.dto.v2;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class EmploymentActionRequestDto {
    private String partnerId;
    private String employmentId;
    private String type;
    private LocalDate startDate;
    private LocalDate endDate;
    private String position;
    private String branch;
    private String department;
    private LocalDate effectiveDate;
    private LocalDate expectedReturnDate;
    private String reason;
    private Boolean affectsPayroll;
    private Boolean suspendSystemAccess;
    private List<String> attachmentObjectIds;
}
