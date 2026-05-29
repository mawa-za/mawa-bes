package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.CaseTaskStatus;

@Getter
@Setter
public class CaseTaskStatusUpdateRequest {
    private CaseTaskStatus status;
    private String completedBy;
}
