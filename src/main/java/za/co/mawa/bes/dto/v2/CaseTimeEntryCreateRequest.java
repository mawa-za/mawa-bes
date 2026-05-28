package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CaseTimeEntryCreateRequest {
    private String taskId;
    private LocalDate entryDate;
    private String userId;
    private String description;
    private Long minutes;
    private Long hourlyRateCents;
    private Boolean billable;
}
