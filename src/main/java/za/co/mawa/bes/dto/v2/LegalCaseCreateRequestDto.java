package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.mawa.bes.enums.CaseBillingType;
import za.co.mawa.bes.enums.CasePriority;
import za.co.mawa.bes.enums.LegalCaseStatus;
import za.co.mawa.bes.enums.LegalCaseType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalCaseCreateRequestDto {

    private String caseNo;
    private String title;
    private String clientPartnerId;
    private LegalCaseType caseType;
    private String caseCategory;
    private String description;
    private LegalCaseStatus status;
    private CasePriority priority;
    private String assignedTo;
    private LocalDate openedDate;
    private LocalDate closedDate;
    private String courtName;
    private String courtCaseNo;
    private String forumType;
    private LocalDateTime nextAppearanceDate;
    private CaseBillingType billingType;
    private Long hourlyRateCents;
    private Long fixedFeeCents;
    private String currency;
    private Boolean billable;
    private Long totalTimeMinutes;
    private Long totalFeesCents;
    private Long totalDisbursementsCents;
    private Long totalBillableCents;
    private Long totalBilledCents;
    private Long balanceCents;
}
