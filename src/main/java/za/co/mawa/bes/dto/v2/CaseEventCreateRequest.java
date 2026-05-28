package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.CaseEventType;

import java.time.LocalDateTime;

@Getter
@Setter
public class CaseEventCreateRequest {
    private CaseEventType eventType;
    private String title;
    private String description;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String location;
    private LocalDateTime reminderAt;
}
