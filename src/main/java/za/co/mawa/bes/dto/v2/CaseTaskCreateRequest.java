package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.CasePriority;

import java.time.LocalDateTime;

@Getter
@Setter
public class CaseTaskCreateRequest {
    private String title;
    private String description;
    private String assignedTo;
    private CasePriority priority;
    private LocalDateTime dueDate;
    private Boolean billable;
    private Long estimatedMinutes;
}
