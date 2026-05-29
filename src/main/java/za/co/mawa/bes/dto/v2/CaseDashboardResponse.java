package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CaseDashboardResponse {
    private Long totalCases;
    private Long openCases;
    private Long inProgressCases;
    private Long onHoldCases;
    private Long closedCases;
    private Long overdueTasks;
    private Long upcomingEvents;
    private Long unbilledTimeEntries;
    private Long unbilledDisbursements;
    private Long unbilledAmountCents;
}
