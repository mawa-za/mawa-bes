package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.CaseEventStatus;

@Getter
@Setter
public class CaseEventStatusUpdateRequest {
    private CaseEventStatus status;
    private String updatedBy;
}
