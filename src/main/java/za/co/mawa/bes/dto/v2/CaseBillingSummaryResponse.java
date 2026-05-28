package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CaseBillingSummaryResponse {
    private String caseId;
    private String caseNo;
    private String title;
    private Long totalTimeMinutes;
    private Long unbilledTimeMinutes;
    private Long totalFeesCents;
    private Long unbilledFeesCents;
    private Long totalDisbursementsCents;
    private Long unbilledDisbursementsCents;
    private Long totalBillableCents;
    private Long totalBilledCents;
    private Long balanceCents;
}
