package za.co.mawa.bes.dto.v2;

import lombok.Getter;
import lombok.Setter;
import za.co.mawa.bes.enums.CaseBillingType;
import za.co.mawa.bes.enums.CasePriority;
import za.co.mawa.bes.enums.LegalCaseType;

import java.time.LocalDate;

@Getter
@Setter
public class LegalCaseCreateRequest {
    private String title;
    private String clientPartnerId;
    private LegalCaseType caseType;
    private String caseCategory;
    private String description;
    private CasePriority priority;
    private String assignedTo;
    private LocalDate openedDate;
    private String courtName;
    private String courtCaseNo;
    private String forumType;
    private CaseBillingType billingType;
    private Long hourlyRateCents;
    private Long fixedFeeCents;
    private Boolean billable;
}
