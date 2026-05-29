package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.CaseBillingType;
import za.co.mawa.bes.enums.CasePriority;
import za.co.mawa.bes.enums.LegalCaseStatus;

import java.time.LocalDateTime;

@Getter
@Setter
public class LegalCaseUpdateRequest {
    private String title;
    private String caseCategory;
    private String description;
    private LegalCaseStatus status;
    private CasePriority priority;
    private String assignedTo;
    private String courtName;
    private String courtCaseNo;
    private String forumType;
    private LocalDateTime nextAppearanceDate;
    private CaseBillingType billingType;
    private Long hourlyRateCents;
    private Long fixedFeeCents;
    private Boolean billable;
}
