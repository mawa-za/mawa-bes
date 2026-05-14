package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApprovalDecisionRequest {

    private String actionBy;
    private String comments;
}