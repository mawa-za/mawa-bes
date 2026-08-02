package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Getter @Builder
public class EmploymentHistoryDto {
    private String id;
    private String employmentId;
    private String actionRequestId;
    private String eventType;
    private String oldStatus;
    private String newStatus;
    private LocalDate effectiveDate;
    private String reason;
    private Map<String, Object> previousValues;
    private Map<String, Object> newValues;
    private String approvalRequestId;
    private LocalDateTime changedAt;
    private String changedBy;
}
